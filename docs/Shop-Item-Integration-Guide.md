# Shop integration

The existing Shop layout is retained. **Upgrades** replaces the upper consumable-item row with Hull, Engine and Reloader. The lower **Unlocks** row remains available for permanent content. Upgrade levels, provisional prices and preview commands are documented in [Permanent Upgrades](Permanent-Upgrades.md).

## Purchases and ownership

- `ShopProducts.availableProducts()` defines the production upgrades. `Category.UPGRADE` places them in the upper row; `Category.UNLOCKABLE` places other permanent products in the lower row.
- `Core.getShopController()` returns the controller for the shared `PlayerAssets`. Call `purchase(productId)` to buy a product using its catalog price.
- `PurchaseSystem` validates the ID, prerequisites, existing ownership and balance. Successful purchases deduct Credits and record a permanent unlock; rejections change nothing.
- Each upgrade card advances to its next level after purchase. All three levels owned displays `MAX`. `Core` captures the purchased bonuses when starting a Campaign.

Actual ship products, prices and ownership-based access still require agreement with the ship team. The preview retains the existing Unlock A, Ship B and Ship C samples; they do not change Hangar selection or ship stats.

## Saves and rewards

`ShopController` saves after a successful purchase. Check `getSaveStatus()` separately from `PurchaseResult`. If saving fails, the purchase remains in memory; `retrySave()` writes the latest assets without charging or granting ownership again.

Campaign `LevelCompletedEvent` subscriptions award and save the stage-clear reward through `CampaignRewards`. Save retries use the same shared assets as the Shop. Enemy-type rewards are not connected yet.

The asset save format retains balance, permanent IDs and legacy item quantities. Legacy quantities are preserved on load/save only; consumable purchase and use services have been removed. Field pickups and temporary effects remain unchanged and do not use this Shop inventory.

## Verification

Run `.\gradlew.bat check` on Windows or `sh ./gradlew check` on macOS/Linux. Tests cover both shop rows, tier prerequisites, repeat purchases, save failures/retries and reloads. `previewUpgrades` runs the game with isolated saves and a one-time test balance; see the upgrade guide for controls.
