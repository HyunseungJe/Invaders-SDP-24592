package hud;

import currency.PlayerAssetsState;
import currency.RewardSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import shop.ItemSlots;
import shop.ShopController;
import shop.ShopProduct;

/** Reads the run's existing assets, rewards and slots without changing or saving them. */
public final class AssetHudProvider {
    private final ShopController shop;
    private final RewardSystem rewards;
    private final ItemSlots slots;
    private final Map<String, String> names;

    public AssetHudProvider(ShopController shop, RewardSystem rewards,
            ItemSlots slots, Map<String, String> fieldItemNames) {
        if (shop == null || rewards == null || slots == null || fieldItemNames == null) {
            throw new IllegalArgumentException("HUD dependencies cannot be null.");
        }
        this.shop = shop;
        this.rewards = rewards;
        this.slots = slots;
        this.names = new HashMap<String, String>(fieldItemNames);
        for (Map.Entry<String, String> entry : names.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()
                    || entry.getValue() == null || entry.getValue().trim().isEmpty()) {
                throw new IllegalArgumentException("Item IDs and display names must be valid.");
            }
        }
        for (ShopProduct product : shop.getProducts()) {
            names.put(product.getId(), product.getName());
        }
    }

    /** Call from the gameplay thread after processing this frame's events and input. */
    public AssetHudSnapshot snapshot() {
        PlayerAssetsState assets = shop.snapshot();
        long credits = (long) assets.getBalance() + rewards.getPendingCurrency();
        List<AssetHudSnapshot.SlotView> views = new ArrayList<AssetHudSnapshot.SlotView>();
        for (int index = 0; index < ItemSlots.SLOT_COUNT; index++) {
            ItemSlots.Slot slot = slots.getSlot(index);
            if (slot == null) {
                views.add(new AssetHudSnapshot.SlotView(index + 1, null, "Empty", null, 0));
                continue;
            }
            String id = slot.getItemId();
            String name = names.containsKey(id) ? names.get(id) : "Item";
            Integer quantity = assets.getItemQuantities().get(id);
            int purchasedQuantity = slot.getSource() == ItemSlots.Source.PURCHASED
                    && quantity != null ? quantity : 0;
            views.add(new AssetHudSnapshot.SlotView(index + 1, id, name,
                    slot.getSource(), purchasedQuantity));
        }
        boolean warning = shop.getSaveStatus() == ShopController.SaveStatus.FAILED;
        return new AssetHudSnapshot(credits, views, warning ? "Save failed" : "", warning);
    }
}
