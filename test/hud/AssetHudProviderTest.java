package hud;

import currency.ConsumableActivation;
import currency.ItemUseResult;
import currency.PlayerAssets;
import currency.PlayerAssetsState;
import currency.RewardSystem;
import engine.AssetSaveState;
import engine.Core;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import shop.ItemSlots;
import shop.ShopController;
import shop.ShopTestFixtures;
import static org.junit.jupiter.api.Assertions.*;

class AssetHudProviderTest {
    @TempDir
    Path temp;
    private PlayerAssets assets;
    private RewardSystem rewards;
    private ShopController shop;
    private ItemSlots slots;
    private AssetHudProvider hud;
    private Path save;

    @BeforeEach
    void setUp() {
        assets = ShopTestFixtures.fundedAssets();
        rewards = new RewardSystem(assets);
        save = temp.resolve("assets.properties");
        shop = ShopTestFixtures.controller(assets,
                new ShopTestFixtures.SaveFile(Core.getFileManager(), save));
        slots = new ItemSlots(shop);
        slots.bind(Map.of(ShopTestFixtures.CONSUMABLE, new TestActivation(),
                "field.item", new TestActivation()));
        hud = new AssetHudProvider(shop, rewards, slots, Map.of("field.item", "Field item"));
    }

    @Test
    void creditsIncludePendingRewardsAndDoNotChangeOnSettlement() {
        assertTrue(rewards.rewardEnemy("BASIC"));
        AssetHudSnapshot before = hud.snapshot();
        assertEquals(1010, before.getCredits());
        assertEquals(1000, assets.getBalance());
        assertEquals(10, rewards.getPendingCurrency());
        assertFalse(Files.exists(save));

        assertTrue(rewards.settle());

        assertEquals(1010, hud.snapshot().getCredits());
        assertEquals(1010, before.getCredits());
        assertEquals(0, rewards.getPendingCurrency());
        assertEquals("", hud.snapshot().getNotice());
    }

    @Test
    void creditsDoNotOverflowWhenSettlementCannotFitTheBalance() {
        assertTrue(assets.restore(new PlayerAssetsState(Integer.MAX_VALUE, Set.of(), Map.of())));
        assertTrue(rewards.rewardEnemy("BASIC"));

        assertEquals((long) Integer.MAX_VALUE + 10, hud.snapshot().getCredits());
        assertFalse(rewards.settle());
        assertEquals((long) Integer.MAX_VALUE + 10, hud.snapshot().getCredits());
    }

    @Test
    void snapshotsKeepSlotOrderSourceAndTotalPurchasedQuantity() {
        assertTrue(assets.restore(new PlayerAssetsState(1000, Set.of(),
                Map.of(ShopTestFixtures.CONSUMABLE, 3))));
        assertTrue(slots.equipPurchased(0, ShopTestFixtures.CONSUMABLE));
        assertTrue(slots.tryCollect("field.item"));
        AssetHudSnapshot before = hud.snapshot();

        assertEquals(1, before.getSlots().get(0).getKeyNumber());
        assertEquals("Item A", before.getSlots().get(0).getName());
        assertEquals(3, before.getSlots().get(0).getPurchasedQuantity());
        assertEquals(ItemSlots.Source.PURCHASED, before.getSlots().get(0).getSource());
        assertEquals(2, before.getSlots().get(1).getKeyNumber());
        assertEquals("Field item", before.getSlots().get(1).getName());
        assertEquals(ItemSlots.Source.FIELD, before.getSlots().get(1).getSource());
        assertEquals(0, before.getSlots().get(1).getPurchasedQuantity());
        assertThrows(UnsupportedOperationException.class, () -> before.getSlots().clear());
        slots.clear();
        assertFalse(before.getSlots().get(0).isEmpty());
        assertTrue(hud.snapshot().getSlots().get(0).isEmpty());
        assertFalse(Files.exists(save));
    }

    @Test
    void warningTracksTheSharedSaveResult() {
        AssetSaveState state = new AssetSaveState();
        shop = new ShopController(assets, Core.getFileManager(), ShopTestFixtures.products(), state);
        slots = new ItemSlots(shop);
        hud = new AssetHudProvider(shop, rewards, slots, Map.of());
        state.recordFailure();
        AssetHudSnapshot failed = hud.snapshot();
        assertTrue(failed.isWarning());
        assertEquals("Save failed", failed.getNotice());
        state.recordSuccess();
        assertFalse(hud.snapshot().isWarning());
        assertEquals("", hud.snapshot().getNotice());
        assertTrue(failed.isWarning());
    }

    private static final class TestActivation implements ConsumableActivation {
        @Override
        public ItemUseResult validate() {
            return ItemUseResult.SUCCESS;
        }

        @Override
        public ItemUseResult activate() {
            return ItemUseResult.SUCCESS;
        }
    }
}
