package currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShopCatalogTest {
    private ShopCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new ShopCatalog();
    }

    @Test
    void testZeroAndNegativePriceRejection() {
        assertFalse(catalog.registerInventoryItem("free_potion", 0));
        assertFalse(catalog.registerUnlockable("free_stage", 0));
        assertFalse(catalog.registerInventoryItem("cursed_item", -50));
        
        // Ensure catalog remained unchanged
        assertEquals(-1, catalog.getPrice("free_potion"));
    }

    @Test
    void testDuplicateRegistrationPreservesOriginal() {
        assertTrue(catalog.registerInventoryItem("potion", 100));
        
        // Attempt to overwrite with a different price and category
        assertFalse(catalog.registerUnlockable("potion", 200));
        assertFalse(catalog.registerInventoryItem("potion", 50));
        
        // Assert original state is preserved
        assertEquals(100, catalog.getPrice("potion"));
        assertTrue(catalog.isInventoryItem("potion"));
        assertFalse(catalog.isUnlockable("potion"));
    }
}