package currency;

import engine.GameplayConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles currency rewards for enemy defeats and level completion.
 *
 * Rewards accumulate until settle() transfers them to PlayerAssets.
 * Gameplay event deduplication is handled by the caller.
 *
 * @author Joshua Hernandez Ruiz
 */
public class RewardSystem {

    private static final int DEFAULT_LEVEL_REWARD = 100;

    private final PlayerAssets playerAssets;
    private final Map<String, Integer> enemyRewards;

    private int baseLevelReward;
    private int pendingCurrency;

    public RewardSystem(PlayerAssets playerAssets) {
        if (playerAssets == null) {
            throw new IllegalArgumentException(
                "PlayerAssets cannot be null."
            );
        }

        this.playerAssets = playerAssets;
        this.enemyRewards = new HashMap<>();
        this.baseLevelReward = DEFAULT_LEVEL_REWARD;
        this.pendingCurrency = 0;

        initializeDefaultRewards();
    }

    private void initializeDefaultRewards() {
        enemyRewards.put("BASIC", 10);
        enemyRewards.put("FAST", 25);
        enemyRewards.put("TANK", 50);
    }

    public boolean setEnemyReward(String enemyType, int amount) {
        if (enemyType == null || enemyType.trim().isEmpty() || amount <= 0) {
            return false;
        }

        enemyRewards.put(enemyType, amount);
        return true;
    }

    public int getEnemyReward(String enemyType) {
        if (enemyType == null) {
            return 0;
        }

        Integer reward = enemyRewards.get(enemyType);
        return reward != null ? reward : 0;
    }

    public boolean rewardEnemy(String enemyType) {

        int reward = getEnemyReward(enemyType);

        if (reward <= 0) {
            return false;
        }

        return accumulateReward(reward);
    }

    public boolean setBaseLevelReward(int amount) {
        if (amount <= 0) {
            return false;
        }

        baseLevelReward = amount;
        return true;
    }

    public int getBaseLevelReward() {
        return baseLevelReward;
    }

    public boolean rewardLevel(int difficulty) {
        final long calculatedReward;

        switch (difficulty) {
            case GameplayConfig.EASY:
                calculatedReward = Math.max(1L, baseLevelReward / 2L);
                break;
            case GameplayConfig.NORMAL:
                calculatedReward = baseLevelReward;
                break;
            case GameplayConfig.HARD:
                calculatedReward = (long) baseLevelReward * 2;
                break;
            default:
                return false;
        }

        if (calculatedReward > Integer.MAX_VALUE) {
            return false;
        }

        return accumulateReward((int) calculatedReward);
    }


    /**
     * Accumulates a reward without changing the player's balance.
     */
    private boolean accumulateReward(int amount) {
        if (amount <= 0) {
            return false;
        }

        long newPending = (long) pendingCurrency + amount;

        if (newPending > Integer.MAX_VALUE) {
            return false;
        }

        pendingCurrency = (int) newPending;

        return true;
    }

    /**
     * Returns accumulated rewards not yet transferred.
     */
    public int getPendingCurrency() {
        return pendingCurrency;
    }

    /**
     * Transfers pending rewards to PlayerAssets.
     *
     * If the transfer fails, pending rewards remain unchanged.
     */
    public boolean settle() {
        if (pendingCurrency == 0) {
            return true;
        }

        if (!playerAssets.addCurrency(pendingCurrency)) {
            return false;
        }

        pendingCurrency = 0;
        return true;
    }

}
