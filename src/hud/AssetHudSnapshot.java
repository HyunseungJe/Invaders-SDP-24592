package hud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import shop.ItemSlots.Source;

/** Read-only display data; credits include rewards awaiting settlement. */
public final class AssetHudSnapshot {
    private final long credits;
    private final List<SlotView> slots;
    private final String notice;
    private final boolean warning;

    AssetHudSnapshot(long credits, List<SlotView> slots, String notice, boolean warning) {
        this.credits = credits;
        this.slots = Collections.unmodifiableList(new ArrayList<SlotView>(slots));
        this.notice = notice;
        this.warning = warning;
    }

    public long getCredits() {
        return credits;
    }

    public List<SlotView> getSlots() {
        return slots;
    }

    public String getNotice() {
        return notice;
    }

    public boolean isWarning() {
        return warning;
    }

    public static final class SlotView {
        private final int keyNumber;
        private final String itemId;
        private final String name;
        private final Source source;
        private final int purchasedQuantity;

        SlotView(int keyNumber, String itemId, String name, Source source, int purchasedQuantity) {
            this.keyNumber = keyNumber;
            this.itemId = itemId;
            this.name = name;
            this.source = source;
            this.purchasedQuantity = purchasedQuantity;
        }

        public int getKeyNumber() {
            return keyNumber;
        }

        public boolean isEmpty() {
            return itemId == null;
        }

        public String getItemId() {
            return itemId;
        }

        public String getName() {
            return name;
        }

        /** Null for an empty slot. */
        public Source getSource() {
            return source;
        }

        /** Total purchased stock, including equipped references; zero for field slots. */
        public int getPurchasedQuantity() {
            return purchasedQuantity;
        }
    }
}
