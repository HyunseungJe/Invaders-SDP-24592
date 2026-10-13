package currency;

import java.util.HashMap;
import java.util.Map;

public class ShopCatalog {
    private final Map<String, Integer> itemPrices;

    public ShopCatalog() {
        this.itemPrices = new HashMap<>();
    }

    private boolean isInvalidId(String id) {
        return id == null || id.trim().isEmpty();
    }

    public boolean registerItem(String itemId, int price) {
        if (isInvalidId(itemId) || price < 0) {
            return false;
        }
        this.itemPrices.put(itemId, price);
        return true;
    }

    public boolean removeItem(String itemId) {
        if (isInvalidId(itemId) || !this.itemPrices.containsKey(itemId)) {
            return false;
        }
        this.itemPrices.remove(itemId);
        return true;
    }

    public int getPrice(String itemId) {
        if (isInvalidId(itemId)) {
            return -1;
        }
        return this.itemPrices.getOrDefault(itemId, -1);
    }

    public boolean isAvailable(String itemId) {
        if (isInvalidId(itemId)) {
            return false;
        }
        return this.itemPrices.containsKey(itemId);
    }
}