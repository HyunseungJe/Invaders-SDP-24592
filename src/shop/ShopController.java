package shop;

import currency.ConsumableActivation;
import currency.ItemUseResult;
import currency.ItemUseService;
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
import java.util.Map;

/**
 * Coordinates purchases, consumable use and saving for the shared player assets.
 * Call from the same gameplay thread as reward settlement and other asset changes.
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
    private boolean itemUseInProgress;
    private ItemUseService activeItemUse;

    /** Uses the supplied assets without loading or saving them. */
    public ShopController(PlayerAssets assets, FileManager fileManager) {
        this(assets, fileManager, ShopProducts.availableProducts());
    }

    /**
     * Creates a shop with product definitions supplied during session initialization.
     * Copies and validates products without loading or saving the shared assets.
     *
     * @throws IllegalArgumentException if a dependency or product is null, or product
     *         definitions contain an invalid or duplicate registration
     */
    public ShopController(PlayerAssets assets, final FileManager fileManager,
            List<ShopProduct> definitions) {
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

    /**
     * Returns the last save status for purchases and uses through this controller.
     * Saves performed by reward settlement do not update this status.
     */
    public synchronized SaveStatus getSaveStatus() {
        return saveStatus;
    }

    /**
     * Creates a controller for one gameplay context without loading, saving or copying assets.
     * Adapters must follow ConsumableActivation's contract and use the catalog product IDs.
     * Adapters must not change assets, settle rewards, start another use or request saving.
     * Reconcile an unexpected activation failure before creating a replacement controller.
     *
     * @throws IllegalArgumentException if activations or an activation ID or adapter is invalid
     */
    public synchronized ItemUseController createItemUseController(
            Map<String, ConsumableActivation> activations) {
        return new ItemUseController(this, new ItemUseService(assets, catalog, activations));
    }

    /**
     * Buys one item or one permanent unlock. Check save status separately.
     *
     * @throws IllegalStateException if called while item use is in progress
     */
    public synchronized PurchaseResult purchase(String productId) {
        requireNoItemUse();
        PurchaseResult result = catalog.isInventoryItem(productId)
                ? purchases.purchaseInventoryItem(productId, 1)
                : purchases.purchaseUnlockable(productId);
        if (result == PurchaseResult.SUCCESS) {
            saveLatestAssets();
        }
        return result;
    }

    /**
     * Retries this controller's failed save using the latest shared assets,
     * without repeating a purchase or effect. Reward save failures are handled separately.
     *
     * @return true if no save is pending here or the latest save succeeded
     * @throws IllegalStateException if called while item use is in progress
     */
    public synchronized boolean retrySave() {
        requireNoItemUse();
        return saveStatus != SaveStatus.FAILED || saveLatestAssets();
    }

    synchronized ItemUseResult useItem(ItemUseService service, String itemId) {
        if (activeItemUse != null) {
            // Let the active service latch reentry even if the adapter catches its exception.
            return activeItemUse.tryUse(itemId);
        }
        requireNoItemUse();
        itemUseInProgress = true;
        try {
            ItemUseResult result;
            activeItemUse = service;
            try {
                result = service.tryUse(itemId);
            } finally {
                activeItemUse = null;
            }
            if (result == ItemUseResult.SUCCESS) {
                saveLatestAssets();
            }
            return result;
        } finally {
            itemUseInProgress = false;
        }
    }

    private void requireNoItemUse() {
        // Do not persist a temporary reservation or start another transaction from an adapter.
        if (itemUseInProgress) {
            throw new IllegalStateException("Asset transactions cannot run during item activation.");
        }
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
        Core.getLogger().warning("Asset save failed; runtime assets retained: " + exception);
    }
}
