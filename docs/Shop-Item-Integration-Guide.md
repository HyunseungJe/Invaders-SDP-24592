# Shop–Item Integration Guide

## 1. Purpose

This document explains how the Shop system and Item system are connected, how purchased items are managed, and how player assets are shared across the game.

The asset and purchase system manages the player's currency, inventory, permanent unlocks, purchase validation, and persistence connection. The Item team is responsible for applying the actual effects of items during gameplay.

The goal is to clarify how these responsibilities work together so that the Shop and Item systems can use the same player asset data consistently.

## 2. System Overview

The asset and purchase system consists of the following classes in `src/currency`.

| Class | Responsibility |
|---|---|
| `CurrencyBalance` | Manages the player's available currency balance. |
| `InventoryManager` | Manages quantities of consumable items. |
| `UnlockManager` | Tracks permanently unlocked items. |
| `PlayerAssets` | Provides a shared entry point for managing player assets. |
| `PlayerAssetsState` | Represents player asset data for saving and restoring state. |
| `ShopCatalog` | Provides product information, including item IDs, categories, and prices. |
| `PurchaseSystem` | Validates purchase requests and processes purchases. |
| `PurchaseResult` | Represents the result of a purchase attempt. |

These classes separate currency, inventory, permanent unlocks, product information, and purchase processing. This separation allows the Shop and Item systems to interact with player assets without combining all responsibilities into a single class.

## 3. Core Design Principles

The integration follows these principles:

- **Currency and score are separate.** The player's balance is managed independently of the game score and must never become negative.
- **Permanent unlocks and consumable quantities are different.** Permanent items are tracked by unlock status, while consumable items are tracked by quantity.
- **`ShopCatalog` is the source of product prices.** The purchase system must use the catalog price rather than trusting a price supplied by another system.
- **Rejected purchases must not change player assets.** If a purchase fails, the balance, inventory, and unlock state must remain unchanged.
- **Purchase success and save success are separate.** Retrying a save must not execute the purchase again or deduct currency twice.
- **The Shop and gameplay use the same `PlayerAssets`.** Both systems must access the same authoritative player asset state so that purchases and item usage remain consistent.
- **Purchasing and using an item are separate operations.** Purchasing a consumable increases its quantity; using it applies its effect and decreases its quantity. Purchasing a permanent item changes its unlock status.
- **Actual item effects belong to the Item team.** Our team prepares the purchase, inventory, item-usage service, and persistence integration. The Item team implements the actual gameplay effects.

## 4. How the Shop and Item Systems Are Connected

The Shop and Item systems interact through `PlayerAssets` and the services that manage purchases and item usage.

The Shop system submits purchase requests to `PurchaseSystem`. The purchase system checks the product information in `ShopCatalog` and updates the player's shared asset state after a successful purchase. During gameplay, the Item system accesses that same state to check whether an item is available and to manage its use.

### System relationship

```text
          Shop System
               |
               v
        PurchaseSystem <---- ShopCatalog
               |
               v
          PlayerAssets
               |
       +-------+--------+
       |       |        |
       v       v        v
    Currency Inventory Unlock
    Balance   Manager  Manager
               |
               v
       Item Usage Service
               |
               v
          Item System
        (Actual Effects)

          PlayerAssetsState
                 |
              Save/Load
```

The diagram illustrates the intended integration between the systems. The exact method calls depend on the service interfaces used by the project.

The responsibilities are divided as follows:

1. **Shop system:** Submits purchase requests.
2. **`ShopCatalog`:** Supplies authoritative product information and prices.
3. **`PurchaseSystem`:** Validates the request and processes the purchase.
4. **`PlayerAssets`:** Provides access to the player's shared balance, inventory, and unlock state.
5. **Item Usage Service:** Checks item availability, coordinates item usage, and updates inventory when usage succeeds.
6. **Item system:** Applies the actual gameplay effect of an item.
7. **Persistence:** Saves and restores the player's asset state.

## 5. Purchase Workflow

When a player purchases an item, the process follows these steps:

1. The Shop system submits the product ID and requested quantity to `PurchaseSystem`.
2. `PurchaseSystem` retrieves the product information and price from `ShopCatalog`.
3. The purchase system validates the product ID, availability, category, quantity, balance, and relevant unlock restrictions.
4. If validation fails, the purchase is rejected and the player's assets remain unchanged.
5. If validation succeeds, the purchase is processed:
   - For a consumable, the inventory quantity increases.
   - For a permanent item, the item's unlock state changes.
   - The required currency is deducted from the player's balance.
