package currency;

/**
 * Manages purchases of inventory items and permanent unlockables.
 *
 * <p>Uses PlayerAssets to access and update the player's currency
 * and item ownership, and ShopCatalog to validate product categories
 * and retrieve prices.</p>
 *
 * <p>Purchase validation and asset updates are handled here.
 * Saving the updated player state is outside this class's
 * responsibility.</p>
 */
public class PurchaseSystem {
    private final PlayerAssets assets;
    private final ShopCatalog catalog;

    public PurchaseSystem(PlayerAssets assets, ShopCatalog catalog) {
        this.assets = assets;
        this.catalog = catalog;
    }

    /**
     * Purchases a specified quantity of an inventory item.
     *
     * <p>Validates the item ID, quantity, product category, total cost,
     * available currency, and inventory capacity before updating assets.
     * Currency is deducted only after these validations pass. The item
     * quantity is then increased. If adding the item fails, the deducted
     * currency is restored.</p>
     *
     * @param itemId the identifier of the inventory item to purchase
     * @param quantity the quantity to purchase; must be greater than zero
     * @return SUCCESS if the purchase succeeds, or a result describing
     *         the reason for failure
     */
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

    /**
     * Purchases a permanent unlockable item.
     *
     * <p>Validates the item ID and product category, then checks whether
     * the item is already unlocked. If it is already unlocked, the
     * purchase is rejected without deducting currency. Otherwise, the
     * item price is deducted from the player's currency. If unlocking
     * the item fails, the deducted currency is restored.</p>
     *
     * @param itemId the identifier of the unlockable item to purchase
     * @return SUCCESS if the item is unlocked successfully, or a result
     *         describing the reason for failure
     */
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