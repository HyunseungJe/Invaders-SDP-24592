package currency;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles currency rewards for enemy defeats and level completion.
 *
 * Reward values can be configured independently from the
 * player's currency balance.
 *
 * Duplicate gameplay events are rejected to prevent
 * players from receiving the same reward multiple times.
 *
 * @author Joshua Hernandez Ruiz
 */
public class RewardSystem {

    private static final int DEFAULT_LEVEL_REWARD = 100;

    private final CurrencyBalance currencyBalance;

    private final Map<String, Integer> enemyRewards;

    private final Set<String> processedEvents;

    private int baseLevelReward;

    /**
     * Creates a reward system using an existing currency balance.
     *
     * @param currencyBalance player's currency balance
     */
    public RewardSystem(CurrencyBalance currencyBalance) {

        if (currencyBalance == null) {
            throw new IllegalArgumentException(
                "CurrencyBalance cannot be null."
            );
        }

        this.currencyBalance = currencyBalance;

        this.enemyRewards = new HashMap<>();

        this.processedEvents = new HashSet<>();

        this.baseLevelReward = DEFAULT_LEVEL_REWARD;

        initializeDefaultRewards();
    }

    /**
     * Initializes the default enemy reward values.
     *
     * These values can be adjusted during game balancing.
     */
    private void initializeDefaultRewards() {

        enemyRewards.put("BASIC", 10);

        enemyRewards.put("FAST", 25);

        enemyRewards.put("TANK", 50);
    }

    /**
     * Configures the reward for an enemy type.
     *
     * @param enemyType enemy category
     * @param amount reward amount
     * @return true if configuration succeeds
     */
    public boolean setEnemyReward(String enemyType, int amount) {

        if (enemyType == null || enemyType.isBlank()) {
            return false;
        }

        if (amount <= 0) {
            return false;
        }

        enemyRewards.put(enemyType, amount);

        return true;
    }

    /**
     * Returns the configured reward for an enemy type.
     *
     * @param enemyType enemy category
     * @return reward amount, or zero if unknown
     */
    public int getEnemyReward(String enemyType) {

        if (enemyType == null) {
            return 0;
        }

        return enemyRewards.getOrDefault(enemyType, 0);
    }

    /**
     * Rewards the player for defeating an enemy.
     *
     * Each event ID can only be processed once.
     *
     * @param eventId unique identifier for the defeat event
     * @param enemyType type of defeated enemy
     * @return true if the reward was successfully granted
     */
    public boolean rewardEnemy(String eventId, String enemyType) {

        if (!isValidEvent(eventId)) {
            return false;
        }

        int reward = getEnemyReward(enemyType);

        if (reward <= 0) {
            return false;
        }

        return grantReward(eventId, reward);
    }

    /**
     * Configures the base reward for completing levels.
     *
     * @param amount new base reward
     * @return true if configuration succeeds
     */
    public boolean setBaseLevelReward(int amount) {

        if (amount <= 0) {
            return false;
        }

        baseLevelReward = amount;

        return true;
    }

    /**
     * Returns the base level completion reward.
     *
     * @return base reward
     */
    public int getBaseLevelReward() {

        return baseLevelReward;
    }

    /**
     * Rewards the player for completing a level.
     *
     * Reward = base reward * difficulty.
     *
     * @param eventId unique identifier for the completion event
     * @param difficulty level difficulty multiplier
     * @return true if the reward was successfully granted
     */
    public boolean rewardLevel(String eventId, int difficulty) {

        if (!isValidEvent(eventId)) {
            return false;
        }

        if (difficulty <= 0) {
            return false;
        }

        long calculatedReward =
            (long) baseLevelReward * difficulty;

        if (calculatedReward > Integer.MAX_VALUE) {
            return false;
        }

        return grantReward(eventId, (int) calculatedReward);
    }

    /**
     * Validates an event identifier.
     *
     * @param eventId event identifier
     * @return true if valid and not already processed
     */
    private boolean isValidEvent(String eventId) {

        return eventId != null
            && !eventId.isBlank()
            && !processedEvents.contains(eventId);
    }

    /**
     * Adds the reward to the player's currency balance.
     *
     * The event is marked as processed only after the
     * currency operation succeeds.
     *
     * @param eventId unique event identifier
     * @param amount reward amount
     * @return true if successfully granted
     */
    private boolean grantReward(String eventId, int amount) {

        if (!isValidEvent(eventId)) {
            return false;
        }

        boolean success = currencyBalance.add(amount);

        if (!success) {
            return false;
        }

        processedEvents.add(eventId);

        return true;
    }

    /**
     * Checks whether an event has already been rewarded.
     *
     * @param eventId unique event identifier
     * @return true if already processed
     */
    public boolean isEventProcessed(String eventId) {

        return processedEvents.contains(eventId);
    }
}