6. The purchase result is returned to the calling system through `PurchaseResult`.
7. The updated player asset state can then be saved through the persistence mechanism.

The purchase system is responsible for deciding whether a purchase is valid. Other systems should use the purchase result rather than independently deciding whether the player can afford an item.

Saving is handled separately from the purchase itself. If saving fails and needs to be retried, the retry should save the existing state instead of processing the purchase again.

## 6. Permanent Items and Consumables

The system distinguishes between two types of items because they behave differently after purchase.

### 6.1. Permanent Items

Permanent items are items that remain unlocked once purchased.

- The purchase system checks whether the item can be purchased.
- After a successful purchase, `UnlockManager` records the item as unlocked.
- Gameplay can check the unlock state to determine whether the player has access to the item.
- The item is not managed as a consumable quantity.

For example, if a permanent upgrade has been purchased and unlocked, the gameplay system can check the player's unlock state before allowing access to that upgrade.

### 6.2. Consumable Items

Consumable items have a limited quantity and can be used during gameplay.

- Purchasing a consumable increases its inventory quantity.
- For purchased items, HanCode checks stock through the usage service. Field pickups are held in run slots separately from purchased stock.
- Successful use of a purchased item applies the effect and consumes one inventory unit.
- A purchased item with zero stock cannot be used; a collected field slot does not require purchased stock.

For example, purchasing three healing items increases the inventory quantity by three. When one is successfully used, the quantity decreases by one.

Permanent unlocks are never consumed, and purchased consumables require sufficient purchased stock.

## 7. Item Usage and Gameplay Integration

Purchasing an item does not automatically apply its gameplay effect. The purchase system manages ownership, while the Item system manages what the item does in the game.

For a purchased consumable, the service flow is:

1. GameScreen requests a slot use; a purchased slot delegates to `ItemUseController.tryUse(itemId)`.
2. `ItemUseService` validates the item ID and checks that the catalog and activation adapter support it.
3. The service calls `validate()`. An ordinary rejection returns without changing inventory.
4. After successful validation, the service reserves one unit through `PlayerAssets`. If no stock is available, it returns `OUT_OF_STOCK` without activating the effect.
5. The service calls `activate()`. On `SUCCESS`, the reserved unit remains consumed.
6. On `SUCCESS`, `ItemUseController` attempts to save the updated assets. Callers check the save result separately with `getSaveStatus()`.

The Item team is responsible for implementing the actual effect, such as healing the player or applying a temporary ability. Our team is responsible for preparing the service that checks availability, manages inventory changes, and connects those changes to player asset state and persistence.

Failures are handled according to the `ConsumableActivation` contract:

- Ordinary rejection: validation rejection leaves inventory unchanged. If activation rejects after a unit has been reserved, that unit is restored.
- Unexpected activation failure: the reserved unit is retained, and the failed `ItemUseService` blocks all subsequent item-use requests through its `ItemUseController`, including requests for other items. Reconciling assets and effects does not reset the failed instance. Create a replacement `ItemUseController` through the shared `ShopController` only after reconciliation.

Ordinary rejections are returned as `ItemUseResult` values; unexpected failures are thrown.


## 8. Shared Player Assets

The Shop and Item systems must use the same authoritative `PlayerAssets` instance or an equivalent shared state-management mechanism.

For example:

- The Shop system purchases two consumable items and updates the inventory.
- The Item system reads the inventory and sees the updated quantity.
- The player uses one item.
- The inventory quantity decreases.
- The Shop and other systems see the updated quantity when they access the same asset state.

If the Shop and Item systems maintain separate, unsynchronized copies of the inventory, one system may display an outdated quantity or allow an item to be used incorrectly.

Using shared player asset state keeps the balance, inventory, and unlock status consistent across the game.

## 9. Persistence: Saving and Restoring Player Assets

`PlayerAssetsState` represents the player's asset data for saving and restoring state.

The persistence integration should preserve the following information:

- Current currency balance.
- Consumable item quantities.
- Permanent item unlock states.

After a purchase or successful item usage, the updated state can be saved so that the player's progress is preserved.

Purchase processing and persistence must remain separate responsibilities. A successful purchase should not be repeated simply because saving failed. Retrying persistence should operate on the updated state without deducting the purchase price again or increasing the inventory a second time.

**Integration entry points**

