package currency;

public class PurchaseSystem {
    private final PlayerAssets assets;
    private final ShopCatalog catalog;

    public PurchaseSystem(PlayerAssets assets, ShopCatalog catalog) {
        this.assets = assets;
        this.catalog = catalog;
    }

    public PurchaseResult purchaseInventoryItem(String itemId, int quantity) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return PurchaseResult.INVALID_ITEM_ID;
        }
        if (quantity <= 0) {
            return PurchaseResult.INVALID_QUANTITY;
        }
        if (!catalog.isInventoryItem(itemId)) {
            return PurchaseResult.CATEGORY_MISMATCH;
        }

        int price = catalog.getPrice(itemId);
        long totalCostLong = (long) price * (long) quantity;
        if (totalCostLong > Integer.MAX_VALUE) {
            return PurchaseResult.INTEGER_OVERFLOW_DETECTED;
        }
        int totalCost = (int) totalCostLong;

        if (!assets.canAfford(totalCost)) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        // Pre-validate inventory capacity before deducting currency
        int currentQuantity = assets.getItemQuantity(itemId);
        if (Integer.MAX_VALUE - currentQuantity < quantity) {
            return PurchaseResult.INTEGER_OVERFLOW_DETECTED;
        }

        // Deduct currency only after all validations pass
        if (!assets.deductCurrency(totalCost)) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        // Defensive fallback: attempt to grant the item. Rollback if unexpected failure occurs.
        if (!assets.addItem(itemId, quantity)) {
            assets.addCurrency(totalCost); 
            return PurchaseResult.INTEGER_OVERFLOW_DETECTED; 
        }

        return PurchaseResult.SUCCESS;
    }

    public PurchaseResult purchaseUnlockable(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return PurchaseResult.INVALID_ITEM_ID;
        }
        if (!catalog.isUnlockable(itemId)) {
            return PurchaseResult.CATEGORY_MISMATCH;
        }

        // Reject if already unlocked without charging
        if (assets.isUnlocked(itemId)) {
            return PurchaseResult.ALREADY_UNLOCKED;
        }

        int price = catalog.getPrice(itemId);

        if (!assets.deductCurrency(price)) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        // Attempt to grant the unlock. Rollback if unexpected failure occurs.
        if (!assets.unlock(itemId)) {
            assets.addCurrency(price);
            return PurchaseResult.ALREADY_UNLOCKED; 
        }

        return PurchaseResult.SUCCESS;
    }
}