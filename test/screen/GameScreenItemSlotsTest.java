package screen;

import audio.AudioManager;
import currency.ConsumableActivation;
import currency.ItemUseResult;
import currency.PlayerAssets;
import engine.Core;
import engine.GameSettings;
import engine.GameState;
import engine.GameplayAssets;
import entity.PlayerStatus;
import entity.Ship;
import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shop.ItemSlots;
import shop.ShopController;
import static org.junit.jupiter.api.Assertions.*;

class GameScreenItemSlotsTest {
    private final Canvas keySource = new Canvas();
    private GameScreen screen;
    private ItemSlots slots;
    private int activations;
    private ItemUseResult validation;

    @BeforeEach
    void setUp() {
        releaseKeys();
        activations = 0;
        validation = ItemUseResult.SUCCESS;
        slots = new ItemSlots(new ShopController(new PlayerAssets(), Core.getFileManager()));
        slots.bind(Map.of("field.item", new ConsumableActivation() {
            @Override
            public ItemUseResult validate() {
                return validation;
            }

            @Override
            public ItemUseResult activate() {
                activations++;
                return ItemUseResult.SUCCESS;
            }
        }));
        screen = new GameScreen(new GameState(1, 0, 3, 0, 0),
                new GameSettings(1, 1, 60, 2000), false, 448, 520, 60);
        screen.setItemSlots(slots);
    }

    @AfterEach
    void releaseKeys() {
        key(KeyEvent.VK_1, false);
        key(KeyEvent.VK_2, false);
    }

    @Test
    void heldKeyDoesNotUseTheNextCollectedItem() {
        assertTrue(screen.tryCollectItem("field.item"));
        key(KeyEvent.VK_1, true);
        process(true);
        assertEquals(1, activations);
        assertNull(slots.getSlot(0));
        assertTrue(screen.tryCollectItem("field.item"));

        process(true);
        assertEquals(1, activations);
        assertNotNull(slots.getSlot(0));
        key(KeyEvent.VK_1, false);
        process(true);
        key(KeyEvent.VK_1, true);
        process(true);
        assertEquals(2, activations);
    }

    @Test
    void heldEntryAndBlockedInputNeedAReleaseBeforeUse() {
        assertTrue(screen.tryCollectItem("field.item"));
        key(KeyEvent.VK_1, true);
        screen.setItemSlots(slots);
        process(true);
        assertEquals(0, activations);
        key(KeyEvent.VK_1, false);
        process(false);
        key(KeyEvent.VK_1, true);
        process(false);
        process(true);
        assertEquals(0, activations);

        key(KeyEvent.VK_1, false);
        process(true);
        key(KeyEvent.VK_1, true);
        process(true);
        assertEquals(1, activations);
    }

    @Test
    void movementOrFireBindingDoesNotAlsoConsumeAnItem() {
        for (int[] bindings : new int[][] {
                {KeyEvent.VK_1, KeyEvent.VK_RIGHT, KeyEvent.VK_SPACE},
                {KeyEvent.VK_LEFT, KeyEvent.VK_1, KeyEvent.VK_SPACE},
                {KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_1}}) {
            if (slots.getSlot(0) == null) assertTrue(screen.tryCollectItem("field.item"));
            key(KeyEvent.VK_1, true);
            screen.processItemInput(true, bindings[0], bindings[1], bindings[2]);
            assertEquals(0, activations);
            assertNotNull(slots.getSlot(0));
            key(KeyEvent.VK_1, false);
            process(true);
        }
    }

    @Test
    void bothSlotsUseOnceAndRejectionWaitsForAnotherPress() {
        assertTrue(screen.tryCollectItem("field.item"));
        assertTrue(screen.tryCollectItem("field.item"));
        assertFalse(screen.tryCollectItem("field.item"));
        validation = ItemUseResult.INVALID_CONTEXT;
        key(KeyEvent.VK_1, true);
        key(KeyEvent.VK_2, true);
        process(true);
        validation = ItemUseResult.SUCCESS;
        process(true);
        assertEquals(0, activations);
        releaseKeys();
        process(true);
        key(KeyEvent.VK_1, true);
        key(KeyEvent.VK_2, true);
        process(true);
        process(true);
        assertEquals(2, activations);
        assertNull(slots.getSlot(0));
        assertNull(slots.getSlot(1));
    }

