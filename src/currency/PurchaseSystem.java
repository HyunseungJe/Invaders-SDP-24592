package currency;

public class PurchaseSystem {
    
    // Required references passed from the main game loop / dependency injection
    private final CurrencyBalance balance;
    private final InventoryManager inventory;
    private final ShopCatalog catalog;

    public PurchaseSystem(CurrencyBalance balance, InventoryManager inventory, ShopCatalog catalog) {
        this.balance = balance;
        this.inventory = inventory;
        this.catalog = catalog;
    }

    public PurchaseResult purchase(String itemId, int quantity) {
        // 1. Basic validation
        if (itemId == null || itemId.trim().isEmpty()) {
            return PurchaseResult.INVALID_ITEM_ID;
        }
        if (quantity <= 0) {
            return PurchaseResult.INVALID_QUANTITY;
        }

        // 2. Check catalog
        if (!catalog.isAvailable(itemId)) {
            return PurchaseResult.ITEM_NOT_AVAILABLE;
        }
        
        int unitPrice = catalog.getPrice(itemId);

        // 3. Prevent total cost integer overflow
        long totalCostLong = (long) unitPrice * (long) quantity;
        if (totalCostLong > Integer.MAX_VALUE) {
             return PurchaseResult.INTEGER_OVERFLOW_DETECTED;
        }
        int totalCost = (int) totalCostLong;

        // 4. Check funds
        if (!balance.canAfford(totalCost)) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        // 5. Check inventory capacity (prevent overflow in inventory)
        int currentInvQuantity = inventory.getQuantity(itemId);
        if (Integer.MAX_VALUE - currentInvQuantity < quantity) {
            return PurchaseResult.INTEGER_OVERFLOW_DETECTED;
        }

        // 6. Execute transaction (Both must succeed to maintain integrity)
        if (balance.deduct(totalCost)) {
            if (inventory.add(itemId, quantity)) {
                return PurchaseResult.SUCCESS;
            } else {
                // Rollback if inventory fails unexpectedly
                balance.add(totalCost);
                return PurchaseResult.INTEGER_OVERFLOW_DETECTED;
            }
        }
        
        return PurchaseResult.INSUFFICIENT_FUNDS;
    }
}