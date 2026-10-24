package shop;

import currency.ShopCatalog.Category;

/** Immutable product details. Purchase prices are registered in ShopCatalog. */
public final class ShopProduct {
    private final String id;
    private final String name;
    private final Category category;
    private final int price;
    private final String description;
    private final String requiredUnlockId;

    public ShopProduct(String id, String name, Category category, int price) {
        this(id, name, category, price,
                category == Category.INVENTORY ? "Inventory item" : "Permanent unlock");
    }

    public ShopProduct(String id, String name, Category category, int price,
            String description) {
        this(id, name, category, price, description, null);
    }

    public ShopProduct(String id, String name, Category category, int price,
            String description, String requiredUnlockId) {
        if (requiredUnlockId != null && (requiredUnlockId.trim().isEmpty()
                || category != Category.UNLOCKABLE || requiredUnlockId.equals(id))) {
            throw new IllegalArgumentException("Only unlocks may require a different unlock.");
        }
        if (id == null || id.trim().isEmpty() || name == null
                || name.trim().isEmpty() || category == null || price <= 0
                || description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Product requires an ID, name, category, price and description.");
        }
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.requiredUnlockId = requiredUnlockId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public int getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public String getRequiredUnlockId() {
        return requiredUnlockId;
    }
}
