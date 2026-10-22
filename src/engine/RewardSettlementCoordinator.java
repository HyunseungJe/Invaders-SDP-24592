package engine;

import currency.PlayerAssets;
import currency.PlayerAssetsState;
import currency.RewardSystem;

import java.io.IOException;
import java.nio.file.Path;

public class RewardSettlementCoordinator {

    private final RewardSystem rewardSystem;
    private final PlayerAssets playerAssets;
    private final FileManager fileManager;
    private final Path customSavePath;
    private final AssetSaveState saveState;

    public RewardSettlementCoordinator(RewardSystem rewardSystem, PlayerAssets playerAssets, FileManager fileManager) {
        this(rewardSystem, playerAssets, fileManager, null, new AssetSaveState());
    }

    /** Uses the same save status as purchases and consumable uses. */
    public RewardSettlementCoordinator(RewardSystem rewardSystem, PlayerAssets playerAssets,
            FileManager fileManager, AssetSaveState saveState) {
        this(rewardSystem, playerAssets, fileManager, null, saveState);
    }

    /** Constructor with package-private visibility for isolated testing. */
    RewardSettlementCoordinator(RewardSystem rewardSystem, PlayerAssets playerAssets, FileManager fileManager, Path customSavePath) {
        this(rewardSystem, playerAssets, fileManager, customSavePath, new AssetSaveState());
    }

    RewardSettlementCoordinator(RewardSystem rewardSystem, PlayerAssets playerAssets,
            FileManager fileManager, Path customSavePath, AssetSaveState saveState) {
        if (rewardSystem == null || playerAssets == null || fileManager == null || saveState == null) {
            throw new IllegalArgumentException("Dependencies cannot be null.");
        }
        this.rewardSystem = rewardSystem;
        this.playerAssets = playerAssets;
        this.fileManager = fileManager;
        this.customSavePath = customSavePath;
        this.saveState = saveState;
    }

    /**
     * Settles accumulated rewards and saves the updated asset state to disk.
     *
     * Rejects settlement if the asset balance overflows, preserving the pending rewards.
     * If saving fails after a successful settlement, the rewards remain in the balance.
     * Use retrySave() to retry persistence without settling newly accumulated rewards.
     * Calling this method again requests a new settlement before saving.
     *
     * @return SUCCESS if rewards were settled and saved; SETTLEMENT_FAILED if the transfer to assets was rejected; SAVE_FAILED if disk persistence failed
     */
    public SettlementResult processSettlement() {
        if (!rewardSystem.settle()) {
            return SettlementResult.SETTLEMENT_FAILED;
        }

        return saveLatestAssets();
    }

    /**
     * Saves the latest shared assets without transferring or clearing pending rewards.
     * Captures a fresh snapshot on every call, including changes made after a failed save.
     * A failed retry leaves both runtime assets and pending rewards unchanged.
     *
     * @return SUCCESS if the latest snapshot was saved; SAVE_FAILED if disk persistence failed
     */
    public SettlementResult retrySave() {
        return saveLatestAssets();
    }

    private SettlementResult saveLatestAssets() {
        PlayerAssetsState snapshot = playerAssets.snapshot();

        try {
            if (customSavePath != null) {
                fileManager.savePlayerAssets(snapshot, customSavePath);
            } else {
                fileManager.savePlayerAssets(snapshot);
            }
            saveState.recordSuccess();
            return SettlementResult.SUCCESS;
        } catch (IOException | SecurityException e) {
            saveState.recordFailure();
            return SettlementResult.SAVE_FAILED;
        }
    }
}
