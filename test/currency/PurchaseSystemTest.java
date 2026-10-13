package currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PurchaseSystemTest {
    private CurrencyBalance balance;
    private InventoryManager inventory;
    private ShopCatalog catalog;
    private PurchaseSystem store;

    @BeforeEach
    void setUp() {
        // Initialize the dependencies
        balance = new CurrencyBalance(1000); 
        inventory = new InventoryManager();
        catalog = new ShopCatalog();
        store = new PurchaseSystem(balance, inventory, catalog);
        
        // Setup base catalog
        catalog.registerItem("potion", 100);
        catalog.registerItem("sword", 500);
    }

    @Test
    void testSuccessfulPurchase() {
        PurchaseResult result = store.purchase("potion", 2); // 200 cost
        
        assertEquals(PurchaseResult.SUCCESS, result);
        assertEquals(800, balance.getBalance());
        assertEquals(2, inventory.getQuantity("potion"));
    }

    @Test
    void testInsufficientFunds() {
        PurchaseResult result = store.purchase("sword", 3); // 1500 cost, only have 1000
        
        assertEquals(PurchaseResult.INSUFFICIENT_FUNDS, result);
        // State should remain unchanged
        assertEquals(1000, balance.getBalance());
        assertEquals(0, inventory.getQuantity("sword"));
    }

    @Test
    void testInvalidItem() {
        assertEquals(PurchaseResult.INVALID_ITEM_ID, store.purchase(null, 1));
        assertEquals(PurchaseResult.ITEM_NOT_AVAILABLE, store.purchase("shield", 1));
    }

    @Test
    void testInvalidQuantity() {
        assertEquals(PurchaseResult.INVALID_QUANTITY, store.purchase("potion", 0));
        assertEquals(PurchaseResult.INVALID_QUANTITY, store.purchase("potion", -5));
    }

    @Test
    void testTotalCostOverflow() {
        catalog.registerItem("expensive_item", 2000000000);
        
        PurchaseResult result = store.purchase("expensive_item", 2); // Cost would exceed int max
        assertEquals(PurchaseResult.INTEGER_OVERFLOW_DETECTED, result);
    }
}