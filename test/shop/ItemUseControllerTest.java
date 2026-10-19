package shop;

import currency.ConsumableActivation;
import currency.ItemUseResult;
import currency.PlayerAssets;
import currency.PlayerAssetsState;
import currency.PurchaseResult;
import currency.RewardSystem;
import currency.ShopCatalog.Category;
import engine.Core;
import engine.FileManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ItemUseControllerTest {
    private static final String ITEM = ShopTestFixtures.CONSUMABLE;
    private static final String UNLOCK = ShopTestFixtures.UNLOCK;

    @TempDir
    Path temp;
    private FileManager files;
    private Path path;
    private PlayerAssets assets;
    private CountingSaver saver;
    private ShopController shop;
    private TestActivation activation;
    private ItemUseController uses;

    @BeforeEach
    void setUp() {
        files = Core.getFileManager();
        path = temp.resolve("assets.properties");
        assets = ShopTestFixtures.fundedAssets();
        saver = new CountingSaver(new ShopTestFixtures.SaveFile(files, path));
        shop = new ShopController(assets, files, ShopTestFixtures.products(), saver);
        activation = new TestActivation();
        uses = shop.createItemUseController(Collections.singletonMap(ITEM, activation));
    }

    @Test
    void constructionDoesNotLoadSaveOrReplaceSharedAssets() throws Exception {
        stock(2);
        Files.write(path, new byte[] { 1, 2, 3 });
        PlayerAssetsState before = assets.snapshot();

        ItemUseController other = shop.createItemUseController(Collections.singletonMap(ITEM, activation));

        assertState(before, shop.snapshot());
        assertEquals(ShopController.SaveStatus.NOT_NEEDED, other.getSaveStatus());
        assertEquals(0, saver.calls);
        assertArrayEquals(new byte[] { 1, 2, 3 }, Files.readAllBytes(path));
    }

    @Test
    void purchaseUseSaveReloadUsesOneAssetStateWithoutChargingAgain() throws Exception {
        assertEquals(PurchaseResult.SUCCESS, shop.purchase(ITEM));
        assertEquals(PurchaseResult.SUCCESS, shop.purchase(ITEM));
        assertEquals(PurchaseResult.SUCCESS, shop.purchase(UNLOCK));

        assertEquals(ItemUseResult.SUCCESS, uses.tryUse(ITEM));

        assertEquals(300, assets.getBalance());
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(1, activation.calls);
        assertEquals(4, saver.calls);
        assertEquals(ShopController.SaveStatus.SAVED, uses.getSaveStatus());
        assertState(assets.snapshot(), files.loadPlayerAssets(path));

        PlayerAssets restored = new PlayerAssets();
        assertTrue(restored.restore(files.loadPlayerAssets(path)));
        ShopController reopened = new ShopController(restored, files, ShopTestFixtures.products(), saver);
        TestActivation nextEffect = new TestActivation();
        ItemUseController nextRun = reopened.createItemUseController(Collections.singletonMap(ITEM, nextEffect));
        assertEquals(ItemUseResult.SUCCESS, nextRun.tryUse(ITEM));
        assertEquals(300, restored.getBalance());
        assertTrue(restored.isUnlocked(UNLOCK));
        assertFalse(restored.hasItem(ITEM));
        assertEquals(1, nextEffect.calls);
        assertState(restored.snapshot(), files.loadPlayerAssets(path));
    }

    @Test
    void sharedProductDefinitionsControlDisplayPurchaseAndUse() {
        List<ShopProduct> definitions = new ArrayList<>();
        definitions.add(new ShopProduct("TEST_CUSTOM", "Custom item", Category.INVENTORY, 17));
        definitions.add(new ShopProduct(UNLOCK, "Unlock", Category.UNLOCKABLE, 30));
        ShopController configured = new ShopController(assets, files, definitions, saver);
        definitions.clear();
        Map<String, ConsumableActivation> adapters = new HashMap<>();
        adapters.put("TEST_CUSTOM", activation);
        adapters.put(UNLOCK, activation);
        adapters.put("TEST_UNKNOWN", activation);
        ItemUseController configuredUses = configured.createItemUseController(adapters);
        adapters.clear();

        assertEquals(17, configured.getProducts().get(0).getPrice());
        assertEquals(PurchaseResult.SUCCESS, configured.purchase("TEST_CUSTOM"));
        assertEquals(983, assets.getBalance());
        assertEquals(ItemUseResult.SUCCESS, configuredUses.tryUse("TEST_CUSTOM"));
        assertEquals(983, assets.getBalance());
        assertEquals(0, assets.getItemQuantity("TEST_CUSTOM"));
        assertEquals(ItemUseResult.UNSUPPORTED_ITEM, configuredUses.tryUse(UNLOCK));
        assertEquals(ItemUseResult.UNSUPPORTED_ITEM, configuredUses.tryUse("TEST_UNKNOWN"));
        assertEquals(1, activation.calls);
        assertEquals(2, saver.calls);
    }

    @Test
    void invalidUnsupportedAndOutOfStockRequestsDoNotSave() {
        assertEquals(ItemUseResult.INVALID_ITEM_ID, uses.tryUse(null));
        assertEquals(ItemUseResult.INVALID_ITEM_ID, uses.tryUse(" "));
        assertEquals(ItemUseResult.UNSUPPORTED_ITEM, uses.tryUse("UNKNOWN"));
        assertEquals(ItemUseResult.UNSUPPORTED_ITEM, uses.tryUse(UNLOCK));
        assertEquals(ItemUseResult.OUT_OF_STOCK, uses.tryUse(ITEM));
        assertEquals(1000, assets.getBalance());
        assertEquals(0, activation.calls);
        assertEquals(0, saver.calls);
        assertFalse(Files.exists(path));
    }

    @Test
    void validationRejectionDoesNotConsumeActivateOrSave() {
        stock(1);
        PlayerAssetsState before = assets.snapshot();
        for (ItemUseResult result : rejections()) {
            activation.validation = result;
            assertEquals(result, uses.tryUse(ITEM));
            assertState(before, assets.snapshot());
        }
        assertEquals(0, activation.calls);
        assertEquals(0, saver.calls);
    }

    @Test
    void activationRejectionRestoresLastUnitWithoutSaving() {
        stock(1);
        PlayerAssetsState before = assets.snapshot();
        activation.onActivate = () -> assertFalse(assets.hasItem(ITEM));
        for (ItemUseResult result : rejections()) {
            activation.result = result;
            assertEquals(result, uses.tryUse(ITEM));
            assertState(before, assets.snapshot());
        }
        assertEquals(3, activation.calls);
        assertEquals(0, saver.calls);
        activation.result = ItemUseResult.SUCCESS;
        assertEquals(ItemUseResult.SUCCESS, uses.tryUse(ITEM));
        assertFalse(assets.hasItem(ITEM));
        assertEquals(1, saver.calls);
    }

    @Test
    void failedSaveKeepsConsumptionAndRetrySavesLatestAssetsWithoutReplayingEffect() throws Exception {
        stock(2);
        saver.save(assets.snapshot());
        byte[] previous = Files.readAllBytes(path);
        Path blocker = temp.resolve("not-a-directory");
        Files.write(blocker, new byte[] { 1 });
        saver.file.setPath(blocker.resolve("assets.properties"));

        assertEquals(ItemUseResult.SUCCESS, uses.tryUse(ITEM));
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(1000, assets.getBalance());
        assertEquals(1, activation.calls);
        assertEquals(ShopController.SaveStatus.FAILED, uses.getSaveStatus());
        assertEquals(ShopController.SaveStatus.FAILED, shop.getSaveStatus());
        assertArrayEquals(previous, Files.readAllBytes(path));
        assertFalse(uses.retrySave());
        assertEquals(1, activation.calls);
        assertEquals(1, assets.getItemQuantity(ITEM));

        RewardSystem rewards = new RewardSystem(assets);
        assertTrue(rewards.rewardEnemy("BASIC"));
        assertTrue(rewards.settle());
        saver.file.setPath(path);
        assertTrue(uses.retrySave());
        assertEquals(1010, files.loadPlayerAssets(path).getBalance());
        assertState(assets.snapshot(), files.loadPlayerAssets(path));
        assertEquals(ShopController.SaveStatus.SAVED, shop.getSaveStatus());
        int writes = saver.calls;
        assertTrue(shop.retrySave());
        assertTrue(uses.retrySave());
        assertEquals(writes, saver.calls);
        assertEquals(1, activation.calls);
        assertEquals(1, assets.getItemQuantity(ITEM));
    }

    @Test
    void securityFailureIsSeparateFromSuccessfulActivation() {
        stock(1);
        saver.denied = true;
        assertEquals(ItemUseResult.SUCCESS, uses.tryUse(ITEM));
        assertEquals(ShopController.SaveStatus.FAILED, uses.getSaveStatus());
        assertFalse(assets.hasItem(ITEM));
        assertEquals(1, activation.calls);
        saver.denied = false;
        assertTrue(uses.retrySave());
        assertEquals(ShopController.SaveStatus.SAVED, uses.getSaveStatus());
        assertEquals(1, activation.calls);
    }

    @Test
    void laterRejectionDoesNotClearOrAutomaticallyRetryFailedSave() {
        stock(2);
        saver.denied = true;
        assertEquals(ItemUseResult.SUCCESS, uses.tryUse(ITEM));
        int writes = saver.calls;
        activation.validation = ItemUseResult.ALREADY_ACTIVE;

        assertEquals(ItemUseResult.ALREADY_ACTIVE, uses.tryUse(ITEM));
        assertEquals(ItemUseResult.UNSUPPORTED_ITEM, uses.tryUse("UNKNOWN"));
        assertEquals(ShopController.SaveStatus.FAILED, uses.getSaveStatus());
        assertEquals(writes, saver.calls);
        assertEquals(1, assets.getItemQuantity(ITEM));
    }

    @Test
    void purchaseAfterFailedUseSavePersistsBothWithoutReplayingEffect() throws Exception {
        stock(2);
        saver.denied = true;
        assertEquals(ItemUseResult.SUCCESS, uses.tryUse(ITEM));
        saver.denied = false;

        assertEquals(PurchaseResult.SUCCESS, shop.purchase(UNLOCK));

        assertEquals(ShopController.SaveStatus.SAVED, uses.getSaveStatus());
        assertEquals(500, files.loadPlayerAssets(path).getBalance());
        assertEquals(1, files.loadPlayerAssets(path).getItemQuantities().get(ITEM));
        assertTrue(files.loadPlayerAssets(path).getUnlockedContentIds().contains(UNLOCK));
        assertEquals(1, activation.calls);
    }

    @Test
    void useAfterFailedPurchaseSavePersistsLatestSharedState() throws Exception {
        saver.denied = true;
        assertEquals(PurchaseResult.SUCCESS, shop.purchase(ITEM));
        assertEquals(ShopController.SaveStatus.FAILED, uses.getSaveStatus());
        saver.denied = false;

        assertEquals(ItemUseResult.SUCCESS, uses.tryUse(ITEM));

        assertEquals(900, files.loadPlayerAssets(path).getBalance());
        assertTrue(files.loadPlayerAssets(path).getItemQuantities().isEmpty());
        assertEquals(ShopController.SaveStatus.SAVED, shop.getSaveStatus());
        assertEquals(1, activation.calls);
    }

    @Test
    void multipleGameplayContextsShareStockAndSaveStatus() throws Exception {
        stock(2);
        TestActivation otherEffect = new TestActivation();
        ItemUseController other = shop.createItemUseController(Collections.singletonMap(ITEM, otherEffect));
        saver.denied = true;
        assertEquals(ItemUseResult.SUCCESS, uses.tryUse(ITEM));
        assertEquals(ShopController.SaveStatus.FAILED, other.getSaveStatus());
        saver.denied = false;
        assertTrue(other.retrySave());
        assertEquals(ShopController.SaveStatus.SAVED, uses.getSaveStatus());
        assertEquals(0, otherEffect.calls);
        assertEquals(ItemUseResult.SUCCESS, other.tryUse(ITEM));
        assertEquals(ItemUseResult.OUT_OF_STOCK, uses.tryUse(ITEM));
        assertEquals(1, activation.calls);
        assertEquals(1, otherEffect.calls);
        assertTrue(files.loadPlayerAssets(path).getItemQuantities().isEmpty());
    }

    @Test
    void unexpectedActivationFailureDoesNotSaveRefundOrAutomaticallyReplaceService() throws Exception {
        stock(2);
        saver.save(assets.snapshot());
        byte[] previous = Files.readAllBytes(path);
        activation.onActivate = () -> { throw new IllegalStateException("Partial effect"); };

        assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(1, saver.calls);
        assertArrayEquals(previous, Files.readAllBytes(path));
        activation.onActivate = null;
        assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));
        assertEquals(1, activation.calls);
        assertEquals(1, saver.calls);
    }

    @Test
    void validationFailureLeavesStockAndBlocksFurtherUse() {
        stock(1);
        activation.onValidate = () -> { throw new IllegalStateException("Invalid context"); };
        assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(0, activation.calls);
        assertEquals(0, saver.calls);
        activation.onValidate = null;
        assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));
    }

    @Test
    void activationErrorRemainsDistinctAndDoesNotWrite() {
        stock(1);
        AssertionError error = new AssertionError("Partial effect");
        activation.onActivate = () -> { throw error; };
        assertSame(error, assertThrows(AssertionError.class, () -> uses.tryUse(ITEM)));
        assertEquals(0, saver.calls);
        assertFalse(assets.hasItem(ITEM));
        assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));
    }

    @Test
    void nestedSaveCannotPersistAnUncommittedReservation() {
        stock(2);
        saver.denied = true;
        assertEquals(ItemUseResult.SUCCESS, uses.tryUse(ITEM));
        saver.denied = false;
        int writes = saver.calls;
        activation.onActivate = () -> assertThrows(IllegalStateException.class, () -> uses.retrySave());
        activation.result = ItemUseResult.EFFECT_REJECTED;

        assertEquals(ItemUseResult.EFFECT_REJECTED, uses.tryUse(ITEM));
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(writes, saver.calls);
        assertEquals(ShopController.SaveStatus.FAILED, uses.getSaveStatus());
        assertTrue(uses.retrySave());
    }

    @Test
    void nestedPurchaseCannotChangeOrSaveAssetsDuringActivation() {
        stock(1);
        activation.onActivate = () -> shop.purchase(UNLOCK);
        assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));
        assertEquals(1000, assets.getBalance());
        assertFalse(assets.isUnlocked(UNLOCK));
        assertEquals(0, saver.calls);
        assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));
    }

    @Test
    void nestedUseInAnotherContextCannotConsumeAnotherUnit() {
        stock(2);
        TestActivation otherEffect = new TestActivation();
        ItemUseController other = shop.createItemUseController(Collections.singletonMap(ITEM, otherEffect));
        activation.onActivate = () -> other.tryUse(ITEM);
        assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(0, otherEffect.calls);
        assertEquals(0, saver.calls);
    }

    @Test
    void caughtReentryDuringValidationStillBlocksOuterAndLaterUse() {
        stock(2);
        activation.onValidate = () -> assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));

        assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));
        assertEquals(2, assets.getItemQuantity(ITEM));
        assertEquals(0, activation.calls);
        assertEquals(0, saver.calls);
        assertEquals(ShopController.SaveStatus.NOT_NEEDED, uses.getSaveStatus());
        assertFalse(Files.exists(path));

        activation.onValidate = null;
        assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));
        assertEquals(2, assets.getItemQuantity(ITEM));
        assertEquals(0, activation.calls);
        assertEquals(0, saver.calls);
    }

    @Test
    void caughtReentryDuringActivationStillBlocksOuterAndLaterUse() {
        stock(2);
        activation.onActivate = () -> assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));

        assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(1, activation.calls);
        assertEquals(0, saver.calls);
        assertEquals(ShopController.SaveStatus.NOT_NEEDED, uses.getSaveStatus());
        assertFalse(Files.exists(path));

        activation.onActivate = null;
        assertThrows(IllegalStateException.class, () -> uses.tryUse(ITEM));
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(1, activation.calls);
        assertEquals(0, saver.calls);
    }

    @Test
    void invalidAdapterDefinitionsDoNotChangeAssetsOrSave() {
        PlayerAssetsState before = assets.snapshot();
        assertThrows(IllegalArgumentException.class, () -> shop.createItemUseController(null));
        assertThrows(IllegalArgumentException.class,
                () -> shop.createItemUseController(Collections.singletonMap(ITEM, null)));
        assertThrows(IllegalArgumentException.class,
                () -> shop.createItemUseController(Collections.singletonMap(" ", activation)));
        assertState(before, assets.snapshot());
        assertEquals(0, saver.calls);
    }

    @Test
    void emptyProductionCatalogDoesNotEnableTestProducts() {
        stock(1);
        ShopController production = new ShopController(assets, files);
        ItemUseController productionUses = production.createItemUseController(Collections.singletonMap(ITEM, activation));
        assertTrue(production.getProducts().isEmpty());
        assertEquals(ItemUseResult.UNSUPPORTED_ITEM, productionUses.tryUse(ITEM));
        assertEquals(1, assets.getItemQuantity(ITEM));
        assertEquals(0, activation.calls);
        assertEquals(ShopController.SaveStatus.NOT_NEEDED, productionUses.getSaveStatus());
    }

    @Test
    void retryWithoutFailureDoesNotCreateSave() {
        assertTrue(uses.retrySave());
        assertEquals(0, saver.calls);
        assertEquals(ShopController.SaveStatus.NOT_NEEDED, uses.getSaveStatus());
        assertFalse(Files.exists(path));
    }

    private void stock(int quantity) {
        assertTrue(assets.restore(new PlayerAssetsState(1000, Collections.<String>emptySet(),
                Collections.singletonMap(ITEM, quantity))));
    }

    private static void assertState(PlayerAssetsState expected, PlayerAssetsState actual) {
        assertEquals(expected.getBalance(), actual.getBalance());
        assertEquals(expected.getUnlockedContentIds(), actual.getUnlockedContentIds());
        assertEquals(expected.getItemQuantities(), actual.getItemQuantities());
    }

    private static ItemUseResult[] rejections() {
        return new ItemUseResult[] { ItemUseResult.INVALID_CONTEXT,
                ItemUseResult.ALREADY_ACTIVE, ItemUseResult.EFFECT_REJECTED };
    }

    private static final class CountingSaver implements ShopController.AssetSaver {
        private final ShopTestFixtures.SaveFile file;
        private int calls;
        private boolean denied;

        private CountingSaver(ShopTestFixtures.SaveFile file) {
            this.file = file;
        }

        @Override
        public void save(PlayerAssetsState state) throws IOException {
            calls++;
            if (denied) throw new SecurityException("Test access denied");
            file.save(state);
        }
    }

    private static final class TestActivation implements ConsumableActivation {
        private ItemUseResult validation = ItemUseResult.SUCCESS;
        private ItemUseResult result = ItemUseResult.SUCCESS;
        private Runnable onValidate;
        private Runnable onActivate;
        private int calls;

        @Override
        public ItemUseResult validate() {
            if (onValidate != null) onValidate.run();
            return validation;
        }

        @Override
        public ItemUseResult activate() {
            calls++;
            if (onActivate != null) onActivate.run();
            return result;
        }
    }
}
