package currency;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the quantities of items in the player's inventory.
 */
public class InventoryManager {

    /**
     * Stores item quantities by item ID.
     */
    private final Map<String, Integer> itemQuantities;

    /**
     * Initializes an empty inventory.
     */
    public InventoryManager() {
        this.itemQuantities = new HashMap<>();
    }

    /**
     * Checks whether an item ID is null or blank.
     *
     * @param id the item ID to check
     * @return true if the ID is invalid; otherwise, false
     */
    private boolean isInvalidId(String id) {
        return id == null || id.trim().isEmpty();
    }

    /**
     * Checks whether an item is registered in the inventory.
     *
     * @param itemId the item ID; null or blank IDs return false
     * @return true if the item exists; otherwise, false
     */
    public boolean has(String itemId) {
        if (isInvalidId(itemId)) {
            return false;
        }
        return this.itemQuantities.containsKey(itemId);
    }

    /**
     * Returns the quantity of an item in the inventory.
     *
     * @param itemId the item ID; null or blank IDs return 0
     * @return the item quantity, or 0 if the item is not found
     */
    public int getQuantity(String itemId) {
        if (isInvalidId(itemId)) {
            return 0;
        }
        Integer quantity = this.itemQuantities.get(itemId);
        return quantity != null ? quantity : 0;
    }

    /**
     * Adds a positive quantity of an item to the inventory.
     *
     * @param itemId the item ID; null or blank IDs are rejected
     * @param quantity the amount to add; must be positive
     * @return true if added; false for invalid input or integer overflow
     */
    public boolean add(String itemId, int quantity) {
        if (quantity <= 0 || isInvalidId(itemId)) {
            return false;
        }

        int currentQuantity = getQuantity(itemId);
        
        // Prevent integer overflow
        if (Integer.MAX_VALUE - currentQuantity < quantity) {
            return false;
        }

        this.itemQuantities.put(itemId, currentQuantity + quantity);
        return true;
    }

    /**
     * Removes a quantity of an item from the inventory.
     *
     * @param itemId the item ID; null or blank IDs are rejected
     * @param quantity the amount to remove; must be positive
     * @return true if removed; false for invalid input or insufficient quantity
     */
    public boolean remove(String itemId, int quantity) {
        if (quantity <= 0 || isInvalidId(itemId)) {
            return false;
        }

        int currentQuantity = getQuantity(itemId);
        if (currentQuantity < quantity) {
            return false; // Cannot remove more than what is owned
        }

        int newQuantity = currentQuantity - quantity;
        if (newQuantity == 0) {
            this.itemQuantities.remove(itemId);
        } else {
            this.itemQuantities.put(itemId, newQuantity);
        }

        return true;
    }

    /**
     * Returns a copy of the inventory item quantities.
     *
     * @return a new map containing the current item quantities
     */
    Map<String, Integer> copyItemQuantities() {
        return new HashMap<>(this.itemQuantities);
    }
}
