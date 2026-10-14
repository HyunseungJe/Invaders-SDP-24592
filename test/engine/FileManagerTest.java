package engine;

import currency.PlayerAssetsState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FileManagerTest {

    @TempDir
    Path tempDir;

    private Path testFilePath;
    private FileManager fileManager;

    @BeforeEach
    void setUp() {
        testFilePath = tempDir.resolve("test_player_assets.properties");
        fileManager = FileManager.getInstance();
        fileManager.setAssetsSavePath(testFilePath);
    }

    @AfterEach
    void tearDown() {
        fileManager.resetAssetsSavePath();
    }

    @Test
    @DisplayName("Loading non-existent save file returns default initial state")
    void testLoadNonExistentFileReturnsDefaultState() throws IOException {
        Path missingPath = tempDir.resolve("non_existent.properties");
        PlayerAssetsState state = fileManager.loadPlayerAssets(missingPath);

        assertNotNull(state);
        assertEquals(0, state.getBalance());
        assertTrue(state.getUnlockedContentIds().isEmpty());
        assertTrue(state.getItemQuantities().isEmpty());
    }

    @Test
    @DisplayName("Exact ID round-trip preserves special characters (colons, commas, semicolons, whitespace)")
    void testSaveAndLoadExactIdRoundTrip() throws IOException {
        Set<String> unlocks = Set.of("ship:laser,v2", "stage;bonus", "  spaced_unlock  ");
        Map<String, Integer> items = Map.of(
                "weapon:beam:plasma", 2,
                "potion,heal;large", 5,
                "  spaced_item  ", 1
        );
        PlayerAssetsState original = new PlayerAssetsState(3500, unlocks, items);

        fileManager.savePlayerAssets(original, testFilePath);
        PlayerAssetsState loaded = fileManager.loadPlayerAssets(testFilePath);

        assertNotNull(loaded);
        assertEquals(3500, loaded.getBalance());
        assertEquals(unlocks, loaded.getUnlockedContentIds());
        assertEquals(items, loaded.getItemQuantities());
    }

    @Test
    @DisplayName("Loading an existing empty or truncated file throws IOException")
    void testLoadEmptyFileThrowsIOException() throws IOException {
        Files.createFile(testFilePath);
        assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(testFilePath));
    }

    @Test
    @DisplayName("Loading save missing required fields throws IOException")
    void testMissingRequiredFieldsThrowsIOException() throws IOException {
        // Missing version
        Files.writeString(testFilePath, "balance=100\nunlockedContentIds=\nitemQuantities=\n");
        assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(testFilePath));

        // Missing balance
        Files.writeString(testFilePath, "version=1\nunlockedContentIds=\nitemQuantities=\n");
        assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(testFilePath));

        // Missing unlockedContentIds
        Files.writeString(testFilePath, "version=1\nbalance=100\nitemQuantities=\n");
        assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(testFilePath));

        // Missing itemQuantities
        Files.writeString(testFilePath, "version=1\nbalance=100\nunlockedContentIds=\n");
        assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(testFilePath));
    }

    @Test
    @DisplayName("Loading unsupported format version throws IOException")
    void testUnsupportedVersionThrowsIOException() throws IOException {
        Files.writeString(testFilePath, "version=2\nbalance=100\nunlockedContentIds=\nitemQuantities=\n");
        assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(testFilePath));
    }

    @Test
    @DisplayName("Loading invalid numeric values throws IOException")
    void testInvalidNumericValuesThrowsIOException() throws IOException {
        // Negative balance
        Files.writeString(testFilePath, "version=1\nbalance=-10\nunlockedContentIds=\nitemQuantities=\n");
        assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(testFilePath));

        // Non-integer balance
        Files.writeString(testFilePath, "version=1\nbalance=abc\nunlockedContentIds=\nitemQuantities=\n");
        assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(testFilePath));

        // Zero item quantity
        Files.writeString(testFilePath, "version=1\nbalance=100\nunlockedContentIds=\nitemQuantities=BOMB:0\n");
        assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(testFilePath));

        // Noninteger item quantity
        Files.writeString(testFilePath, "version=1\nbalance=100\nunlockedContentIds=\nitemQuantities=BOMB:five\n");
        assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(testFilePath));
    }

    @Test
    @DisplayName("Loading malformed inventory entries throws IOException")
    void testMalformedInventoryEntriesThrowsIOException() throws IOException {
        // Missing colon delimiter
        Files.writeString(testFilePath, "version=1\nbalance=100\nunlockedContentIds=\nitemQuantities=BOMB\n");
        assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(testFilePath));

        // Empty token / double delimiter
        Files.writeString(testFilePath, "version=1\nbalance=100\nunlockedContentIds=SKIN1,,SKIN2\nitemQuantities=\n");
        assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(testFilePath));

        // Blank item ID
        Files.writeString(testFilePath, "version=1\nbalance=100\nunlockedContentIds=\nitemQuantities=%20%20:2\n");
        assertThrows(IOException.class, () -> fileManager.loadPlayerAssets(testFilePath));
    }

    @Test
    @DisplayName("Failed save leaves previous valid save file intact")
    void testFailedSavePreservesPreviousValidFile() throws IOException {
        PlayerAssetsState validState = new PlayerAssetsState(1500, Set.of("SHIP_A"), Map.of("ITEM_X", 3));
        fileManager.savePlayerAssets(validState, testFilePath);

        PlayerAssetsState before = fileManager.loadPlayerAssets(testFilePath);
        assertEquals(1500, before.getBalance());

        assertThrows(IllegalArgumentException.class, () -> fileManager.savePlayerAssets(null, testFilePath));

        PlayerAssetsState after = fileManager.loadPlayerAssets(testFilePath);
        assertEquals(1500, after.getBalance());
        assertEquals(Set.of("SHIP_A"), after.getUnlockedContentIds());
        assertEquals(Map.of("ITEM_X", 3), after.getItemQuantities());
    }

    @Test
    @DisplayName("Configured path isolation ensures production path is untouched")
    void testPathConfigurationIsolation() throws IOException {
        Path productionPath = Path.of(FileManager.DEFAULT_ASSETS_SAVE_PATH);
        boolean existedBefore = Files.exists(productionPath);

        PlayerAssetsState state = new PlayerAssetsState(500, Set.of("TEST_UNLOCK"), Map.of("TEST_ITEM", 1));
        fileManager.savePlayerAssets(state);

        assertTrue(Files.exists(testFilePath));
        assertEquals(existedBefore, Files.exists(productionPath));
    }
}