    @Test
    void screenUpdateUsesSlotsOnlyDuringActiveGameplay() throws Exception {
        boolean previouslyMuted = AudioManager.isMuted();
        AudioManager.setMuted(true);
        try {
            screen.initialize();
            assertTrue(screen.tryCollectItem("field.item"));
            key(KeyEvent.VK_1, true);
            screen.update();
            assertEquals(0, activations, "Countdown must block item use");

            screen.inputDelay = Core.getCooldown(0);
            screen.update();
            assertEquals(0, activations, "A key held through countdown needs a release");
            key(KeyEvent.VK_1, false);
            screen.update();
            key(KeyEvent.VK_1, true);
            screen.update();
            assertEquals(1, activations);
            assertNull(slots.getSlot(0));

            assertTrue(screen.tryCollectItem("field.item"));
            Ship ship = (Ship) read("ship");
            ship.destroy();
            pressDuringUpdate();
            assertEquals(1, activations, "A destroyed ship must not use an item");

            PlayerStatus status = (PlayerStatus) read("playerStatus");
            assertTrue(status.takeDamage(status.getLives()));
            pressDuringUpdate();
            assertEquals(1, activations, "A dead player must not use an item");
            assertNotNull(slots.getSlot(0));
        } finally {
            AudioManager.stopBGM();
            AudioManager.setMuted(previouslyMuted);
        }
    }

    @Test
    void finishedStageDoesNotConsumeSlots() throws Exception {
        boolean previouslyMuted = AudioManager.isMuted();
        AudioManager.setMuted(true);
        try {
            screen.initialize();
            screen.inputDelay = Core.getCooldown(0);
            Field finished = GameScreen.class.getDeclaredField("levelFinished");
            finished.setAccessible(true);
            finished.setBoolean(screen, true);
            assertTrue(screen.tryCollectItem("field.item"));

            pressDuringUpdate();

            assertEquals(0, activations);
            assertNotNull(slots.getSlot(0));
        } finally {
            AudioManager.stopBGM();
            AudioManager.setMuted(previouslyMuted);
        }
    }

    @Test
    void campaignStagesKeepSlotsAndRebindEffectsToTheNewScreen() {
        boolean previouslyMuted = AudioManager.isMuted();
        AudioManager.setMuted(true);
        GameplayAssets gameplay = new GameplayAssets(new PlayerAssets(), Core.getFileManager(),
                java.util.Collections.emptyList());
        gameplay.beginRun();
        final int[] stageEffects = {0, 0};
        try {
            screen.setGameplayAssets(gameplay);
            screen.initialize();
            screen.bindItemEffects(stageAdapter(stageEffects, 0), Map.of("field.item", "Field item"));
            assertTrue(screen.tryCollectItem("field.item"));
            assertTrue(screen.damagePlayer(1, "test"));
            assertEquals("field.item", screen.getAssetHudSnapshot().getSlots().get(0).getItemId());
            ItemSlots carried = gameplay.getSlots();

            GameScreen next = new GameScreen(new GameState(2, 0, 2, 0, 0),
                    new GameSettings(1, 1, 60, 2000), false, 448, 520, 60);
            next.setGameplayAssets(gameplay);
            next.initialize();
            next.bindItemEffects(stageAdapter(stageEffects, 1), Map.of("field.item", "Field item"));
            next.inputDelay = Core.getCooldown(0);
            assertSame(carried, gameplay.getSlots());
            assertEquals("field.item", next.getAssetHudSnapshot().getSlots().get(0).getItemId());
            key(KeyEvent.VK_1, true);
            next.update();

            assertArrayEquals(new int[] {0, 1}, stageEffects);
            assertTrue(next.getAssetHudSnapshot().getSlots().get(0).isEmpty());
        } finally {
            gameplay.endRun();
            AudioManager.stopBGM();
            AudioManager.setMuted(previouslyMuted);
        }
    }

    private Map<String, ConsumableActivation> stageAdapter(int[] counts, int stage) {
        return Map.of("field.item", new ConsumableActivation() {
            @Override
            public ItemUseResult validate() {
                return ItemUseResult.SUCCESS;
            }

            @Override
            public ItemUseResult activate() {
                counts[stage]++;
                return ItemUseResult.SUCCESS;
            }
        });
    }

    private Object read(String name) throws Exception {
        Field field = GameScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(screen);
    }

    private void pressDuringUpdate() {
        key(KeyEvent.VK_1, false);
        screen.update();
        key(KeyEvent.VK_1, true);
        screen.update();
    }

    private void process(boolean acceptsInput) {
        screen.processItemInput(acceptsInput, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_SPACE);
    }

    private void key(int code, boolean down) {
        KeyEvent event = new KeyEvent(keySource,
                down ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED, 0, 0, code, KeyEvent.CHAR_UNDEFINED);
        if (down) Core.getInputManager().keyPressed(event);
        else Core.getInputManager().keyReleased(event);
    }
}
