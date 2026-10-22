package shop;

import currency.ItemUseResult;
import currency.ItemUseService;

/**
 * Uses the session's assets and catalog, then saves successful consumption.
 * Obtain this from ShopController and call it on the gameplay thread.
 */
public final class ItemUseController {
    private final ShopController shop;
    private final ItemUseService service;

    ItemUseController(ShopController shop, ItemUseService service) {
        this.shop = shop;
        this.service = service;
    }

    /**
     * Returns the activation result. SUCCESS does not imply that saving succeeded;
     * check getSaveStatus() separately. Rejections do not save or clear a prior save failure.
     * Unexpected activation failures propagate without saving or retrying the effect.
     * Reconcile assets and effects before replacing a controller blocked by such a failure.
     *
     * @throws IllegalStateException if the service is blocked or an adapter fails
     *         or violates its contract
     */
    public ItemUseResult tryUse(String itemId) {
        return shop.useItem(service, itemId);
    }

    /**
     * Activates an item owned by a field slot, without changing inventory or saving.
     * The caller clears that slot only on SUCCESS. The same failure rules as tryUse apply.
     */
    public ItemUseResult tryUseFieldItem(String itemId) {
        return shop.useFieldItem(service, itemId);
    }

    /**
     * Returns the shared asset save status across gameplay contexts.
     */
    public ShopController.SaveStatus getSaveStatus() {
        return shop.getSaveStatus();
    }

    /**
     * Retries the shared shop's failed save without consuming or activating again.
     * Pending rewards are not settled by this retry.
     *
     * @throws IllegalStateException if called while item use is in progress
     */
    public boolean retrySave() {
        return shop.retrySave();
    }
}
