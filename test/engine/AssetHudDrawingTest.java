package engine;

import currency.ConsumableActivation;
import currency.ItemUseResult;
import currency.PlayerAssets;
import currency.RewardSystem;
import hud.AssetHudProvider;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;
import screen.Screen;
import shop.ItemSlots;
import shop.ShopController;
import static org.junit.jupiter.api.Assertions.*;

class AssetHudDrawingTest {
    @Test
    void slotDrawingStaysInsideTheRequestedArea() throws Exception {
        PlayerAssets assets = new PlayerAssets();
        ShopController shop = new ShopController(assets, Core.getFileManager());
        ItemSlots slots = new ItemSlots(shop);
        slots.bind(Map.of("field.item", new ConsumableActivation() {
            @Override
            public ItemUseResult validate() {
                return ItemUseResult.SUCCESS;
            }

            @Override
            public ItemUseResult activate() {
                return ItemUseResult.SUCCESS;
            }
        }));
        assertTrue(slots.tryCollect("field.item"));
        AssetHudProvider hud = new AssetHudProvider(shop, new RewardSystem(assets), slots,
                Map.of("field.item", "A very long field item name that must fit the HUD"));
        Screen screen = new Screen(448, 520, 60);
        DrawManager draws = Core.getDrawManager();
        draws.initDrawing(screen);
        try {
            Field bufferField = DrawManager.class.getDeclaredField("backBuffer");
            bufferField.setAccessible(true);
            BufferedImage buffer = (BufferedImage) bufferField.get(null);
            int[] before = buffer.getRGB(0, 0, 448, 520, null, 0, 448);
            Field graphicsField = DrawManager.class.getDeclaredField("backBufferGraphics");
            graphicsField.setAccessible(true);
            Graphics graphics = (Graphics) graphicsField.get(null);
            Object previousFont = graphics.getFont();
            Object previousColor = graphics.getColor();

            int height = draws.drawItemSlots(screen, hud.snapshot(), 10, 50, 180);

            assertEquals(previousFont, graphics.getFont());
            assertEquals(previousColor, graphics.getColor());
            int changed = 0;
            for (int y = 0; y < 520; y++) {
                for (int x = 0; x < 448; x++) {
                    if (before[y * 448 + x] != buffer.getRGB(x, y)) {
                        assertTrue(x >= 10 && x < 190 && y >= 50 && y < 50 + height);
                        changed++;
                    }
                }
            }
            assertTrue(changed > 0);
        } finally {
            draws.completeDrawing(screen);
        }
    }
}
