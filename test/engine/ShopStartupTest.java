package engine;

import currency.PlayerAssets;
import currency.PlayerAssetsState;
import currency.PurchaseResult;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
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
    private boolean isolatedProcess;

    @Test
    void missingSaveCreatesEmptySessionWithoutWriting() throws Exception {
        if (!isolatedProcess) {
            runIsolated("missing");
            return;
        }
        PlayerAssets shared = new PlayerAssets();

        ShopController controller = Core.initializeShop(shared, files);

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
        if (!isolatedProcess) {
            runIsolated("saved");
            return;
        }
        PlayerAssetsState persisted = new PlayerAssetsState(700,
                Collections.singleton(ShopTestFixtures.UNLOCK),
                Collections.singletonMap(ShopTestFixtures.CONSUMABLE, 2));
        files.savePlayerAssets(persisted, save);
        byte[] saved = Files.readAllBytes(save);
        PlayerAssets shared = new PlayerAssets();

        ShopController controller = Core.initializeShop(shared, files);

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
        if (!isolatedProcess) {
            runIsolated("corrupt");
            return;
        }
        PlayerAssets shared = new PlayerAssets();
        PlayerAssetsState before = new PlayerAssetsState(1000,
                Collections.singleton(ShopTestFixtures.UNLOCK),
                Collections.singletonMap(ShopTestFixtures.CONSUMABLE, 3));
        shared.restore(before);
        byte[] corrupt = "version=999\nbalance=0\n".getBytes(StandardCharsets.UTF_8);
        Files.write(save, corrupt);

        assertThrows(IOException.class, () -> Core.initializeShop(shared, files));

        assertEquals(before.getBalance(), shared.getBalance());
        assertEquals(before.getUnlockedContentIds(), shared.snapshot().getUnlockedContentIds());
        assertEquals(before.getItemQuantities(), shared.snapshot().getItemQuantities());
        assertArrayEquals(corrupt, Files.readAllBytes(save));

        PlayerAssetsState recovered = new PlayerAssetsState(650,
                Collections.singleton(ShopTestFixtures.UNLOCK),
                Collections.singletonMap(ShopTestFixtures.CONSUMABLE, 1));
        files.savePlayerAssets(recovered, save);
        byte[] repairedSave = Files.readAllBytes(save);

        ShopController controller = Core.initializeShop(shared, files);

        assertEquals(650, shared.getBalance());
        assertTrue(shared.isUnlocked(ShopTestFixtures.UNLOCK));
        assertEquals(1, shared.getItemQuantity(ShopTestFixtures.CONSUMABLE));
        assertEquals(650, controller.snapshot().getBalance());
        assertArrayEquals(repairedSave, Files.readAllBytes(save));
    }

    @Test
    void purchasesUseDefaultSaveAndRestoreOnStartup() throws Exception {
        if (!isolatedProcess) {
            runIsolated("purchase");
            return;
        }
        PlayerAssets shared = ShopTestFixtures.fundedAssets();
        ShopController controller = ShopTestFixtures.controllerUsingDefaultSave(shared, files);

        assertEquals(PurchaseResult.SUCCESS, controller.purchase(ShopTestFixtures.CONSUMABLE));
        assertEquals(900, files.loadPlayerAssets().getBalance());
        assertEquals(1, files.loadPlayerAssets().getItemQuantities().get(ShopTestFixtures.CONSUMABLE));
        assertEquals(ShopController.SaveStatus.SAVED, controller.getSaveStatus());

        assertEquals(PurchaseResult.SUCCESS, controller.purchase(ShopTestFixtures.UNLOCK));
        assertEquals(400, files.loadPlayerAssets().getBalance());
        assertTrue(files.loadPlayerAssets().getUnlockedContentIds().contains(ShopTestFixtures.UNLOCK));

        PlayerAssets restored = new PlayerAssets();
        ShopController reopened = Core.initializeShop(restored, files);
        assertEquals(400, reopened.snapshot().getBalance());
        assertEquals(1, restored.getItemQuantity(ShopTestFixtures.CONSUMABLE));
        assertTrue(restored.isUnlocked(ShopTestFixtures.UNLOCK));
    }

    private void runIsolated(String scenario) throws Exception {
        // Default saves sit beside the compiled classes, so a new working directory alone is not enough.
        Path source = codeLocation(FileManager.class);
        Path classes = temp.resolve(Files.isDirectory(source) ? "classes" : "classes.jar");
        if (Files.isDirectory(source)) {
            try (Stream<Path> paths = Files.walk(source)) {
                for (Path file : (Iterable<Path>) paths::iterator) {
                    if (file.toString().endsWith(".class") && Files.isRegularFile(file)) {
                        Path target = classes.resolve(source.relativize(file));
                        Files.createDirectories(target.getParent());
                        Files.copy(file, target);
                    }
                }
            }
        } else {
            Files.copy(source, classes);
        }
        String classpath = classes + File.pathSeparator + codeLocation(ShopStartupTest.class);
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            classpath += File.pathSeparator + Paths.get(entry).toAbsolutePath();
        }
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Paths.get(System.getProperty("java.home"), "bin", executable);
        Path log = temp.resolve("process.log");
        Process process = new ProcessBuilder(java.toString(), "-Djava.awt.headless=true",
                "-Dfile.encoding=UTF-8", "-cp", classpath, ShopStartupTest.class.getName(),
                temp.toString(), scenario)
                .directory(temp.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Isolated storage test timed out.");
            assertEquals(0, process.exitValue(), Files.readString(log));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    private static Path codeLocation(Class<?> type) throws Exception {
        return Paths.get(type.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    public static void main(String[] args) throws Exception {
        Path expected = Paths.get(args[0]).toAbsolutePath().normalize().resolve(FileManager.ASSETS_FILENAME);
        Path actual = FileManager.resolveStoragePath(FileManager.ASSETS_FILENAME).toAbsolutePath().normalize();
        assertEquals(expected, actual, "Refusing to use a save outside the isolated test directory.");

        ShopStartupTest test = new ShopStartupTest();
        test.isolatedProcess = true;
        test.files = Core.getFileManager();
        test.save = actual;
        switch (args[1]) {
        case "missing":
            test.missingSaveCreatesEmptySessionWithoutWriting();
            break;
        case "saved":
            test.validSaveLoadsIntoTheSameSharedAssetObject();
            break;
        case "corrupt":
            test.corruptLoadPreservesMemoryAndFileAndCanBeRetried();
            break;
        case "purchase":
            test.purchasesUseDefaultSaveAndRestoreOnStartup();
            break;
        default:
            throw new IllegalArgumentException("Unknown storage test: " + args[1]);
        }
    }
}
