package shop;

import currency.ConsumableActivation;
import currency.ItemUseResult;
import currency.PlayerAssets;
import currency.PlayerAssetsState;
import currency.ShopCatalog.Category;
import engine.Core;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemSlotsTest {
    private static final String ITEM = "test.item";
    private static final String FIELD = "test.field";
    private static final String UNLOCK = "test.ship";
    private PlayerAssets assets;
    private ShopController shop;
    private ItemSlots slots;
    private TestActivation activation;
    private int saves;
    private boolean denySave;

    @BeforeEach
    void setUp() {
        assets = new PlayerAssets();
        assertTrue(assets.restore(new PlayerAssetsState(100, Set.of(), Map.of(ITEM, 1))));
        shop = new ShopController(assets, Core.getFileManager(), List.of(
                new ShopProduct(ITEM, "Test item", Category.INVENTORY, 10),
                new ShopProduct(UNLOCK, "Test ship", Category.UNLOCKABLE, 20)), state -> {
                    saves++;
                    if (denySave) throw new IOException("Test save failure");
                });
        activation = new TestActivation();
        slots = new ItemSlots(shop);
        slots.bind(Map.of(ITEM, activation, FIELD, activation, UNLOCK, activation));
    }

    @Test
    void equippingReferencesStockWithoutDoubleAllocatingOneUnit() {
        assertTrue(slots.equipPurchased(0, ITEM));
        assertFalse(slots.equipPurchased(1, ITEM));
        assertFalse(slots.equipPurchased(1, FIELD));
        assertFalse(slots.equipPurchased(1, UNLOCK));
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(ItemSlots.Source.PURCHASED, slots.getSlot(0).getSource());
        assertNull(slots.getSlot(1));
        assertEquals(0, saves);
        assertEquals(0, activation.calls);
    }

    @Test
    void startingItemsFollowCatalogOrderAndDoNotRefillDuringTheRun() {
        String second = "test.second";
        String third = "test.third";
        assertTrue(assets.restore(new PlayerAssetsState(100, Set.of(),
                Map.of(ITEM, 4, second, 2, third, 1))));
        shop = new ShopController(assets, Core.getFileManager(), List.of(
                new ShopProduct(ITEM, "First", Category.INVENTORY, 10),
                new ShopProduct(second, "Second", Category.INVENTORY, 10),
                new ShopProduct(third, "Third", Category.INVENTORY, 10)), state -> saves++);
        slots = new ItemSlots(shop);
        slots.bind(Map.of(ITEM, activation, second, activation, third, activation));

        slots.equipStartingItems();

        assertEquals(ITEM, slots.getSlot(0).getItemId());
        assertEquals(second, slots.getSlot(1).getItemId());
        assertEquals(4, assets.getItemQuantity(ITEM));
        assertEquals(0, saves);
        assertEquals(ItemUseResult.SUCCESS, slots.tryUse(0));
        slots.equipStartingItems();
        slots.bind(Map.of(ITEM, new TestActivation(), second, new TestActivation()));
        slots.equipStartingItems();
        assertNull(slots.getSlot(0));
        assertEquals(second, slots.getSlot(1).getItemId());
        assertEquals(3, assets.getItemQuantity(ITEM));
        assertEquals(1, saves);
    }

    @Test
    void collectingFillsOnlyEmptySlotsAndDoesNotApplyTheEffect() {
        assertFalse(slots.tryCollect(null));
        assertFalse(slots.tryCollect("unknown"));
        assertFalse(slots.tryCollect(UNLOCK));
        assertTrue(slots.tryCollect(FIELD));
        assertTrue(slots.tryCollect(ITEM));
        assertFalse(slots.tryCollect(FIELD));
        assertEquals(FIELD, slots.getSlot(0).getItemId());
        assertEquals(ITEM, slots.getSlot(1).getItemId());
        assertEquals(ItemSlots.Source.FIELD, slots.getSlot(1).getSource());
        assertEquals(0, activation.calls);
        assertEquals(0, saves);
        assertEquals(1, assets.getItemQuantity(ITEM));
    }

    @Test
    void purchasedAndFieldSlotsUseTheSameEffectWithDifferentStockHandling() {
        assertTrue(slots.equipPurchased(0, ITEM));
        assertTrue(slots.tryCollect(ITEM));
        assertEquals(ItemUseResult.SUCCESS, slots.tryUse(1));
        assertNull(slots.getSlot(1));
        assertNotNull(slots.getSlot(0));
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(0, saves);

        assertEquals(ItemUseResult.SUCCESS, slots.tryUse(0));
        assertNull(slots.getSlot(0));
        assertEquals(0, assets.getItemQuantity(ITEM));
        assertEquals(1, saves);
        assertEquals(2, activation.calls);
        assertEquals(ItemUseResult.INVALID_CONTEXT, slots.tryUse(0));
        assertEquals(2, activation.calls);
    }

    @Test
    void rejectedUseKeepsTheSlotAndLastPurchasedUnit() {
        assertTrue(slots.equipPurchased(0, ITEM));
        ItemSlots.Slot original = slots.getSlot(0);
        activation.result = ItemUseResult.EFFECT_REJECTED;

        assertEquals(ItemUseResult.EFFECT_REJECTED, slots.tryUse(0));

        assertSame(original, slots.getSlot(0));
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(0, saves);
    }

    @Test
    void saveFailureClearsTheUsedSlotAndRetryDoesNotReplayTheEffect() {
        assertTrue(slots.equipPurchased(0, ITEM));
        denySave = true;

        assertEquals(ItemUseResult.SUCCESS, slots.tryUse(0));

        assertNull(slots.getSlot(0));
        assertEquals(0, assets.getItemQuantity(ITEM));
        assertEquals(ShopController.SaveStatus.FAILED, shop.getSaveStatus());
        denySave = false;
        assertTrue(shop.retrySave());
        assertEquals(1, activation.calls);
        assertEquals(0, assets.getItemQuantity(ITEM));
        assertEquals(ShopController.SaveStatus.SAVED, shop.getSaveStatus());
    }

    @Test
    void rebindingForAnotherStageKeepsSlotsButUsesTheNewShipEffect() {
        assertTrue(slots.equipPurchased(0, ITEM));
        assertTrue(slots.tryCollect(FIELD));
        ItemSlots.Slot purchased = slots.getSlot(0);
        ItemSlots.Slot field = slots.getSlot(1);
        TestActivation nextStage = new TestActivation();

        slots.bind(Map.of(ITEM, nextStage, FIELD, nextStage));

        assertSame(purchased, slots.getSlot(0));
        assertSame(field, slots.getSlot(1));
        assertEquals(ItemUseResult.SUCCESS, slots.tryUse(1));
        assertEquals(0, activation.calls);
        assertEquals(1, nextStage.calls);
        assertEquals(1, assets.getItemQuantity(ITEM));
    }

    @Test
    void endingTheRunClearsSlotsButKeepsUnusedPurchasedStock() {
        assertTrue(slots.equipPurchased(0, ITEM));
        assertTrue(slots.tryCollect(FIELD));

        slots.clear();

        assertNull(slots.getSlot(0));
        assertNull(slots.getSlot(1));
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(100, assets.getBalance());
        assertEquals(0, saves);
    }

    @Test
    void unexpectedFailureCannotBeBypassedByRebindingEffects() {
        assertTrue(slots.tryCollect(FIELD));
        activation.onActivate = () -> { throw new IllegalStateException("Test effect failure"); };

        assertThrows(IllegalStateException.class, () -> slots.tryUse(0));
        assertThrows(IllegalStateException.class, () -> slots.bind(Map.of(FIELD, new TestActivation())));
        assertThrows(IllegalStateException.class, () -> slots.tryUse(0));
        assertEquals(1, activation.calls);
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(0, saves);
    }

    @Test
    void activationCannotCollectAnotherItemEvenIfItCatchesTheError() {
        assertTrue(slots.tryCollect(FIELD));
        activation.onActivate = () -> assertThrows(IllegalStateException.class,
                () -> slots.tryCollect(ITEM));

        assertThrows(IllegalStateException.class, () -> slots.tryUse(0));

        assertNull(slots.getSlot(1));
        assertThrows(IllegalStateException.class, () -> slots.tryUse(0));
        assertEquals(1, activation.calls);
    }

    private static final class TestActivation implements ConsumableActivation {
        private ItemUseResult result = ItemUseResult.SUCCESS;
        private Runnable onActivate;
        private int calls;

        @Override
        public ItemUseResult validate() {
            return ItemUseResult.SUCCESS;
        }

        @Override
        public ItemUseResult activate() {
            calls++;
            if (onActivate != null) onActivate.run();
            return result;
        }
    }
}
