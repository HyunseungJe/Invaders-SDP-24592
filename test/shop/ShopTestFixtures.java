package shop;

import currency.PlayerAssets;
import currency.PlayerAssetsState;
import currency.ShopCatalog.Category;
import engine.FileManager;
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

    public static ShopController controller(PlayerAssets assets, FileManager files) {
        return new ShopController(assets, files, products());
    }
}
