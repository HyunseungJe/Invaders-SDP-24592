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

    public RewardSettlementCoordinator(RewardSystem rewardSystem, PlayerAssets playerAssets, FileManager fileManager) {
        this(rewardSystem, playerAssets, fileManager, null);
    }

    /** Constructor with package-private visibility for isolated testing. */
    RewardSettlementCoordinator(RewardSystem rewardSystem, PlayerAssets playerAssets, FileManager fileManager, Path customSavePath) {
        if (rewardSystem == null || playerAssets == null || fileManager == null) {
            throw new IllegalArgumentException("Dependencies cannot be null.");
        }
        this.rewardSystem = rewardSystem;
        this.playerAssets = playerAssets;
        this.fileManager = fileManager;
        this.customSavePath = customSavePath;
    }

    /**
     * Settles accumulated rewards and saves the updated asset state to disk.
     *
     * Rejects settlement if the asset balance overflows, preserving the pending rewards.
     * If saving fails after a successful settlement, the rewards remain in the balance
     * to prevent duplication on retry. Subsequent calls with no new rewards will
     * bypass settlement and directly retry saving the latest snapshot.
     *
     * @return SUCCESS if rewards were settled and saved; SETTLEMENT_FAILED if the transfer to assets was rejected; SAVE_FAILED if disk persistence failed
     */
    public SettlementResult processSettlement() {
        if (!rewardSystem.settle()) {
            return SettlementResult.SETTLEMENT_FAILED;
        }

        PlayerAssetsState snapshot = playerAssets.snapshot();

        try {
            if (customSavePath != null) {
                fileManager.savePlayerAssets(snapshot, customSavePath);
            } else {
                fileManager.savePlayerAssets(snapshot);
            }
            return SettlementResult.SUCCESS;
        } catch (IOException e) {
            return SettlementResult.SAVE_FAILED;
        }
    }
}