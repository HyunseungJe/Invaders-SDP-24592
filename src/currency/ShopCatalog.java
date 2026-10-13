package currency;

import java.util.HashMap;
import java.util.Map;

public class ShopCatalog {
    public enum Category {
        INVENTORY,
        UNLOCKABLE
    }

    private static class Product {
        final int price;
        final Category category;

        Product(int price, Category category) {
            this.price = price;
            this.category = category;
        }
    }

    private final Map<String, Product> catalog = new HashMap<>();

    private boolean isInvalidId(String id) {
        return id == null || id.trim().isEmpty();
    }

    public boolean registerInventoryItem(String itemId, int price) {
        return register(itemId, price, Category.INVENTORY);
    }

    public boolean registerUnlockable(String itemId, int price) {
        return register(itemId, price, Category.UNLOCKABLE);
    }

    private boolean register(String itemId, int price, Category category) {
        // Reject zero or negative prices
        if (price <= 0 || isInvalidId(itemId)) {
            return false;
        }
        
        // Reject duplicate registrations to preserve existing price and category
        if (catalog.containsKey(itemId)) {
            return false;
        }
        
        catalog.put(itemId, new Product(price, category));
        return true;
    }

    public int getPrice(String itemId) {
        if (isInvalidId(itemId) || !catalog.containsKey(itemId)) {
            return -1;
        }
        return catalog.get(itemId).price;
    }

    public boolean isInventoryItem(String itemId) {
        return !isInvalidId(itemId) && catalog.containsKey(itemId) && catalog.get(itemId).category == Category.INVENTORY;
    }

    public boolean isUnlockable(String itemId) {
        return !isInvalidId(itemId) && catalog.containsKey(itemId) && catalog.get(itemId).category == Category.UNLOCKABLE;
    }
}