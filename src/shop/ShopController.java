package shop;

import currency.PlayerAssets;
import currency.PlayerAssetsState;
import currency.PurchaseResult;
import currency.PurchaseSystem;
import currency.ShopCatalog;
import currency.ShopCatalog.Category;
import engine.Core;
import engine.FileManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Coordinates permanent purchases and saving on the gameplay thread. */
public final class ShopController {
    public enum SaveStatus { NOT_NEEDED, SAVED, FAILED }

    interface AssetSaver {
        void save(PlayerAssetsState state) throws IOException;
    }

    private final PlayerAssets assets;
    private final AssetSaver saver;
    private final PurchaseSystem purchases;
    private final List<ShopProduct> products;
    private SaveStatus saveStatus = SaveStatus.NOT_NEEDED;

    public ShopController(PlayerAssets assets, FileManager files) {
        this(assets, files, ShopProducts.availableProducts());
    }

    public ShopController(PlayerAssets assets, FileManager files, List<ShopProduct> definitions) {
        this(assets, files, definitions, state -> files.savePlayerAssets(state));
    }

    ShopController(PlayerAssets assets, FileManager files, List<ShopProduct> definitions, AssetSaver saver) {
        if (assets == null || files == null || definitions == null || saver == null) {
            throw new IllegalArgumentException("Shop dependencies cannot be null.");
        }
        this.assets = assets;
        this.saver = saver;
        ShopCatalog catalog = new ShopCatalog();
        List<ShopProduct> listed = new ArrayList<ShopProduct>();
        for (ShopProduct product : definitions) {
            if (product == null) throw new IllegalArgumentException("Product cannot be null.");
            boolean registered = product.getCategory() == Category.UPGRADE
                    ? catalog.registerUpgrade(product.getId(), product.getPrice(), product.getRequiredUnlockId())
                    : catalog.registerUnlockable(product.getId(), product.getPrice(), product.getRequiredUnlockId());
            if (!registered) throw new IllegalArgumentException("Duplicate or invalid product: " + product.getId());
            listed.add(product);
        }
        products = Collections.unmodifiableList(listed);
        purchases = new PurchaseSystem(assets, catalog);
    }

    public synchronized List<ShopProduct> getProducts() {
        return ShopProducts.visibleProducts(products, assets.snapshot());
    }

    public synchronized PlayerAssetsState snapshot() {
        return assets.snapshot();
    }

    /** Reward saves are tracked by CampaignRewards separately. */
    public synchronized SaveStatus getSaveStatus() {
        return saveStatus;
    }

    public synchronized PurchaseResult purchase(String productId) {
        PurchaseResult result = purchases.purchaseUnlockable(productId);
        if (result == PurchaseResult.SUCCESS) saveLatestAssets();
        return result;
    }

    /** Save the latest state without buying or granting the upgrade again. */
    public synchronized boolean retrySave() {
        return saveStatus != SaveStatus.FAILED || saveLatestAssets();
    }

    private boolean saveLatestAssets() {
        try {
            saver.save(assets.snapshot());
            saveStatus = SaveStatus.SAVED;
            return true;
        } catch (IOException | SecurityException exception) {
            saveStatus = SaveStatus.FAILED;
            Core.getLogger().warning("Asset save failed; runtime assets retained: " + exception);
            return false;
        }
    }
}
