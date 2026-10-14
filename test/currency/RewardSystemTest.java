package currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RewardSystemTest {

    private CurrencyBalance balance;
    private RewardSystem rewards;

    @BeforeEach
    void setUp() {
        balance = new CurrencyBalance();
        rewards = new RewardSystem(balance);
    }

    @Test
    void testInitialBalance() {
        assertEquals(0, balance.getBalance());
    }

    @Test
    void testBasicEnemyReward() {
        assertTrue(rewards.rewardEnemy("enemy-1", "BASIC"));
        assertEquals(10, balance.getBalance());
    }

    @Test
    void testFastEnemyReward() {
        assertTrue(rewards.rewardEnemy("enemy-2", "FAST"));
        assertEquals(25, balance.getBalance());
    }

    @Test
    void testTankEnemyReward() {
        assertTrue(rewards.rewardEnemy("enemy-3", "TANK"));
        assertEquals(50, balance.getBalance());
    }

    @Test
    void testDuplicateEnemyReward() {
        assertTrue(rewards.rewardEnemy("enemy-1", "BASIC"));
        assertFalse(rewards.rewardEnemy("enemy-1", "BASIC"));
        assertEquals(10, balance.getBalance());
    }

    @Test
    void testLevelCompletionReward() {
        assertTrue(rewards.rewardLevel("level-1", 3));
        assertEquals(300, balance.getBalance());
    }

    @Test
    void testDuplicateLevelReward() {
        assertTrue(rewards.rewardLevel("level-1", 2));
        assertFalse(rewards.rewardLevel("level-1", 2));
        assertEquals(200, balance.getBalance());
    }

    @Test
    void testUnknownEnemy() {
        assertFalse(rewards.rewardEnemy("enemy-4", "UNKNOWN"));
        assertEquals(0, balance.getBalance());
    }

    @Test
    void testInvalidDifficulty() {
        assertFalse(rewards.rewardLevel("level-2", -1));
        assertFalse(rewards.rewardLevel("level-3", 0));
        assertEquals(0, balance.getBalance());
    }

    @Test
    void testCustomEnemyReward() {
        assertTrue(rewards.setEnemyReward("BOSS", 500));
        assertTrue(rewards.rewardEnemy("boss-1", "BOSS"));
        assertEquals(500, balance.getBalance());
    }

    @Test
    void testInvalidEventId() {
        assertFalse(rewards.rewardEnemy("", "BASIC"));
        assertFalse(rewards.rewardEnemy(null, "BASIC"));
        assertEquals(0, balance.getBalance());
    }

    @Test
    void testOverflowProtection() {
        CurrencyBalance maxBalance =
            new CurrencyBalance(Integer.MAX_VALUE);

        RewardSystem maxRewards = new RewardSystem(maxBalance);

        assertFalse(maxRewards.rewardEnemy("enemy-5", "BASIC"));
        assertEquals(Integer.MAX_VALUE, maxBalance.getBalance());
        assertFalse(maxRewards.isEventProcessed("enemy-5"));
    }

    @Test
    void testInvalidBalance() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new RewardSystem(null)
        );
    }

    @Test
    void testInvalidRewardConfiguration() {
        assertFalse(rewards.setEnemyReward(null, 10));
        assertFalse(rewards.setEnemyReward("BASIC", -10));
        assertFalse(rewards.setBaseLevelReward(0));
        assertEquals(10, rewards.getEnemyReward("BASIC"));
        assertEquals(100, rewards.getBaseLevelReward());
    }

    @Test
    void testLevelRewardMultiplicationOverflow() {
        assertFalse(rewards.rewardLevel("level-overflow", Integer.MAX_VALUE));
        assertEquals(0, balance.getBalance());
        assertFalse(rewards.isEventProcessed("level-overflow"));
    }
}