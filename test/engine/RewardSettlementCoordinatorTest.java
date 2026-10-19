package engine;

import currency.PlayerAssets;
import currency.RewardSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RewardSettlementCoordinatorTest {

    private PlayerAssets assets;
    private RewardSystem rewards;
    private FileManager fileManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        assets = new PlayerAssets();
        rewards = new RewardSystem(assets);
        fileManager = FileManager.getInstance();
    }

    @Test
    void testSuccessfulSettlementAndSave() {
        Path savePath = tempDir.resolve("test_assets.properties");
        RewardSettlementCoordinator coordinator = new RewardSettlementCoordinator(rewards, assets, fileManager, savePath);

        assertTrue(rewards.rewardEnemy("TANK")); // Adds 50 pending

        SettlementResult result = coordinator.processSettlement();

        assertEquals(SettlementResult.SUCCESS, result);
        assertEquals(0, rewards.getPendingCurrency());
        assertEquals(50, assets.getBalance());
        assertTrue(Files.exists(savePath));
    }

    @Test
    void testSettlementFailurePreservesPendingRewards() {
        Path savePath = tempDir.resolve("test_assets.properties");
        RewardSettlementCoordinator coordinator = new RewardSettlementCoordinator(rewards, assets, fileManager, savePath);

        // Force an overflow condition in PlayerAssets
        assets.restore(new currency.PlayerAssetsState(Integer.MAX_VALUE, java.util.Collections.<String>emptySet(), java.util.Collections.<String, Integer>emptyMap()));
        assertTrue(rewards.rewardEnemy("BASIC")); // 10 pending

        SettlementResult result = coordinator.processSettlement();

        assertEquals(SettlementResult.SETTLEMENT_FAILED, result);
        assertEquals(10, rewards.getPendingCurrency()); // Pending rewards preserved
        assertEquals(Integer.MAX_VALUE, assets.getBalance());
    }

    @Test
    void testSaveFailurePreservesSettledAssets() {
        // Passing the directory itself as the file path will force an IOException during save
        RewardSettlementCoordinator coordinator = new RewardSettlementCoordinator(rewards, assets, fileManager, tempDir);

        assertTrue(rewards.rewardEnemy("TANK"));

        SettlementResult result = coordinator.processSettlement();

        assertEquals(SettlementResult.SAVE_FAILED, result);
        assertEquals(0, rewards.getPendingCurrency()); // Settlement succeeded
        assertEquals(50, assets.getBalance()); // Assets are updated in runtime memory
    }

    @Test
    void testSaveRetryUsesLatestStateWithoutDuplicatingRewards() {
        // 1. Initial failure setup
        RewardSettlementCoordinator failingCoordinator = new RewardSettlementCoordinator(rewards, assets, fileManager, tempDir);
        assertTrue(rewards.rewardEnemy("FAST")); // 25 pending
        assertEquals(SettlementResult.SAVE_FAILED, failingCoordinator.processSettlement());
        assertEquals(25, assets.getBalance());
        assertEquals(0, rewards.getPendingCurrency());

        // 2. Add changes after the failed save
        rewards.rewardEnemy("BASIC"); // 10 pending

        // 3. Retry save with a valid path
        Path validPath = tempDir.resolve("retry_assets.properties");
        RewardSettlementCoordinator validCoordinator = new RewardSettlementCoordinator(rewards, assets, fileManager, validPath);

        SettlementResult retryResult = validCoordinator.processSettlement();

        assertEquals(SettlementResult.SUCCESS, retryResult);
        assertEquals(0, rewards.getPendingCurrency());
        assertEquals(35, assets.getBalance()); // 25 from failed save + 10 from retry
        assertTrue(Files.exists(validPath));
    }
}