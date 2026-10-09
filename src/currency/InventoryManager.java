package currency;

import java.util.HashMap;
import java.util.Map;

public class InventoryManager {
    private final Map<String, Integer> itemQuantities;

    public InventoryManager() {
        this.itemQuantities = new HashMap<>();
    }

    // Helper method to check if an ID is invalid (null or blank)
    private boolean isInvalidId(String id) {
        return id == null || id.trim().isEmpty();
    }

    public boolean has(String itemId) {
        if (isInvalidId(itemId)) {
            return false;
        }
        return this.itemQuantities.containsKey(itemId);
    }

    public int getQuantity(String itemId) {
        if (isInvalidId(itemId)) {
            return 0;
        }
        return this.itemQuantities.getOrDefault(itemId, 0);
    }

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
    /** Returns a detached copy for asset snapshots within this package. */
    Map<String, Integer> copyItemQuantities() {
        return new HashMap<>(this.itemQuantities);
    }
}
