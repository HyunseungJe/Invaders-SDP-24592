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

/**
 * Runs shop purchases and saves the shared player assets.
 * Call from the game loop that updates those assets.
 */
public final class ShopController {
    public enum SaveStatus {
        NOT_NEEDED,
        SAVED,
        FAILED
    }

    interface AssetSaver {
        void save(PlayerAssetsState state) throws IOException;
    }

    private final PlayerAssets assets;
    private final AssetSaver saver;
    private final ShopCatalog catalog;
    private final PurchaseSystem purchases;
    private final List<ShopProduct> products;
    private SaveStatus saveStatus = SaveStatus.NOT_NEEDED;

    /** Uses the supplied assets without loading or saving them. */
    public ShopController(PlayerAssets assets, FileManager fileManager) {
        this(assets, fileManager, ShopProducts.availableProducts());
    }

    ShopController(PlayerAssets assets, final FileManager fileManager, List<ShopProduct> definitions) {
        this(assets, fileManager, definitions, new AssetSaver() {
            @Override
            public void save(PlayerAssetsState state) throws IOException {
                fileManager.savePlayerAssets(state);
            }
        });
    }

    ShopController(PlayerAssets assets, FileManager fileManager, List<ShopProduct> definitions,
            AssetSaver saver) {
        if (assets == null || fileManager == null || definitions == null || saver == null) {
            throw new IllegalArgumentException("Shop dependencies cannot be null.");
        }
        this.assets = assets;
        this.saver = saver;
        this.catalog = new ShopCatalog();

        List<ShopProduct> listed = new ArrayList<ShopProduct>();
        for (ShopProduct product : definitions) {
            if (product == null) {
                throw new IllegalArgumentException("Product cannot be null.");
            }
            boolean registered = product.getCategory() == Category.INVENTORY
                    ? catalog.registerInventoryItem(product.getId(), product.getPrice())
                    : catalog.registerUnlockable(product.getId(), product.getPrice());
            if (!registered) {
                throw new IllegalArgumentException("Duplicate or invalid product: " + product.getId());
            }
            // Display the price registered for purchases.
            listed.add(new ShopProduct(product.getId(), product.getName(),
                    product.getCategory(), catalog.getPrice(product.getId()),
                    product.getDescription()));
        }
        this.products = Collections.unmodifiableList(listed);
        this.purchases = new PurchaseSystem(assets, catalog);
    }

    public List<ShopProduct> getProducts() {
        return products;
    }

    public synchronized PlayerAssetsState snapshot() {
        return assets.snapshot();
    }

    public synchronized SaveStatus getSaveStatus() {
        return saveStatus;
    }

    /** Buys one item or one permanent unlock. Check save status separately. */
    public synchronized PurchaseResult purchase(String productId) {
        PurchaseResult result = catalog.isInventoryItem(productId)
                ? purchases.purchaseInventoryItem(productId, 1)
                : purchases.purchaseUnlockable(productId);
        if (result == PurchaseResult.SUCCESS) {
            saveLatestAssets();
        }
        return result;
    }

    /** Retries a failed save using the latest assets, without repeating a purchase. */
    public synchronized boolean retrySave() {
        return saveStatus != SaveStatus.FAILED || saveLatestAssets();
    }

    private boolean saveLatestAssets() {
        try {
            saver.save(assets.snapshot());
            saveStatus = SaveStatus.SAVED;
            return true;
        } catch (IOException exception) {
            reportSaveFailure(exception);
        } catch (SecurityException exception) {
            reportSaveFailure(exception);
        }
        return false;
    }

    private void reportSaveFailure(Exception exception) {
        saveStatus = SaveStatus.FAILED;
        Core.getLogger().warning("Shop save failed; runtime assets retained: " + exception);
    }
}
