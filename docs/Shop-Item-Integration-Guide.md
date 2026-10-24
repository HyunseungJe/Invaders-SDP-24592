# Shop integration

The existing Shop layout is retained. **Upgrades** replaces the upper consumable-item row with Hull, Engine and Reloader. The lower **Unlocks** row remains available for permanent content. Upgrade levels, provisional prices and preview commands are documented in [Permanent Upgrades](Permanent-Upgrades.md).

## Purchases and ownership

- `ShopProducts.availableProducts()` defines the production upgrades and ships. `Category.UPGRADE` places upgrades in the upper row; `Category.UNLOCKABLE` places ships and other permanent products in the lower row.
- `Core.getShopController()` returns the controller for the shared `PlayerAssets`. Call `purchase(productId)` to buy a product using its catalog price.
- `PurchaseSystem` validates the ID, prerequisites, existing ownership and balance. Successful purchases deduct Credits and record a permanent unlock; rejections change nothing.
- Each upgrade card advances to its next level after purchase. All three levels owned displays `MAX`. `Core` captures the purchased bonuses when starting a Campaign.

Standard Ship, Speedster and Heavy Tank appear in the lower row as available without purchase. Confirming a card does not deduct Credits, grant ownership or write a save; all three remain selectable in Hangar. The entries use `ship.standard`, `ship.speedster` and `ship.tank`. Paid access requires agreed prices, existing-player policy and matching Hangar checks; changing a shop entry alone does not restrict ship selection. Existing saved ownership is retained.

## Saves and rewards

`ShopController` saves after a successful purchase. Check `getSaveStatus()` separately from `PurchaseResult`. If saving fails, the purchase remains in memory; `retrySave()` writes the latest assets without charging or granting ownership again.

Campaign `LevelCompletedEvent` subscriptions record a clear; `CampaignRewards` awards and saves its bonus after the game screen finishes, including its end-of-stage delay. Save retries use the same shared assets as the Shop. Enemy-type rewards are not connected yet.

The asset save format retains balance, permanent IDs and legacy item quantities. Legacy quantities are preserved on load/save only; consumable purchase and use services have been removed. Field pickups and temporary effects remain unchanged and do not use this Shop inventory.

## HUD balance API

On the gameplay thread after startup, read `long credits = Core.getAssetHudSnapshot().getCredits();`. Each call returns an immutable snapshot of the existing session; request a new snapshot when updating the HUD.

Credits combine the current balance and unpaid rewards from finished stage screens, including queued completions. The current stage's clear bonus appears only after its screen finishes. Pending rewards remain in memory for this session and cannot be spent before settlement; the display total can exceed the balance's `int` limit.

Reading does not load, save or settle rewards. Calling before assets and rewards are initialized throws `IllegalStateException`. The API exposes no consumable inventory, slots or item actions. HUD drawing remains with the HUD team.

## Verification

Run `.\gradlew.bat check` on Windows or `sh ./gradlew check` on macOS/Linux. Tests cover both shop rows, tier prerequisites, repeat purchases, save failures/retries and reloads. Use the existing `run` task or **Invaders - Run** in IntelliJ to try the upgrades in Campaign.
