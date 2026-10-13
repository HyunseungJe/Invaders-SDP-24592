package engine;

import currency.PlayerAssetsState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FileManagerTest {

    private FileManager fileManager;

    @BeforeEach
    void setUp() {
        fileManager = FileManager.getInstance();
    }

    @Test
    @DisplayName("Saving and loading PlayerAssetsState should accurately persist data")
    void testSaveAndLoadPlayerAssets() throws IOException {
        Set<String> unlocks = Set.of("SHIP_SKIN_RED", "STAGE_02");
        Map<String, Integer> items = Map.of("SHIELD", 3, "BOMB", 5);
        PlayerAssetsState originalState = new PlayerAssetsState(2500, unlocks, items);

        fileManager.savePlayerAssets(originalState);
        PlayerAssetsState loadedState = fileManager.loadPlayerAssets();

        assertNotNull(loadedState);
        assertEquals(2500, loadedState.getBalance());
        assertEquals(2, loadedState.getUnlockedContentIds().size());
        assertTrue(loadedState.getUnlockedContentIds().contains("SHIP_SKIN_RED"));
        assertTrue(loadedState.getUnlockedContentIds().contains("STAGE_02"));
        assertEquals(3, loadedState.getItemQuantities().get("SHIELD"));
        assertEquals(5, loadedState.getItemQuantities().get("BOMB"));
    }

    @Test
    @DisplayName("Saving null state should throw IllegalArgumentException")
    void testSaveNullStateThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> fileManager.savePlayerAssets(null));
    }
}
