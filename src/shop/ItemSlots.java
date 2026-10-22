package shop;

import currency.ConsumableActivation;
import currency.ItemUseResult;
import currency.ShopCatalog.Category;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Holds the two consumables carried through one run. Use on the gameplay thread. */
public final class ItemSlots {
    public static final int SLOT_COUNT = 2;

    public enum Source {
        PURCHASED,
        FIELD
    }

    public static final class Slot {
        private final String itemId;
        private final Source source;

        private Slot(String itemId, Source source) {
            this.itemId = itemId;
            this.source = source;
        }

        public String getItemId() {
            return itemId;
        }

        public Source getSource() {
            return source;
        }
    }

    private final ShopController shop;
    private final Slot[] slots = new Slot[SLOT_COUNT];
    private ItemUseController uses;
    private Set<String> availableIds = new HashSet<String>();
    private boolean usingItem;
    private boolean failed;
    private boolean startingItemsEquipped;

    public ItemSlots(ShopController shop) {
        if (shop == null) {
            throw new IllegalArgumentException("Shop cannot be null.");
        }
        this.shop = shop;
    }

    /** Rebinds effects to the current stage's ship without changing carried items. */
    public void bind(Map<String, ConsumableActivation> activations) {
        requireReady();
        ItemUseController nextUses = shop.createItemUseController(activations);
        availableIds = new HashSet<String>(activations.keySet());
        uses = nextUses;
    }

    /** Slot indexes are 0 and 1. A null slot is empty. */
    public Slot getSlot(int index) {
        checkIndex(index);
        return slots[index];
    }

    /** Equips one unit per product in catalog order, once at the start of a run. */
    public void equipStartingItems() {
        requireReady();
        if (uses == null) {
            throw new IllegalStateException("Bind item effects before equipping starting items.");
        }
        if (startingItemsEquipped) {
            return;
        }
        for (ShopProduct product : shop.getProducts()) {
            for (int index = 0; index < slots.length; index++) {
                if (equipPurchased(index, product.getId())) {
                    break;
                }
            }
        }
        startingItemsEquipped = true;
    }

    /** Reserves a reference to purchased stock without consuming or saving it. */
    public boolean equipPurchased(int index, String itemId) {
        requireReady();
        checkIndex(index);
        if (slots[index] != null || !availableIds.contains(itemId)
                || !isInventoryItem(itemId)) {
            return false;
        }
        int allocated = 0;
        for (Slot slot : slots) {
            if (slot != null && slot.source == Source.PURCHASED && slot.itemId.equals(itemId)) {
                allocated++;
            }
        }
        Integer quantity = shop.snapshot().getItemQuantities().get(itemId);
        if (quantity == null || quantity <= allocated) {
            return false;
        }
        slots[index] = new Slot(itemId, Source.PURCHASED);
        return true;
    }

    /** Returns true only when the caller may remove the collected field pickup. */
    public boolean tryCollect(String itemId) {
        requireReady();
        if (!availableIds.contains(itemId) || isUnlockable(itemId)) {
            return false;
        }
        for (int index = 0; index < slots.length; index++) {
            if (slots[index] == null) {
                slots[index] = new Slot(itemId, Source.FIELD);
                return true;
            }
        }
        return false;
    }

    /** Clears a slot after successful activation, even when saving purchased stock fails. */
    public ItemUseResult tryUse(int index) {
        requireReady();
        checkIndex(index);
        Slot slot = slots[index];
        if (slot == null || uses == null) {
            return ItemUseResult.INVALID_CONTEXT;
        }
        usingItem = true;
        try {
            ItemUseResult result = slot.source == Source.PURCHASED
                    ? uses.tryUse(slot.itemId) : uses.tryUseFieldItem(slot.itemId);
            if (failed) {
                throw new IllegalStateException("Slots changed during item activation.");
            }
            if (result == ItemUseResult.SUCCESS) {
                slots[index] = null;
            }
            return result;
        } catch (RuntimeException | Error exception) {
            failed = true;
            throw exception;
        } finally {
            usingItem = false;
        }
    }

    /** Ends the run without removing unused purchased stock. */
    public void clear() {
        requireReady();
        Arrays.fill(slots, null);
    }

    private boolean isInventoryItem(String itemId) {
        for (ShopProduct product : shop.getProducts()) {
            if (product.getId().equals(itemId)) {
                return product.getCategory() == Category.INVENTORY;
            }
        }
        return false;
    }

    private boolean isUnlockable(String itemId) {
        for (ShopProduct product : shop.getProducts()) {
            if (product.getId().equals(itemId)) {
                return product.getCategory() == Category.UNLOCKABLE;
            }
        }
        return false;
    }

    private void requireReady() {
        if (failed || usingItem) {
            failed = true;
            throw new IllegalStateException("Slot use is blocked; check assets and effects first.");
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= slots.length) {
            throw new IndexOutOfBoundsException("Slot index must be 0 or 1.");
        }
    }
}
