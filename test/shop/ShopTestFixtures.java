package shop;

import currency.PlayerAssets;
import currency.PlayerAssetsState;
import currency.ShopCatalog.Category;
import engine.FileManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Sample products and balances used only by tests. */
public final class ShopTestFixtures {
    public static final String CONSUMABLE = "TEST_CONSUMABLE";
    public static final String UNLOCK = "TEST_PERMANENT";

    private ShopTestFixtures() {
    }

    public static PlayerAssets fundedAssets() {
        PlayerAssets assets = new PlayerAssets();
        assets.restore(new PlayerAssetsState(1000, Collections.<String>emptySet(),
                Collections.<String, Integer>emptyMap()));
        return assets;
    }

    public static List<ShopProduct> products() {
        return Arrays.asList(
                new ShopProduct(CONSUMABLE, "Item A", Category.INVENTORY, 100,
                        "Sample inventory item."),
                new ShopProduct(UNLOCK, "Unlock A", Category.UNLOCKABLE, 500,
                        "Sample permanent unlock."));
    }

    /** More sample cards for browsing; these do not implement gameplay effects. */
    public static List<ShopProduct> previewProducts() {
        List<ShopProduct> products = new ArrayList<ShopProduct>(products());
        products.add(new ShopProduct("TEST_ITEM_B", "Item B", Category.INVENTORY, 150,
                "Sample inventory item."));
        products.add(new ShopProduct("TEST_ITEM_C", "Item C", Category.INVENTORY, 200,
                "Sample inventory item."));
        products.add(new ShopProduct("TEST_SHIP_B", "Ship B", Category.UNLOCKABLE, 650,
                "Sample ship unlock."));
        products.add(new ShopProduct("TEST_SHIP_C", "Ship C", Category.UNLOCKABLE, 800,
                "Sample ship unlock."));
        return products;
    }

    public static ShopController controller(PlayerAssets assets, FileManager files) {
        return new ShopController(assets, files, products());
    }

    public static ShopController controller(PlayerAssets assets, FileManager files,
            List<ShopProduct> products) {
        return new ShopController(assets, files, products);
    }
}
