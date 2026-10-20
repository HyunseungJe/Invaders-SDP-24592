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
- Before use, the Item system checks whether the player has enough of that item.
- When usage succeeds, the item's gameplay effect is applied and the inventory quantity decreases.
- An item with zero quantity cannot be used.

For example, purchasing three healing items increases the inventory quantity by three. When one is successfully used, the quantity decreases by one.

This separation ensures that permanent unlocks are not accidentally consumed and that consumable items cannot be used without sufficient inventory.

## 7. Item Usage and Gameplay Integration

Purchasing an item does not automatically apply its gameplay effect. The purchase system manages ownership, while the Item system manages what the item does in the game.

When a player attempts to use a consumable, the intended flow is:

1. The Item system requests use through `ItemUseController.tryUse(itemId)`.
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
- `createItemUseController(...)` is the integration entry point for item usage.

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
