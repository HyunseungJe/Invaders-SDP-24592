package engine;

import currency.ConsumableActivation;
import currency.PlayerAssets;
import currency.RewardSystem;
import hud.AssetHudProvider;
import hud.AssetHudSnapshot;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import shop.ItemSlots;
import shop.ShopController;
import shop.ShopProduct;

/** Shares session assets and keeps one slot container for each Campaign run. */
public final class GameplayAssets {
    private final ShopController shop;
    private final RewardSystem rewards;
    private final RewardSettlementCoordinator settlement;
    private ItemSlots slots;
    private AssetHudProvider hud;
    private boolean itemUseFailed;

    public GameplayAssets(PlayerAssets assets, FileManager files, List<ShopProduct> products) {
        AssetSaveState saveState = new AssetSaveState();
        shop = new ShopController(assets, files, products, saveState);
        rewards = new RewardSystem(assets);
        settlement = new RewardSettlementCoordinator(rewards, assets, files, saveState);
    }

    public ShopController getShop() {
        return shop;
    }

    public RewardSystem getRewards() {
        return rewards;
    }

    public void beginRun() {
        if (itemUseFailed) {
            throw new IllegalStateException("Check assets and effects before starting another run.");
        }
        if (slots != null) {
            throw new IllegalStateException("A Campaign run is already active.");
        }
        slots = new ItemSlots(shop);
        hud = new AssetHudProvider(shop, rewards, slots, Collections.<String, String>emptyMap());
    }

    public ItemSlots getSlots() {
        if (slots == null) {
            throw new IllegalStateException("No Campaign run is active.");
        }
        return slots;
    }

    /** Bind after the new stage's ship and effect managers have been created. */
    public void bindItemEffects(Map<String, ConsumableActivation> effects,
            Map<String, String> fieldItemNames) {
        ItemSlots currentSlots = getSlots();
        AssetHudProvider nextHud = new AssetHudProvider(shop, rewards, currentSlots, fieldItemNames);
        currentSlots.bind(effects);
        currentSlots.equipStartingItems();
        hud = nextHud;
    }

    /** Read on the gameplay thread after input and event processing. */
    public AssetHudSnapshot snapshot() {
        getSlots();
        return hud.snapshot();
    }

    /** Settles only earned rewards; the completion listener owns the clear bonus. */
    public SettlementResult settleStage() {
        return settlement.processSettlement();
    }

    public SettlementResult retrySave() {
        return settlement.retrySave();
    }

    /** Preserves pending rewards on transfer failure and never repeats item effects. */
    public SettlementResult prepareExit() {
        if (rewards.getPendingCurrency() > 0) {
            return settlement.processSettlement();
        }
        return shop.retrySave() ? SettlementResult.SUCCESS : SettlementResult.SAVE_FAILED;
    }

    public void endRun() {
        ItemSlots finishedSlots = getSlots();
        if (finishedSlots.hasFailure()) {
            itemUseFailed = true;
        } else {
            finishedSlots.clear();
        }
        slots = null;
        hud = null;
    }
}
