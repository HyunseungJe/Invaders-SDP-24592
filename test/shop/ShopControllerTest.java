package shop;

import currency.PlayerAssets;
import currency.PlayerAssetsState;
import currency.PurchaseResult;
import currency.RewardSystem;
import currency.ShopCatalog.Category;
import engine.Core;
import engine.FileManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ShopControllerTest {
    @TempDir
    Path temp;

    private FileManager files;
    private ShopTestFixtures.SaveFile saveFile;
    private Path save;
    private PlayerAssets assets;
    private ShopController shop;

    @BeforeEach
    void setUp() {
        files = Core.getFileManager();
        save = temp.resolve("assets.properties");
        saveFile = new ShopTestFixtures.SaveFile(files, save);
        assets = ShopTestFixtures.fundedAssets();
        shop = ShopTestFixtures.controller(assets, saveFile);
    }

    @Test
    void constructorKeepsAssetsAndSaveFileWithEmptyProductionCatalog() throws Exception {
        Files.write(save, new byte[] { 1, 2, 3 });
        byte[] before = Files.readAllBytes(save);

        ShopController production = new ShopController(assets, files);

        assertEquals(1000, production.snapshot().getBalance());
        assertTrue(production.getProducts().isEmpty());
        assertEquals(ShopController.SaveStatus.NOT_NEEDED, production.getSaveStatus());
        assertArrayEquals(before, Files.readAllBytes(save));
    }

    @Test
    void consumablePurchasesUseCatalogPriceAndSaveEachTime() throws Exception {
        assertEquals(PurchaseResult.SUCCESS, shop.purchase(ShopTestFixtures.CONSUMABLE));
        assertEquals(900, saveFile.load().getBalance());

        assertEquals(PurchaseResult.SUCCESS, shop.purchase(ShopTestFixtures.CONSUMABLE));
        assertEquals(800, assets.getBalance());
        assertEquals(2, assets.getItemQuantity(ShopTestFixtures.CONSUMABLE));
        assertEquals(800, saveFile.load().getBalance());
        assertEquals(2, saveFile.load().getItemQuantities().get(ShopTestFixtures.CONSUMABLE));
        assertEquals(ShopController.SaveStatus.SAVED, shop.getSaveStatus());
    }

    @Test
    void unlockIsChargedOnceAndRejectionDoesNotSave() throws Exception {
        assertEquals(PurchaseResult.SUCCESS, shop.purchase(ShopTestFixtures.UNLOCK));
        byte[] saved = Files.readAllBytes(save);

        assertEquals(PurchaseResult.ALREADY_UNLOCKED, shop.purchase(ShopTestFixtures.UNLOCK));
        assertEquals(500, assets.getBalance());
        assertTrue(assets.isUnlocked(ShopTestFixtures.UNLOCK));
        assertArrayEquals(saved, Files.readAllBytes(save));
    }

    @Test
    void insufficientFundsAndBadIdsDoNotChangeAssetsOrCreateSave() {
        PlayerAssets empty = new PlayerAssets();
        ShopController poorShop = ShopTestFixtures.controller(empty, saveFile);

        assertEquals(PurchaseResult.INSUFFICIENT_FUNDS, poorShop.purchase(ShopTestFixtures.CONSUMABLE));
        assertEquals(PurchaseResult.INSUFFICIENT_FUNDS, poorShop.purchase(ShopTestFixtures.UNLOCK));
        assertEquals(PurchaseResult.INVALID_ITEM_ID, shop.purchase(null));
        assertEquals(PurchaseResult.INVALID_ITEM_ID, shop.purchase(" "));
        assertEquals(PurchaseResult.CATEGORY_MISMATCH, shop.purchase("UNKNOWN"));
        assertEquals(1000, assets.getBalance());
        assertEquals(0, empty.getBalance());
        assertEquals(0, assets.getItemQuantity(ShopTestFixtures.CONSUMABLE));
        assertFalse(assets.isUnlocked(ShopTestFixtures.UNLOCK));
        assertFalse(Files.exists(save));
    }

    @Test
    void inventoryOverflowDoesNotChargeOrSave() {
        assets.restore(new PlayerAssetsState(1000, Collections.<String>emptySet(),
                Collections.singletonMap(ShopTestFixtures.CONSUMABLE, Integer.MAX_VALUE)));

        assertEquals(PurchaseResult.INTEGER_OVERFLOW_DETECTED, shop.purchase(ShopTestFixtures.CONSUMABLE));
        assertEquals(1000, assets.getBalance());
        assertEquals(Integer.MAX_VALUE, assets.getItemQuantity(ShopTestFixtures.CONSUMABLE));
        assertFalse(Files.exists(save));
    }

    @Test
    void savedPurchasesCanBeRestoredToSharedAssets() throws Exception {
        shop.purchase(ShopTestFixtures.CONSUMABLE);
        shop.purchase(ShopTestFixtures.UNLOCK);
        PlayerAssets restored = new PlayerAssets();
        assertTrue(restored.restore(saveFile.load()));

        ShopController restarted = ShopTestFixtures.controller(restored, saveFile);

        assertEquals(400, restarted.snapshot().getBalance());
        assertTrue(restored.isUnlocked(ShopTestFixtures.UNLOCK));
        assertEquals(1, restored.getItemQuantity(ShopTestFixtures.CONSUMABLE));
        restarted.purchase(ShopTestFixtures.CONSUMABLE);
        assertEquals(300, restored.getBalance());
        assertEquals(2, restored.getItemQuantity(ShopTestFixtures.CONSUMABLE));
    }

    @Test
    void failedSaveKeepsPurchaseAndRetrySavesLatestAssets() throws Exception {
        saveFile.save(assets.snapshot());
        byte[] previousSave = Files.readAllBytes(save);
        Path blocker = temp.resolve("not-a-directory");
        Files.write(blocker, new byte[] { 1 });
        saveFile.setPath(blocker.resolve("assets.properties"));

        assertEquals(PurchaseResult.SUCCESS, shop.purchase(ShopTestFixtures.CONSUMABLE));
        assertEquals(900, assets.getBalance());
        assertEquals(1, assets.getItemQuantity(ShopTestFixtures.CONSUMABLE));
        assertEquals(ShopController.SaveStatus.FAILED, shop.getSaveStatus());
        assertFalse(shop.retrySave());
        assertEquals(PurchaseResult.CATEGORY_MISMATCH, shop.purchase("UNKNOWN"));
        assertEquals(ShopController.SaveStatus.FAILED, shop.getSaveStatus());
        assertArrayEquals(previousSave, Files.readAllBytes(save));

        RewardSystem rewards = new RewardSystem(assets);
        rewards.rewardEnemy("BASIC");
        assertTrue(rewards.settle());
        int latestBalance = assets.getBalance();
        assertTrue(latestBalance > 900);
        saveFile.setPath(save);

        assertTrue(shop.retrySave());
        assertEquals(ShopController.SaveStatus.SAVED, shop.getSaveStatus());
        assertEquals(latestBalance, saveFile.load().getBalance());
        assertEquals(1, saveFile.load().getItemQuantities().get(ShopTestFixtures.CONSUMABLE));
        byte[] recovered = Files.readAllBytes(save);
        assertTrue(shop.retrySave());
        assertArrayEquals(recovered, Files.readAllBytes(save));
        assertEquals(latestBalance, assets.getBalance());
        assertEquals(1, assets.getItemQuantity(ShopTestFixtures.CONSUMABLE));
    }

    @Test
    void laterPurchaseAlsoSavesEarlierUnsavedPurchase() throws Exception {
        Path blocker = temp.resolve("blocker");
        Files.write(blocker, new byte[] { 1 });
        saveFile.setPath(blocker.resolve("save"));
        shop.purchase(ShopTestFixtures.CONSUMABLE);
        saveFile.setPath(save);

        shop.purchase(ShopTestFixtures.UNLOCK);

        assertEquals(ShopController.SaveStatus.SAVED, shop.getSaveStatus());
        PlayerAssetsState saved = saveFile.load();
        assertEquals(400, saved.getBalance());
        assertTrue(saved.getUnlockedContentIds().contains(ShopTestFixtures.UNLOCK));
        assertEquals(1, saved.getItemQuantities().get(ShopTestFixtures.CONSUMABLE));
    }

    @Test
    void productListAndSnapshotsAreReadOnlyCopies() {
        List<ShopProduct> input = new ArrayList<ShopProduct>(ShopTestFixtures.products());
        ShopController configured = new ShopController(assets, files, input, saveFile);
        input.clear();

        assertEquals(2, configured.getProducts().size());
        assertEquals(100, configured.getProducts().get(0).getPrice());
        assertEquals(ShopTestFixtures.products().get(0).getDescription(),
                configured.getProducts().get(0).getDescription());
        assertThrows(UnsupportedOperationException.class, () -> configured.getProducts().clear());
        PlayerAssetsState before = configured.snapshot();
        configured.purchase(ShopTestFixtures.CONSUMABLE);
        assertEquals(1000, before.getBalance());
        assertTrue(before.getItemQuantities().isEmpty());
    }

    @Test
    void invalidDependenciesAndProductDefinitionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ShopController(null, files));
        assertThrows(IllegalArgumentException.class, () -> new ShopController(assets, null));
        assertThrows(IllegalArgumentException.class, () -> new ShopController(assets, files, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ShopController(assets, files, Collections.singletonList(null)));
        assertThrows(IllegalArgumentException.class, () -> new ShopProduct(" ", "X", Category.INVENTORY, 1));
        assertThrows(IllegalArgumentException.class, () -> new ShopProduct("x", "", Category.INVENTORY, 1));
        assertThrows(IllegalArgumentException.class, () -> new ShopProduct("x", "X", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new ShopProduct("x", "X", Category.INVENTORY, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ShopProduct("x", "X", Category.INVENTORY, 1, " "));
        List<ShopProduct> duplicate = new ArrayList<ShopProduct>(ShopTestFixtures.products());
        duplicate.add(new ShopProduct(ShopTestFixtures.CONSUMABLE, "Duplicate", Category.UNLOCKABLE, 10));

        assertThrows(IllegalArgumentException.class, () -> new ShopController(assets, files, duplicate));
        assertFalse(Files.exists(save));
    }

    @Test
    void controllersKeepTheirSavePathsIndependent() throws Exception {
        ShopTestFixtures.SaveFile otherSave = new ShopTestFixtures.SaveFile(files,
                temp.resolve("other-assets.properties"));
        ShopController other = ShopTestFixtures.controller(ShopTestFixtures.fundedAssets(), otherSave);

        shop.purchase(ShopTestFixtures.CONSUMABLE);
        other.purchase(ShopTestFixtures.UNLOCK);

        assertEquals(900, saveFile.load().getBalance());
        assertEquals(1, saveFile.load().getItemQuantities().get(ShopTestFixtures.CONSUMABLE));
        assertTrue(saveFile.load().getUnlockedContentIds().isEmpty());
        assertEquals(500, otherSave.load().getBalance());
        assertTrue(otherSave.load().getItemQuantities().isEmpty());
        assertTrue(otherSave.load().getUnlockedContentIds().contains(ShopTestFixtures.UNLOCK));
    }

    @Test
    void retryWithoutFailedSaveDoesNotWrite() {
        assertTrue(shop.retrySave());
        assertEquals(ShopController.SaveStatus.NOT_NEEDED, shop.getSaveStatus());
        assertFalse(Files.exists(save));
    }
}
