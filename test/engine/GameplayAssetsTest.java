package engine;

import campaign.CampaignManager;
import campaign.CampaignStageData;
import currency.ConsumableActivation;
import currency.ItemUseResult;
import currency.PlayerAssets;
import currency.PlayerAssetsState;
import currency.PurchaseResult;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import shop.ItemSlots;
import shop.ShopController;
import shop.ShopTestFixtures;
import static org.junit.jupiter.api.Assertions.*;

class GameplayAssetsTest {
    @TempDir
    Path directory;
    private String previousDirectory;
    private PlayerAssets assets;
    private GameplayAssets gameplay;
    private int effects;
    private int previousCampaignStage;

    @BeforeEach
    void setUp() throws Exception {
        previousCampaignStage = CampaignManager.getMaxCampaignStage();
        setCampaignStage(1);
        previousDirectory = System.getProperty("invaders.storageDir");
        System.setProperty("invaders.storageDir", directory.toString());
        assets = ShopTestFixtures.fundedAssets();
        gameplay = new GameplayAssets(assets, Core.getFileManager(), ShopTestFixtures.products());
    }

    @AfterEach
    void restoreDirectory() throws Exception {
        setCampaignStage(previousCampaignStage);
        if (previousDirectory == null) System.clearProperty("invaders.storageDir");
        else System.setProperty("invaders.storageDir", previousDirectory);
    }

    @Test
    void startupAndSettlementUseTheSameLoadedAssets() throws Exception {
        PlayerAssetsState saved = new PlayerAssetsState(75, Collections.emptySet(), Collections.emptyMap());
        GameplayAssets initialized = Core.initializeGameplayAssets(assets, Core.getFileManager(), saved);
        assertEquals(75, initialized.getShop().snapshot().getBalance());
        assertTrue(initialized.getShop().getProducts().isEmpty());
        assertFalse(Files.exists(directory.resolve(FileManager.ASSETS_FILENAME)));
        assertTrue(initialized.getRewards().rewardEnemy("BASIC"));
        initialized.beginRun();
        assertEquals(85, initialized.snapshot().getCredits());

        assertEquals(SettlementResult.SUCCESS, initialized.settleStage());

        assertEquals(85, assets.getBalance());
        assertEquals(85, initialized.snapshot().getCredits());
        assertEquals(0, initialized.getRewards().getPendingCurrency());
        assertEquals(ShopController.SaveStatus.SAVED, initialized.getShop().getSaveStatus());
        assertEquals(85, Core.getFileManager().loadPlayerAssets().getBalance());
    }

    @Test
    void runEndDropsFieldSlotsAndTheNextRunEquipsOnlyPurchasedStock() {
        assertEquals(PurchaseResult.SUCCESS, gameplay.getShop().purchase(ShopTestFixtures.CONSUMABLE));
        gameplay.beginRun();
        gameplay.bindItemEffects(adapters(), Map.of("field.item", "Field item"));
        ItemSlots slots = gameplay.getSlots();
        assertEquals(ItemSlots.Source.PURCHASED, slots.getSlot(0).getSource());
        assertTrue(slots.tryCollect("field.item"));
        assertThrows(IllegalStateException.class, gameplay::beginRun);

        gameplay.endRun();

        assertNull(slots.getSlot(0));
        assertNull(slots.getSlot(1));
        assertEquals(1, assets.getItemQuantity(ShopTestFixtures.CONSUMABLE));
        assertThrows(IllegalStateException.class, gameplay::getSlots);
        gameplay.beginRun();
        gameplay.bindItemEffects(adapters(), Map.of());
        assertNotSame(slots, gameplay.getSlots());
        assertEquals(ShopTestFixtures.CONSUMABLE, gameplay.getSlots().getSlot(0).getItemId());
        assertNull(gameplay.getSlots().getSlot(1));
        assertEquals(0, effects);
    }