- `Core.getShopController()` is the integration entry point for the Shop system.
- `GameScreen.bindItemEffects(...)` connects supplied adapters to the run slots. Those slots use `createItemUseController(...)` internally.

**Saving after item usage**

A successful `tryUse()` call automatically attempts to save the updated assets. Usage success and save success are separate: an item can be used successfully even if the save fails. Callers should check `getSaveStatus()` and, if the save did not succeed, call `retrySave()`. `retrySave()` saves the latest assets without consuming another item and without replaying the effect.

## 10. Team Responsibilities

### Asset and Purchase Team

Our team's responsibilities include:

- Managing the player's currency balance.
- Managing consumable inventory quantities.
- Tracking permanent item unlocks.
- Maintaining product information and prices through `ShopCatalog`.
- Validating and processing purchases through `PurchaseSystem`.
- Returning purchase outcomes through `PurchaseResult`.
- Providing shared access to player assets through `PlayerAssets`.
- Preparing the item-usage service and inventory updates.
- Connecting player asset state to saving and restoring.

### Item Team

The Item team's responsibilities include:

- Implementing the actual gameplay effects of items.
- Defining the conditions under which an item effect succeeds or fails.
- Integrating item effects with the item-usage service.
- Checking permanent unlock status when access to an item or upgrade depends on ownership.
- Coordinating how item usage and inventory updates behave when an effect fails.

### Shared Integration Responsibilities

Both teams need to coordinate:

- Consistent item IDs between the Shop catalog and the Item system.
- The distinction between permanent items and consumables.
- The interface between the item-usage service and gameplay effects.
- The rules for consuming an item only when usage succeeds.
- Access to shared `PlayerAssets` state.
- Saving and restoring changes to player assets.

## 11. Summary

The Shop and Item systems are connected through shared player asset state and dedicated services.

The Shop system uses `ShopCatalog` and `PurchaseSystem` to validate and process purchases. Successful purchases update the player's balance, inventory, or permanent unlock state through `PlayerAssets`. During gameplay, the Item system accesses that same asset state to check ownership and availability, while the item-usage service coordinates inventory updates with the actual gameplay effect.

Our team manages purchasing, inventory, usage-service preparation, and persistence integration. The Item team manages the actual effects of items. Keeping these responsibilities separate while sharing the same player asset state allows both systems to work together consistently.

## 12. Run Slots and HUD Hooks

Core creates one `GameplayAssets` session from the loaded assets. It shares the shop, reward accumulator and latest save result, creates a fresh slot container for each Campaign run, attaches it to each stage and clears it when the run ends. Actual item adapters and pickup routing, typed reward event subscriptions, and final HUD placement still need the corresponding team changes.

### Shared save result and settlement

`GameplayAssets` constructs `ShopController` and `RewardSettlementCoordinator` with the same `PlayerAssets`, `FileManager` and `AssetSaveState`. Gameplay uses these existing objects; do not create or load a second set of assets. `Core.getShopController()` still returns the shared shop.

Core calls `settleStage()` after a Campaign stage returns, including failure and a normal window-close request. This transfers only rewards already accumulated; it does not award a completion bonus. The forthcoming Campaign event listener must award that bonus exactly once from the agreed completion event. HanCode still needs to configure reward values and subscribe to the typed events once their publication paths are available in the integration base. Ordinary gameplay does not yet earn credits.

Saving failures keep runtime assets intact and offer a save-only retry. Window closing finishes the current frame and returns to Core. Core attempts to save before disposing the window. If that fails, it warns about losing unsaved progress and offers Retry, Return to menu, or Exit without saving. Closing this warning returns to the menu. Balance overflow retains pending rewards and does not end the Campaign run; a cleared stage still advances, and later rewards keep accumulating. These rules cover normal application exits, not forced process termination.

### Two slots

Core already calls `beginRun()` and `gameScreen.setGameplayAssets(...)`. Once Best-French supplies its adapters, HanCode will bind them after the stage's ship and effect managers have been created, before gameplay starts:

```java
gameScreen.bindItemEffects(activations, fieldItemNames);
```

Here `activations` is a `Map<String, ConsumableActivation>` for the current ship, and `fieldItemNames` supplies display names for field-only IDs. Binding equips starting items only once per run. At each following stage, bind fresh adapters for the new ship; existing slots are retained without automatic refilling. Until adapters are provided, the production slots remain empty and the existing item paths keep their behavior.

Starting equipment takes one unit per supported product in catalog order, up to two slots. Equipping only holds a reference to purchased stock. It does not consume or save it. Starting equipment is selected once; using an item does not automatically refill the slot.

