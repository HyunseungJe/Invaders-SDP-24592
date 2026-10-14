package currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PurchaseSystemTest {
    private PlayerAssets assets;
    private ShopCatalog catalog;
    private PurchaseSystem store;

    @BeforeEach
    void setUp() {
        assets = new PlayerAssets(); 
        assets.addCurrency(1000); // Using the package-private method to fund the test account
        
        catalog = new ShopCatalog();
        store = new PurchaseSystem(assets, catalog);
        
        catalog.registerInventoryItem("potion", 100);
        catalog.registerUnlockable("stage_02", 500);
    }

    @Test
    void testInventoryItemPurchase() {
        PurchaseResult result = store.purchaseInventoryItem("potion", 2); // 200 cost
        assertEquals(PurchaseResult.SUCCESS, result);
        assertEquals(800, assets.getBalance());
        assertEquals(2, assets.getItemQuantity("potion"));
    }

    @Test
    void testPermanentUnlockPurchase() {
        PurchaseResult result = store.purchaseUnlockable("stage_02");
        assertEquals(PurchaseResult.SUCCESS, result);
        assertTrue(assets.isUnlocked("stage_02"));
        assertEquals(500, assets.getBalance()); // 1000 - 500
    }

    @Test
    void testRepeatUnlockPurchaseRejection() {
        store.purchaseUnlockable("stage_02"); // First time succeeds
        
        PurchaseResult repeatResult = store.purchaseUnlockable("stage_02");
        assertEquals(PurchaseResult.ALREADY_UNLOCKED, repeatResult);
        
        // Balance should NOT be deducted twice
        assertEquals(500, assets.getBalance()); 
    }

    @Test
    void testCategoryMismatch() {
        assertEquals(PurchaseResult.CATEGORY_MISMATCH, store.purchaseUnlockable("potion"));
        assertEquals(PurchaseResult.CATEGORY_MISMATCH, store.purchaseInventoryItem("stage_02", 1));
        
        // State remains unchanged
        assertEquals(1000, assets.getBalance());
    }
    
    @Test
    void testInventoryCapacityOverflowRollback() {
        catalog.registerInventoryItem("cheap_potion", 1);
        assets.addItem("cheap_potion", Integer.MAX_VALUE - 2);
        
        PurchaseResult result = store.purchaseInventoryItem("cheap_potion", 5);
        
        assertEquals(PurchaseResult.INTEGER_OVERFLOW_DETECTED, result);
        // Ensure balance was rolled back after the inventory rejected it
        assertEquals(1000, assets.getBalance());
    }

    @Test
    void testInsufficientFunds() {
        PurchaseResult result = store.purchaseInventoryItem("potion", 20); // 2000 cost, only have 1000
        assertEquals(PurchaseResult.INSUFFICIENT_FUNDS, result);
        
        // Assert player assets remain unchanged
        assertEquals(1000, assets.getBalance());
        assertEquals(0, assets.getItemQuantity("potion"));
    }

    @Test
    void testInvalidIdOrUnavailableProduct() {
        assertEquals(PurchaseResult.INVALID_ITEM_ID, store.purchaseInventoryItem(null, 1));
        assertEquals(PurchaseResult.INVALID_ITEM_ID, store.purchaseInventoryItem("   ", 1));
        assertEquals(PurchaseResult.CATEGORY_MISMATCH, store.purchaseInventoryItem("non_existent_item", 1));
        
        // Assert player assets remain unchanged
        assertEquals(1000, assets.getBalance());
    }

    @Test
    void testInvalidQuantity() {
        assertEquals(PurchaseResult.INVALID_QUANTITY, store.purchaseInventoryItem("potion", 0));
        assertEquals(PurchaseResult.INVALID_QUANTITY, store.purchaseInventoryItem("potion", -5));
        
        // Assert player assets remain unchanged
        assertEquals(1000, assets.getBalance());
        assertEquals(0, assets.getItemQuantity("potion"));
    }

    @Test
    void testTotalCostOverflow() {
        catalog.registerInventoryItem("expensive_item", 2000000000);
        PurchaseResult result = store.purchaseInventoryItem("expensive_item", 2); // Exceeds Integer.MAX_VALUE
        
        assertEquals(PurchaseResult.INTEGER_OVERFLOW_DETECTED, result);
        assertEquals(1000, assets.getBalance());
        assertEquals(0, assets.getItemQuantity("expensive_item"));
    }

    @Test
    void testInventoryCapacityOverflowCheck() {
        catalog.registerInventoryItem("cheap_potion", 1);
        assets.addItem("cheap_potion", Integer.MAX_VALUE - 2);
        
        // Attempt to buy 5, which triggers the pre-validation capacity check
        PurchaseResult result = store.purchaseInventoryItem("cheap_potion", 5);
        
        assertEquals(PurchaseResult.INTEGER_OVERFLOW_DETECTED, result);
        // Ensure BOTH balance and the original item quantity remain unchanged
        assertEquals(1000, assets.getBalance()); 
        assertEquals(Integer.MAX_VALUE - 2, assets.getItemQuantity("cheap_potion"));
    }
}