    @Test
    void failedSettlementSaveRetriesOnlyPersistenceAndSharesTheWarning() throws Exception {
        gameplay.beginRun();
        Path save = directory.resolve(FileManager.ASSETS_FILENAME);
        Files.createDirectory(save);
        Path blocker = Files.writeString(save.resolve("blocker"), "keep");
        assertTrue(gameplay.getRewards().rewardEnemy("BASIC"));

        assertEquals(SettlementResult.SAVE_FAILED, gameplay.settleStage());
        assertEquals(1010, assets.getBalance());
        assertTrue(gameplay.snapshot().isWarning());
        assertEquals(ShopController.SaveStatus.FAILED, gameplay.getShop().getSaveStatus());
        assertTrue(gameplay.getRewards().rewardEnemy("TANK"));
        Files.delete(blocker);
        Files.delete(save);

        assertEquals(SettlementResult.SUCCESS, gameplay.retrySave());

        assertFalse(gameplay.snapshot().isWarning());
        assertEquals(1010, Core.getFileManager().loadPlayerAssets().getBalance());
        assertEquals(50, gameplay.getRewards().getPendingCurrency());
        assertEquals(1060, gameplay.snapshot().getCredits());
    }

    @Test
    void failedBalanceTransferKeepsRewardsAcrossRuns() {
        assertTrue(assets.restore(new PlayerAssetsState(Integer.MAX_VALUE,
                Collections.emptySet(), Collections.emptyMap())));
        gameplay.beginRun();
        assertTrue(gameplay.getRewards().rewardEnemy("BASIC"));
        assertEquals(SettlementResult.SETTLEMENT_FAILED, gameplay.settleStage());
        gameplay.endRun();
        gameplay.beginRun();

        assertEquals(10, gameplay.getRewards().getPendingCurrency());
        assertEquals((long) Integer.MAX_VALUE + 10, gameplay.snapshot().getCredits());
        assertFalse(Files.exists(directory.resolve(FileManager.ASSETS_FILENAME)));
    }

    @Test
    void normalExitSettlesEarnedRewardsWithoutAddingAClearBonus() throws Exception {
        gameplay.beginRun();
        assertTrue(gameplay.getRewards().rewardEnemy("BASIC"));

        assertEquals(SettlementResult.SUCCESS, gameplay.prepareExit());
        gameplay.endRun();
        assertEquals(SettlementResult.SUCCESS, gameplay.prepareExit());

        assertEquals(1010, assets.getBalance());
        assertEquals(0, gameplay.getRewards().getPendingCurrency());
        assertEquals(1010, Core.getFileManager().loadPlayerAssets().getBalance());
    }

    @Test
    void exitSaveFailureRetainsProgressAndRetryDoesNotPayAgain() throws Exception {
        gameplay.beginRun();
        assertTrue(gameplay.getRewards().rewardEnemy("BASIC"));
        Path save = directory.resolve(FileManager.ASSETS_FILENAME);
        Files.createDirectory(save);
        Path blocker = Files.writeString(save.resolve("blocker"), "keep");

        assertEquals(SettlementResult.SAVE_FAILED, gameplay.prepareExit());
        assertEquals(1010, assets.getBalance());
        assertEquals(0, gameplay.getRewards().getPendingCurrency());
        assertEquals(SettlementResult.SAVE_FAILED, gameplay.prepareExit());
        Files.delete(blocker);
        Files.delete(save);

        assertEquals(SettlementResult.SUCCESS, gameplay.prepareExit());
        assertEquals(1010, Core.getFileManager().loadPlayerAssets().getBalance());
    }

