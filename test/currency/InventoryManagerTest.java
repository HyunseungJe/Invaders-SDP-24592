package currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryManagerTest {
    private InventoryManager inventory;

    @BeforeEach
    void setUp() {
        inventory = new InventoryManager();
    }

    @Test
    void testInitialState() {
        assertFalse(inventory.has("potion"));
        assertEquals(0, inventory.getQuantity("potion"));
    }

    @Test
    void testAddValidItem() {
        assertTrue(inventory.add("potion", 5));
        assertTrue(inventory.has("potion"));
        assertEquals(5, inventory.getQuantity("potion"));
    }

    @Test
    void testAddInvalidId() {
        assertFalse(inventory.add(null, 5));
        assertFalse(inventory.add("   ", 5));
    }

    @Test
    void testAddInvalidQuantity() {
        assertFalse(inventory.add("potion", 0));
        assertFalse(inventory.add("potion", -3));
        assertFalse(inventory.has("potion"));
    }

    @Test
    void testAddOverflow() {
        assertTrue(inventory.add("potion", Integer.MAX_VALUE));
        assertFalse(inventory.add("potion", 1));
        assertEquals(Integer.MAX_VALUE, inventory.getQuantity("potion"));
    }

    @Test
    void testRemoveValidItem() {
        inventory.add("potion", 10);
        assertTrue(inventory.remove("potion", 4));
        assertEquals(6, inventory.getQuantity("potion"));
    }

    @Test
    void testRemoveToZero() {
        inventory.add("potion", 10);
        assertTrue(inventory.remove("potion", 10));
        assertFalse(inventory.has("potion"));
        assertEquals(0, inventory.getQuantity("potion"));
    }

    @Test
    void testRemoveExceedingQuantity() {
        inventory.add("potion", 5);
        assertFalse(inventory.remove("potion", 10));
        assertEquals(5, inventory.getQuantity("potion"));
    }

    @Test
    void testRemoveInvalidQuantity() {
        inventory.add("potion", 5);
        assertFalse(inventory.remove("potion", -2));
        assertEquals(5, inventory.getQuantity("potion"));
    }

    @Test
    void testRepeatedAdditions() {
        assertTrue(inventory.add("potion", 5));
        assertTrue(inventory.add("potion", 3));
        assertEquals(8, inventory.getQuantity("potion"));
    }

    @Test
    void testRejectedRemovalsPreserveInventory() {
        inventory.add("potion", 10);

        assertFalse(inventory.remove(null, 1));
        assertFalse(inventory.remove("   ", 1));
        assertFalse(inventory.remove("potion", 0));

        // Invariant: rejected operations must leave state unchanged
        assertEquals(10, inventory.getQuantity("potion"));
    }

    @Test
    void testItemIndependence() {
        inventory.add("potion", 5);
        inventory.add("bomb", 3);

        // Modify one item and assert the other is unaffected
        assertTrue(inventory.remove("potion", 2));
        assertEquals(3, inventory.getQuantity("potion"));
        assertEquals(3, inventory.getQuantity("bomb"));

        // Completely remove one item and verify independence
        assertTrue(inventory.remove("potion", 3));
        assertFalse(inventory.has("potion"));
        assertEquals(0, inventory.getQuantity("potion"));
        assertEquals(3, inventory.getQuantity("bomb"));
    }
}