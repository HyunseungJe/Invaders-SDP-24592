package engine;

import currency.PlayerAssets;
import currency.RewardSystem;
import event.GameplayEventListener;
import event.GameplayEventPublisher;
import event.LevelCompletedEvent;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Campaign clear rewards for one application session; use on the gameplay thread. */
final class CampaignRewards {
    private final RewardSystem rewards;
    private final RewardSettlementCoordinator settlement;
    private final Deque<Integer> waitingCompletions = new ArrayDeque<Integer>();
    private SettlementResult lastResult = SettlementResult.SUCCESS;

    CampaignRewards(PlayerAssets assets, FileManager files) {
        this(assets, files, null);
    }

    CampaignRewards(PlayerAssets assets, FileManager files, Path savePath) {
        rewards = new RewardSystem(assets);
        settlement = new RewardSettlementCoordinator(rewards, assets, files, savePath);
    }

    StageSubscription subscribe(GameplayEventPublisher events, int level) {
        Objects.requireNonNull(events, "events");
        if (level < 1) throw new IllegalArgumentException("Campaign level must be positive.");
        return new StageSubscription(events, level);
    }

    SettlementResult getLastResult() {
        return lastResult;
    }

    /** Unpaid rewards from stages whose screens have finished, including queued completions. */
    long getPendingCurrency() {
        long pending = rewards.getPendingCurrency();
        for (int difficulty : waitingCompletions) {
            pending += rewards.getLevelReward(difficulty);
        }
        return pending;
    }

    /** Retries unpaid rewards and saves the latest assets without repeating paid bonuses. */
    SettlementResult retryPendingRewards() {
        if (rewards.getPendingCurrency() > 0) {
            lastResult = settlement.processSettlement();
            if (lastResult != SettlementResult.SUCCESS) return lastResult;
        }
        // Keep later completions queued if the balance is full, including across new runs.
        while (!waitingCompletions.isEmpty()) {
            if (!rewards.rewardLevel(waitingCompletions.peekFirst())) {
                lastResult = SettlementResult.SETTLEMENT_FAILED;
                return lastResult;
            }
            waitingCompletions.removeFirst();
            lastResult = settlement.processSettlement();
            if (lastResult != SettlementResult.SUCCESS) return lastResult;
        }
        if (lastResult == SettlementResult.SAVE_FAILED) {
            lastResult = settlement.retrySave();
        }
        return lastResult;
    }

    /** Each screen is a separate attempt, so a new run can earn the same stage again. */
    final class StageSubscription implements AutoCloseable, GameplayEventListener<LevelCompletedEvent> {
        private final GameplayEventPublisher events;
        private final int level;
        private Integer completedDifficulty;
        private boolean closed;

        private StageSubscription(GameplayEventPublisher events, int level) {
            this.events = events;
            this.level = level;
            events.subscribe(LevelCompletedEvent.class, this);
        }

        @Override
        public void onEvent(LevelCompletedEvent event) {
            if (closed || completedDifficulty != null || event.getLevelId() != level) return;
            completedDifficulty = event.getDifficulty();
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            events.unsubscribe(LevelCompletedEvent.class, this);
            if (completedDifficulty == null) return;
            // Apply the clear bonus after the game screen, including its end delay, has finished.
            waitingCompletions.addLast(completedDifficulty);
            SettlementResult result = retryPendingRewards();
            if (result != SettlementResult.SUCCESS) {
                Core.getLogger().warning("Campaign clear reward needs retry: " + result);
            }
        }
    }
}
