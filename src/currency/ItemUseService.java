package currency;

import java.util.HashMap;
import java.util.Map;

/**
 * Uses purchased stock without charging currency again.
 * Call on the gameplay thread that owns the shared assets.
 */
public final class ItemUseService {
    private final PlayerAssets assets;
    private final ShopCatalog catalog;
    private final Map<String, ConsumableActivation> activations;
    private boolean usingItem;
    private boolean failed;

    public ItemUseService(PlayerAssets assets, ShopCatalog catalog,
            Map<String, ConsumableActivation> activations) {
        if (assets == null || catalog == null || activations == null) {
            throw new IllegalArgumentException("Item use dependencies cannot be null.");
        }
        this.assets = assets;
        this.catalog = catalog;
        this.activations = new HashMap<String, ConsumableActivation>(activations);
        for (Map.Entry<String, ConsumableActivation> entry : this.activations.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()
                    || entry.getValue() == null) {
                throw new IllegalArgumentException("Activation IDs and adapters must be valid.");
            }
        }
    }

    /**
     * Consumes one unit on success and restores it after an ordinary rejection.
     * Unexpected failures leave any reservation in place because the effect may
     * have partly applied. Reconcile assets and effects before replacing a failed service.
     *
     * @throws IllegalStateException if an adapter fails, violates its contract,
     *         or this service cannot safely accept another use
     */
    public ItemUseResult tryUse(String itemId) {
        if (failed || usingItem) {
            failed = true;
            throw new IllegalStateException("Item use is blocked; check assets and effects first.");
        }

        usingItem = true;
        try {
            if (itemId == null || itemId.trim().isEmpty()) {
                return ItemUseResult.INVALID_ITEM_ID;
            }
            ConsumableActivation activation = activations.get(itemId);
            if (!catalog.isInventoryItem(itemId) || activation == null) {
                return ItemUseResult.UNSUPPORTED_ITEM;
            }

            ItemUseResult result = checkActivationResult(activation.validate());
            if (result != ItemUseResult.SUCCESS) {
                return result;
            }
            if (!assets.removeItem(itemId, 1)) {
                return ItemUseResult.OUT_OF_STOCK;
            }

            result = checkActivationResult(activation.activate());
            if (result != ItemUseResult.SUCCESS && !assets.addItem(itemId, 1)) {
                throw new IllegalStateException("Could not restore reserved stock.");
            }
            return result;
        } catch (RuntimeException exception) {
            failed = true;
            throw new IllegalStateException("Item use failed; check assets and effects before retrying.",
                    exception);
        } catch (Error error) {
            failed = true;
            throw error;
        } finally {
            usingItem = false;
        }
    }

    private ItemUseResult checkActivationResult(ItemUseResult result) {
        if (failed) {
            throw new IllegalStateException("The activation attempted another item use.");
        }
        if (result == null) {
            throw new IllegalStateException("Activation returned null.");
        }
        switch (result) {
            case SUCCESS:
            case INVALID_CONTEXT:
            case ALREADY_ACTIVE:
            case EFFECT_REJECTED:
                return result;
            default:
                throw new IllegalStateException("Invalid activation result: " + result);
        }
    }
}
