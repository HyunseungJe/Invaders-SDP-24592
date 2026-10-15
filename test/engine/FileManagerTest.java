package engine;

import currency.PlayerAssetsState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FileManagerTest {

    @TempDir
    Path tempDir;

    private FileManager fileManager;
    private Path savePath;

    @BeforeEach
    void setUp() {
        fileManager = FileManager.getInstance();
        savePath = tempDir.resolve("player_assets.properties");
        fileManager.setAssetsSavePath(savePath);
    }

    @AfterEach
    void tearDown() {
        fileManager.resetAssetsSavePath();
    }

    @Test
    @DisplayName("Default player assets save path shares directory with scores file")
    void testDefaultAssetPathSharesDirectoryWithScores() {
        Path defaultAssetPath = FileManager.resolveStoragePath(FileManager.ASSETS_FILENAME);
        Path scoresPath = FileManager.resolveStoragePath("scores");

        assertEquals(scoresPath.getParent(), defaultAssetPath.getParent());
        assertEquals("player_assets.properties", defaultAssetPath.getFileName().toString());

        fileManager.resetAssetsSavePath();
        assertEquals(defaultAssetPath, fileManager.getAssetsSavePath());
    }

    @Test
    @DisplayName("loadPlayerAssets throws IOException when unlocked content ID has malformed URL encoding")
    void testLoadMalformedEncodedUnlockIdThrowsIOException() throws IOException {
        String malformedData = "version=1\nbalance=100\nunlockedContentIds=VALID_SHIP,%ZZ_INVALID\nitemQuantities=ITEM_A:1\n";
        Files.writeString(savePath, malformedData);

        IOException ex = assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(savePath));
        assertTrue(ex.getMessage().contains("Malformed URL-encoded unlocked content ID"));
    }

    @Test
    @DisplayName("loadPlayerAssets throws IOException when item ID has malformed URL encoding")
    void testLoadMalformedEncodedInventoryIdThrowsIOException() throws IOException {
        String malformedData = "version=1\nbalance=100\nunlockedContentIds=VALID_SHIP\nitemQuantities=%ZZ_BAD_ITEM:3\n";
        Files.writeString(savePath, malformedData);

        IOException ex = assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(savePath));
        assertTrue(ex.getMessage().contains("Malformed URL-encoded item ID"));
    }

    @Test
    @DisplayName("savePlayerAssets write failure preserves preexisting valid save file")
    void testFailedSavePreservesPreviousValidFile() throws IOException {
        Path isolatedDir = tempDir.resolve("restricted_dir");
        Files.createDirectories(isolatedDir);
        Path targetFile = isolatedDir.resolve("player_assets.properties");

        PlayerAssetsState initialValidState = new PlayerAssetsState(
                350,
                Set.of("SHIP_ORIGINAL"),
                Map.of("SHIELD", 2)
        );
        fileManager.savePlayerAssets(initialValidState, targetFile);
        assertTrue(Files.exists(targetFile));
        String originalContent = Files.readString(targetFile);

        PlayerAssetsState nextState = new PlayerAssetsState(
                9999,
                Set.of("SHIP_NEW"),
                Map.of("SHIELD", 100)
        );

        File dirFile = isolatedDir.toFile();
        boolean posixSupported = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

        try {
            if (posixSupported) {
                Files.setPosixFilePermissions(isolatedDir, PosixFilePermissions.fromString("r-xr-xr-x"));
            } else {
                dirFile.setWritable(false);
            }

            assertThrows(IOException.class, () -> fileManager.savePlayerAssets(nextState, targetFile));

            assertEquals(originalContent, Files.readString(targetFile));
            PlayerAssetsState reloaded = fileManager.loadPlayerAssets(targetFile);
            assertEquals(350, reloaded.getBalance());
            assertTrue(reloaded.getUnlockedContentIds().contains("SHIP_ORIGINAL"));
            assertEquals(2, reloaded.getItemQuantities().get("SHIELD"));
        } finally {
            if (posixSupported) {
                Files.setPosixFilePermissions(isolatedDir, PosixFilePermissions.fromString("rwxr-xr-x"));
            } else {
                dirFile.setWritable(true);
            }
        }
    }

    @Test
    @DisplayName("savePlayerAssets rejects null state before I/O begins")
    void testSaveNullStateThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> fileManager.savePlayerAssets(null, savePath));
    }

    @Test
    @DisplayName("Round-trip serialization preserves characters requiring encoding")
    void testRoundTripWithSpecialCharacters() throws IOException {
        Set<String> unlocks = Set.of("STAGE:01,BONUS", "SHIP/SPACE BLUE");
        Map<String, Integer> items = Map.of("POTION:HEALTH,SUPER", 5, "GEM%BLUE", 10);
        PlayerAssetsState state = new PlayerAssetsState(1200, unlocks, items);

        fileManager.savePlayerAssets(state, savePath);
        PlayerAssetsState loaded = fileManager.loadPlayerAssets(savePath);

        assertEquals(1200, loaded.getBalance());
        assertEquals(unlocks, loaded.getUnlockedContentIds());
        assertEquals(items, loaded.getItemQuantities());
    }
}

