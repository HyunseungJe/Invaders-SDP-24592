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
    void testRegisterValidItem() {
        assertTrue(catalog.registerItem("potion", 50));
        assertTrue(catalog.isAvailable("potion"));
        assertEquals(50, catalog.getPrice("potion"));
    }

    @Test
    void testRegisterInvalidItem() {
        assertFalse(catalog.registerItem(null, 50));
        assertFalse(catalog.registerItem("   ", 50));
        assertFalse(catalog.registerItem("sword", -10));
    }

    @Test
    void testRemoveItem() {
        catalog.registerItem("potion", 50);
        assertTrue(catalog.removeItem("potion"));
        assertFalse(catalog.isAvailable("potion"));
        assertEquals(-1, catalog.getPrice("potion"));
    }
    
    @Test
    void testRemoveInvalidOrNonExistentItem() {
        assertFalse(catalog.removeItem("shield"));
        assertFalse(catalog.removeItem("   "));
    }
}