For a field pickup, remove it only when `gameScreen.tryCollectItem(itemId)` returns `true`. A full or unavailable container returns `false`, so the pickup can continue falling. Field items use the same activation contract but do not consume or save purchased inventory.

GameScreen calls the first and second slots once per new 1/2 key press while gameplay accepts input. A key already held on entry or during blocked input must be released before use. A key assigned to movement or firing cannot also consume a slot. Ordinary rejection keeps the slot and stock unchanged. Success clears the slot, including when the subsequent purchased-stock save fails. An unexpected adapter exception blocks the container until its assets and effects have been reconciled; rebinding cannot bypass this failure.

Core retains the same container across stages; life loss does not replace it. At run end, Core calls `endRun()` to clear and discard the container. Unused purchased stock remains in `PlayerAssets`; field slots and active effects are not saved. `setItemSlots(...)` remains available for isolated screen checks.

### HUD data and drawing

The Campaign screen now exposes its existing provider through `getAssetHudSnapshot()`. After gameplay input and events have been processed, read it on the gameplay thread, inside the normal drawing pass:

```java
AssetHudSnapshot state = gameScreen.getAssetHudSnapshot();
if (state != null) {
    long credits = state.getCredits();
    String notice = state.getNotice();
    boolean warning = state.isWarning();
    int slotHeight = drawManager.drawItemSlots(gameScreen, state, x, y, availableWidth);
}
```

A null snapshot means the screen has no Campaign asset context. The returned data is read-only; Friends does not need to construct a provider or calculate rewards. Product names come from the shop, while field-only names come from the item binding above.

`credits` is the current balance plus earned rewards awaiting settlement. It is a `long` to avoid display overflow. Settlement transfers that amount without changing the displayed total; purchases still use only the settled balance. The notice is `"Save failed"` when the last shared save attempt failed, otherwise empty. Pending rewards alone do not produce a save warning.

Each immutable slot view supplies its 1/2 key number, item ID, display name, purchased/field source and total purchased quantity. Empty slots report `isEmpty()`. Purchased quantity includes equipped references; a field slot reports zero purchased quantity.

`drawItemSlots(...)` draws two rows using the existing game font and palette, clips to the supplied width and returns the height used. Call it between `initDrawing(...)` and `completeDrawing(...)`. Friends can choose coordinates alongside its existing Credits display after the joint layout check. The method does not move the separator or replace the active-buff display. The production drawing pass does not call this method yet.

HUD-facing files are `src/screen/GameScreen.java`, `src/hud/AssetHudProvider.java`, `src/hud/AssetHudSnapshot.java`, and `src/engine/DrawManager.java`. HanCode owns the lifecycle in `src/engine/GameplayAssets.java` and `src/engine/Core.java`, with normal-exit handling in `src/engine/Frame.java` and `src/screen/Screen.java`. The existing `ShopScreen` constructor and menu return code are unchanged.

## 13. Speedster Unlock Check

The first ship unlock uses `ship.speedster`. Standard and Heavy Tank remain freely available. KimchiBaguette owns the locked-state display and selection check; the preview below verifies HanCode's purchase and persistence side. The production catalog remains empty until prices are agreed.

From the repository root, compile the preview and choose a separate test directory:

```powershell
.\gradlew.bat testClasses
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
$previewClasspath = '.\build\classes\java\main;.\build\classes\java\test;.\build\resources\main'
$speedsterProfile = Join-Path $env:TEMP 'invaders-speedster-preview'
java -cp $previewClasspath shop.ShopPreview --speedster-profile $speedsterProfile
```

A new profile starts with 1,000 test credits and no unlocks. Speedster costs 500 only in this preview; this is not the agreed production price. Buy it, close the preview, and run the last command again with the same directory. The balance must remain 500 and Speedster must remain owned. Existing profiles are never refilled or reset. Use a different directory for a fresh check.

The profile uses `player_assets.properties`, so the selection implementation can read the same state using `-Dinvaders.storageDir=<profile directory>`. For example, after the selection check has been connected:

```powershell
java "-Dinvaders.storageDir=$speedsterProfile" -cp $previewClasspath engine.Core
```

Read `PlayerAssets.isUnlocked("ship.speedster")` on the shared assets for the selection decision. Keep locked Speedster visible, block its selection until purchase, and do not add checks for Standard or Heavy Tank. The preview does not implement that selection policy itself.
