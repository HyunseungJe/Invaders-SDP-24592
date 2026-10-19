package engine;

import currency.PlayerAssetsState;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import shop.ShopController;
import shop.ShopTestFixtures;
import static org.junit.jupiter.api.Assertions.*;

class CoreShopControllerAccessTest {
    @Test
    void accessBeforeInitializationFailsWithoutCreatingASecondSession() throws Exception {
        Field field = Core.class.getDeclaredField("shopController");
        field.setAccessible(true);
        Object previous = field.get(null);
        try {
            field.set(null, null);
            assertThrows(IllegalStateException.class, Core::getShopController);
            assertNull(field.get(null));
        } finally {
            field.set(null, previous);
        }
    }

    @Test
    void accessReturnsExistingSessionAndSupportsPublicProductConfiguration() throws Exception {
        ShopController configured = new ShopController(ShopTestFixtures.fundedAssets(),
                Core.getFileManager(), ShopTestFixtures.products());
        Field field = Core.class.getDeclaredField("shopController");
        field.setAccessible(true);
        Object previous = field.get(null);
        try {
            field.set(null, configured);
            assertSame(configured, Core.getShopController());
            assertSame(configured, Core.getShopController());
            PlayerAssetsState state = Core.getShopController().snapshot();
            assertEquals(1000, state.getBalance());
            assertEquals(2, configured.getProducts().size());
            assertEquals(ShopController.SaveStatus.NOT_NEEDED, configured.getSaveStatus());
        } finally {
            field.set(null, previous);
        }
    }
}