    @Test
    void exitPreparationRetainsRewardsThatDidNotFitTheBalance() {
        assertTrue(assets.restore(new PlayerAssetsState(Integer.MAX_VALUE,
                Collections.emptySet(), Collections.emptyMap())));
        assertTrue(gameplay.getRewards().rewardEnemy("BASIC"));
        assertEquals(SettlementResult.SETTLEMENT_FAILED, gameplay.prepareExit());
        assertEquals(10, gameplay.getRewards().getPendingCurrency());
        assertFalse(Files.exists(directory.resolve(FileManager.ASSETS_FILENAME)));

        assertEquals(PurchaseResult.SUCCESS, gameplay.getShop().purchase(ShopTestFixtures.CONSUMABLE));
        assertEquals(SettlementResult.SUCCESS, gameplay.prepareExit());
        assertEquals(Integer.MAX_VALUE - 90, assets.getBalance());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failedActivationKeepsItsCauseWhenTheRunEnds(boolean purchased) throws Exception {
        assertTrue(assets.restore(new PlayerAssetsState(1000, Collections.emptySet(),
                Map.of(ShopTestFixtures.CONSUMABLE, 1))));
        Core.getFileManager().savePlayerAssets(assets.snapshot());
        Path save = directory.resolve(FileManager.ASSETS_FILENAME);
        byte[] saved = Files.readAllBytes(save);
        IllegalArgumentException cause = new IllegalArgumentException("Test effect failure");
        String itemId = purchased ? ShopTestFixtures.CONSUMABLE : "field.item";
        Map<String, ConsumableActivation> broken = Map.of(itemId, new ConsumableActivation() {
            @Override
            public ItemUseResult validate() {
                return ItemUseResult.SUCCESS;
            }

            @Override
            public ItemUseResult activate() {
                effects++;
                throw cause;
            }
        });
        gameplay.beginRun();
        gameplay.bindItemEffects(broken, Map.of());
        ItemSlots slots = gameplay.getSlots();
        if (!purchased) assertTrue(slots.tryCollect(itemId));

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> {
            try {
                slots.tryUse(0);
            } finally {
                gameplay.endRun();
            }
        });

        assertSame(cause, failure.getCause());
        assertThrows(IllegalStateException.class, gameplay::getSlots);
        assertThrows(IllegalStateException.class, gameplay::beginRun);
        assertThrows(IllegalStateException.class, () -> slots.tryUse(0));
        assertThrows(IllegalStateException.class, () -> slots.bind(adapters()));
        assertEquals(1, effects);
        assertEquals(purchased ? 0 : 1, assets.getItemQuantity(ShopTestFixtures.CONSUMABLE));
        assertEquals(1000, assets.getBalance());
        assertArrayEquals(saved, Files.readAllBytes(save));
    }

    @Test
    void clearedStageRemainsUnlockedWhenRewardSettlementOverflows() {
        assertTrue(assets.restore(new PlayerAssetsState(Integer.MAX_VALUE,
                Collections.emptySet(), Collections.emptyMap())));
        assertTrue(gameplay.getRewards().rewardEnemy("BASIC"));

        assertEquals(SettlementResult.SETTLEMENT_FAILED,
                Core.finishCampaignStage(gameplay, 1, true));

        assertEquals(2, CampaignManager.getMaxCampaignStage());
        assertEquals(Integer.MAX_VALUE, assets.getBalance());
        assertEquals(10, gameplay.getRewards().getPendingCurrency());
        assertFalse(Files.exists(directory.resolve(FileManager.ASSETS_FILENAME)));
    }

