package engine;

import currency.PlayerAssets;
import currency.PlayerAssetsState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import shop.ShopController;
import shop.ShopTestFixtures;
import static org.junit.jupiter.api.Assertions.*;

class ShopStartupTest {
    @TempDir
    Path temp;

    private FileManager files;
    private Path save;

    @BeforeEach
    void setUp() {
        files = Core.getFileManager();
        save = temp.resolve("assets.properties");
    }

    @Test
    void missingSaveCreatesEmptySessionWithoutWriting() throws Exception {
        PlayerAssets shared = new PlayerAssets();

        ShopController controller = Core.initializeShop(shared, files, files.loadPlayerAssets(save));

        assertEquals(0, shared.getBalance());
        assertEquals(0, controller.snapshot().getBalance());
        assertTrue(controller.snapshot().getUnlockedContentIds().isEmpty());
        assertTrue(controller.snapshot().getItemQuantities().isEmpty());
        assertTrue(controller.getProducts().isEmpty());
        assertEquals(ShopController.SaveStatus.NOT_NEEDED, controller.getSaveStatus());
        assertFalse(Files.exists(save));
    }

    @Test
    void validSaveLoadsIntoTheSameSharedAssetObject() throws Exception {
        PlayerAssetsState persisted = new PlayerAssetsState(700,
                Collections.singleton(ShopTestFixtures.UNLOCK),
                Collections.singletonMap(ShopTestFixtures.CONSUMABLE, 2));
        files.savePlayerAssets(persisted, save);
        byte[] saved = Files.readAllBytes(save);
        PlayerAssets shared = new PlayerAssets();

        ShopController controller = Core.initializeShop(shared, files, files.loadPlayerAssets(save));

        assertEquals(700, shared.getBalance());
        assertTrue(shared.isUnlocked(ShopTestFixtures.UNLOCK));
        assertEquals(2, shared.getItemQuantity(ShopTestFixtures.CONSUMABLE));
        assertEquals(persisted.getBalance(), controller.snapshot().getBalance());
        assertEquals(persisted.getUnlockedContentIds(), controller.snapshot().getUnlockedContentIds());
        assertEquals(persisted.getItemQuantities(), controller.snapshot().getItemQuantities());
        assertEquals(ShopController.SaveStatus.NOT_NEEDED, controller.getSaveStatus());

        shared.restore(new PlayerAssetsState(900, persisted.getUnlockedContentIds(),
                persisted.getItemQuantities()));
        assertEquals(900, controller.snapshot().getBalance());
        assertArrayEquals(saved, Files.readAllBytes(save));
    }

    @Test
    void corruptLoadPreservesMemoryAndFileAndCanBeRetried() throws Exception {
        PlayerAssets shared = new PlayerAssets();
        PlayerAssetsState before = new PlayerAssetsState(1000,
                Collections.singleton(ShopTestFixtures.UNLOCK),
                Collections.singletonMap(ShopTestFixtures.CONSUMABLE, 3));
        shared.restore(before);
        byte[] corrupt = "version=999\nbalance=0\n".getBytes(StandardCharsets.UTF_8);
        Files.write(save, corrupt);

        assertThrows(IOException.class, () -> Core.initializeShop(shared, files, files.loadPlayerAssets(save)));

        assertEquals(before.getBalance(), shared.getBalance());
        assertEquals(before.getUnlockedContentIds(), shared.snapshot().getUnlockedContentIds());
        assertEquals(before.getItemQuantities(), shared.snapshot().getItemQuantities());
        assertArrayEquals(corrupt, Files.readAllBytes(save));

        PlayerAssetsState recovered = new PlayerAssetsState(650,
                Collections.singleton(ShopTestFixtures.UNLOCK),
                Collections.singletonMap(ShopTestFixtures.CONSUMABLE, 1));
        files.savePlayerAssets(recovered, save);
        byte[] repairedSave = Files.readAllBytes(save);

        ShopController controller = Core.initializeShop(shared, files, files.loadPlayerAssets(save));

        assertEquals(650, shared.getBalance());
        assertTrue(shared.isUnlocked(ShopTestFixtures.UNLOCK));
        assertEquals(1, shared.getItemQuantity(ShopTestFixtures.CONSUMABLE));
        assertEquals(650, controller.snapshot().getBalance());
        assertArrayEquals(repairedSave, Files.readAllBytes(save));
    }
}
