package currency;

import java.util.HashMap;
import java.util.Map;

/**
 * Maintains the shop's product catalog, including each product's price
 * and category.
 *
 * <p>Supports registering inventory items and permanent unlockables,
 * retrieving product prices, and checking product categories.
 * Product information is stored in an in-memory map.</p>
 *
 * <p>This class manages catalog information only; it does not deduct
 * player currency, grant items, or save player state.</p>
 */
public class ShopCatalog {

    /**
     * Defines the types of products that can be registered in the shop.
     */
    public enum Category {
        INVENTORY,
        UNLOCKABLE
    }

    /**
     * Stores the price and category of a single product in the catalog.
     *
     * <p>This class is used internally by ShopCatalog to associate
     * product information with a product ID.</p>
     */
    private static class Product {
        final int price;
        final Category category;

        /**
         * Creates a product entry with the specified price and category.
         *
         * @param price the product's price
         * @param category the type of product
         */
        Product(int price, Category category) {
            this.price = price;
            this.category = category;
        }
    }

    /**
     * Stores registered products by their IDs.
     *
     * <p>Each entry maps a product ID to its price and category.
     * The map is initialized as an empty HashMap and populated
     * when products are registered successfully.</p>
     */
    private final Map<String, Product> catalog = new HashMap<>();

    /**
     * Checks whether a product ID is null or blank.
     *
     * <p>An ID is considered invalid if it is null or contains
     * only whitespace characters.</p>
     *
     * @param id the product ID to validate
     * @return true if the ID is null or blank; otherwise, false
     */
    private boolean isInvalidId(String id) {
        return id == null || id.trim().isEmpty();
    }

    /**
     * Registers a product as an inventory item in the shop catalog.
     *
     * @param itemId the unique identifier of the inventory item
     * @param price the item's price; must be positive
     * @return true if registration succeeds; otherwise, false
     */
    public boolean registerInventoryItem(String itemId, int price) {
        return register(itemId, price, Category.INVENTORY);
    }

    /**
     * Registers a product as a permanent unlockable in the shop catalog.
     *
     * @param itemId the unique identifier of the unlockable item
     * @param price the item's price; must be positive
     * @return true if registration succeeds; otherwise, false
     */
    public boolean registerUnlockable(String itemId, int price) {
        return register(itemId, price, Category.UNLOCKABLE);
    }

    /**
     * Registers a product if its ID and price are valid and the ID is unused.
     *
     * @param itemId the product ID
     * @param price the product price
     * @param category the product category
     * @return true if registered; otherwise, false
     */
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

    /**
     * Returns the price of a registered product.
     *
     * @param itemId the product ID
     * @return the product price, or -1 if not found
     */
    public int getPrice(String itemId) {
        if (isInvalidId(itemId) || !catalog.containsKey(itemId)) {
            return -1;
        }
        return catalog.get(itemId).price;
    }

    /**
     * Checks whether the specified product is an inventory item.
     *
     * @param itemId the product ID
     * @return true if it is a registered inventory item; otherwise, false
     */
    public boolean isInventoryItem(String itemId) {
        return !isInvalidId(itemId) && catalog.containsKey(itemId) && catalog.get(itemId).category == Category.INVENTORY;
    }

    /**
     * Checks whether the specified product is an unlockable.
     *
     * @param itemId the product ID
     * @return true if it is a registered unlockable; otherwise, false
     */
    public boolean isUnlockable(String itemId) {
        return !isInvalidId(itemId) && catalog.containsKey(itemId) && catalog.get(itemId).category == Category.UNLOCKABLE;
    }
}