    @Test
    void overflowedRewardsSurviveLaterCampaignStages() throws Exception {
        assertTrue(assets.restore(new PlayerAssetsState(Integer.MAX_VALUE,
                Collections.emptySet(), Collections.emptyMap())));
        gameplay.beginRun();
        gameplay.bindItemEffects(adapters(), Map.of());
        ItemSlots slots = gameplay.getSlots();
        assertTrue(slots.tryCollect("field.item"));
        assertTrue(gameplay.getRewards().rewardEnemy("BASIC"));

        assertEquals(SettlementResult.SETTLEMENT_FAILED,
                Core.finishCampaignStage(gameplay, 1, true));
        gameplay.bindItemEffects(adapters(), Map.of());
        assertTrue(gameplay.getRewards().rewardEnemy("TANK"));
        assertEquals(SettlementResult.SETTLEMENT_FAILED,
                Core.finishCampaignStage(gameplay, 2, true));

        assertSame(slots, gameplay.getSlots());
        assertEquals("field.item", slots.getSlot(0).getItemId());
        assertEquals(3, CampaignManager.getMaxCampaignStage());
        assertEquals(Integer.MAX_VALUE, assets.getBalance());
        assertEquals(60, gameplay.getRewards().getPendingCurrency());
        assertEquals((long) Integer.MAX_VALUE + 60, gameplay.snapshot().getCredits());
        assertFalse(Files.exists(directory.resolve(FileManager.ASSETS_FILENAME)));
        gameplay.endRun();

        assertEquals(PurchaseResult.SUCCESS, gameplay.getShop().purchase(ShopTestFixtures.CONSUMABLE));
        assertEquals(SettlementResult.SUCCESS, gameplay.prepareExit());
        assertEquals(SettlementResult.SUCCESS, gameplay.prepareExit());
        assertEquals(0, gameplay.getRewards().getPendingCurrency());
        assertEquals(Integer.MAX_VALUE - 40, assets.getBalance());
        assertEquals(Integer.MAX_VALUE - 40, Core.getFileManager().loadPlayerAssets().getBalance());
    }

    @Test
    void clearedStageRemainsUnlockedAfterSaveFailureAndRetry() throws Exception {
        assertTrue(gameplay.getRewards().rewardEnemy("BASIC"));
        Path save = directory.resolve(FileManager.ASSETS_FILENAME);
        Files.createDirectory(save);
        Path blocker = Files.writeString(save.resolve("blocker"), "keep");

        assertEquals(SettlementResult.SAVE_FAILED, Core.finishCampaignStage(gameplay, 1, true));
        assertEquals(2, CampaignManager.getMaxCampaignStage());
        assertEquals(1010, assets.getBalance());
        assertEquals(0, gameplay.getRewards().getPendingCurrency());
        Files.delete(blocker);
        Files.delete(save);

        assertEquals(SettlementResult.SUCCESS, gameplay.retrySave());
        assertEquals(2, CampaignManager.getMaxCampaignStage());
        assertEquals(1010, Core.getFileManager().loadPlayerAssets().getBalance());
    }

    @Test
    void failedStageSettlesEarnedRewardsWithoutUnlockingAnotherStage() {
        assertTrue(gameplay.getRewards().rewardEnemy("BASIC"));

        assertEquals(SettlementResult.SUCCESS, Core.finishCampaignStage(gameplay, 1, false));

        assertEquals(1, CampaignManager.getMaxCampaignStage());
        assertEquals(1010, assets.getBalance());
        assertEquals(0, gameplay.getRewards().getPendingCurrency());
    }

    @Test
    void finalStageDoesNotUnlockANonexistentStage() {
        int lastStage = CampaignStageData.getTotalStages();

        assertEquals(SettlementResult.SUCCESS, Core.finishCampaignStage(gameplay, lastStage, true));

        assertEquals(lastStage, CampaignManager.getMaxCampaignStage());
        assertEquals(1000, assets.getBalance());
    }

    private void setCampaignStage(int stage) throws Exception {
        Field highestStage = CampaignManager.class.getDeclaredField("maxCampaignStage");
        highestStage.setAccessible(true);
        highestStage.setInt(null, stage);
    }

    private Map<String, ConsumableActivation> adapters() {
        ConsumableActivation effect = new ConsumableActivation() {
            @Override
            public ItemUseResult validate() {
                return ItemUseResult.SUCCESS;
            }

            @Override
            public ItemUseResult activate() {
                effects++;
                return ItemUseResult.SUCCESS;
            }
        };
        return Map.of(ShopTestFixtures.CONSUMABLE, effect, "field.item", effect);
    }
}
