package currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemUseServiceTest {
    private static final String ITEM_ID = "test_consumable";
    private static final String OTHER_ID = "test_other";
    private static final String UNLOCK_ID = "test_ship";

    private PlayerAssets assets;
    private ShopCatalog catalog;
    private Map<String, ConsumableActivation> activations;
    private TestActivation activation;
    private ItemUseService service;

    @BeforeEach
    void setUp() {
        assets = new PlayerAssets();
        assets.addCurrency(100);
        assets.addItem(ITEM_ID, 2);
        assets.addItem(OTHER_ID, 3);
        assets.unlock(UNLOCK_ID);
        catalog = new ShopCatalog();
        catalog.registerInventoryItem(ITEM_ID, 10);
        catalog.registerInventoryItem(OTHER_ID, 20);
        catalog.registerUnlockable(UNLOCK_ID, 30);
        activation = new TestActivation();
        activations = new HashMap<>();
        activations.put(ITEM_ID, activation);
        service = new ItemUseService(assets, catalog, activations);
    }

    @Test
    void successfulUseReservesOneUnitBeforeActivation() {
        PlayerAssetsState before = assets.snapshot();

        assertEquals(ItemUseResult.SUCCESS, service.tryUse(ITEM_ID));

        assertEquals(1, activation.validationCalls);
        assertEquals(2, activation.stockAtValidation);
        assertEquals(1, activation.activationCalls);
        assertEquals(1, activation.stockAtActivation);
        assertTrue(activation.effectActive);
        assertEquals(1, assets.getItemQuantity(ITEM_ID));
        assertUnrelatedAssetsUnchanged(before);
    }

    @Test
    void differentItemsUseOnlyTheirOwnEffectsAndStock() {
        TestActivation other = new TestActivation();
        other.onActivate = () -> assertEquals(2, assets.getItemQuantity(OTHER_ID));
        activations.put(OTHER_ID, other);
        service = new ItemUseService(assets, catalog, activations);
        PlayerAssetsState before = assets.snapshot();

        assertEquals(ItemUseResult.SUCCESS, service.tryUse(OTHER_ID));
        assertEquals(0, activation.validationCalls);
        assertEquals(0, activation.activationCalls);
        assertFalse(activation.effectActive);
        assertEquals(1, other.validationCalls);
        assertEquals(1, other.activationCalls);
        assertTrue(other.effectActive);
        assertEquals(2, assets.getItemQuantity(ITEM_ID));
        assertEquals(2, assets.getItemQuantity(OTHER_ID));
        assertEquals(before.getBalance(), assets.getBalance());
        assertEquals(before.getUnlockedContentIds(), assets.snapshot().getUnlockedContentIds());

        assertEquals(ItemUseResult.SUCCESS, service.tryUse(ITEM_ID));
        assertEquals(1, activation.validationCalls);
        assertEquals(1, activation.activationCalls);
        assertTrue(activation.effectActive);
        assertEquals(1, other.validationCalls);
        assertEquals(1, other.activationCalls);
        assertTrue(other.effectActive);
        Map<String, Integer> expected = new HashMap<>(before.getItemQuantities());
        expected.put(ITEM_ID, 1);
        expected.put(OTHER_ID, 2);
        assertEquals(expected, assets.snapshot().getItemQuantities());
        assertEquals(before.getBalance(), assets.getBalance());
        assertEquals(before.getUnlockedContentIds(), assets.snapshot().getUnlockedContentIds());
    }

    @Test
    void purchaseAndUseShareAssetsWithoutChargingTwice() {
        assets.removeItem(ITEM_ID, 2);
        PurchaseSystem purchases = new PurchaseSystem(assets, catalog);
        assertEquals(PurchaseResult.SUCCESS, purchases.purchaseInventoryItem(ITEM_ID, 2));
        assertEquals(80, assets.getBalance());
        assertEquals(2, assets.getItemQuantity(ITEM_ID));

        assertEquals(ItemUseResult.SUCCESS, service.tryUse(ITEM_ID));
        assertEquals(80, assets.getBalance());
        assertEquals(1, assets.getItemQuantity(ITEM_ID));
        assertEquals(1, activation.activationCalls);
    }

    @Test
    void invalidAndUnsupportedIdsLeaveAssetsUnchanged() {
        PlayerAssetsState before = assets.snapshot();
        for (String id : new String[] {null, "", " \t "}) {
            assertEquals(ItemUseResult.INVALID_ITEM_ID, service.tryUse(id));
        }
        for (String id : new String[] {"unknown", OTHER_ID, UNLOCK_ID, " " + ITEM_ID}) {
            assertEquals(ItemUseResult.UNSUPPORTED_ITEM, service.tryUse(id));
        }
        assertEquals(0, activation.validationCalls);
        assertEquals(0, activation.activationCalls);
        assertAssetsEqual(before);
    }

    @Test
    void registeredAdapterDoesNotMakeAnUnknownOrUnlockableProductUsable() {
        activations.put("unknown", activation);
        activations.put(UNLOCK_ID, activation);
        service = new ItemUseService(assets, catalog, activations);
        PlayerAssetsState before = assets.snapshot();

        assertEquals(ItemUseResult.UNSUPPORTED_ITEM, service.tryUse("unknown"));
        assertEquals(ItemUseResult.UNSUPPORTED_ITEM, service.tryUse(UNLOCK_ID));
        assertEquals(0, activation.validationCalls);
        assertEquals(0, activation.activationCalls);
        assertAssetsEqual(before);
    }

    @Test
    void emptyStockDoesNotActivateAndRestockingAllowsUse() {
        assets.removeItem(ITEM_ID, 2);
        PlayerAssetsState before = assets.snapshot();

        assertEquals(ItemUseResult.OUT_OF_STOCK, service.tryUse(ITEM_ID));
        assertEquals(1, activation.validationCalls);
        assertEquals(0, activation.activationCalls);
        assertFalse(activation.effectActive);
        assertAssetsEqual(before);

        assets.addItem(ITEM_ID, 1);
        assertEquals(ItemUseResult.SUCCESS, service.tryUse(ITEM_ID));
        assertEquals(0, assets.getItemQuantity(ITEM_ID));
        assertEquals(ItemUseResult.OUT_OF_STOCK, service.tryUse(ITEM_ID));
        assertEquals(1, activation.activationCalls);
    }

    @Test
    void validationRejectionsLeaveAssetsUnchangedAndAllowLaterUse() {
        PlayerAssetsState before = assets.snapshot();
        for (ItemUseResult rejection : rejections()) {
            activation.validationResult = rejection;
            assertEquals(rejection, service.tryUse(ITEM_ID));
            assertAssetsEqual(before);
        }
        assertEquals(0, activation.activationCalls);
        assertFalse(activation.effectActive);

        activation.validationResult = ItemUseResult.SUCCESS;
        assertEquals(ItemUseResult.SUCCESS, service.tryUse(ITEM_ID));
    }

    @Test
    void activationRejectionsRestoreStockAndAllowLaterUse() {
        PlayerAssetsState before = assets.snapshot();
        for (ItemUseResult rejection : rejections()) {
            activation.activationResult = rejection;
            assertEquals(rejection, service.tryUse(ITEM_ID));
            assertEquals(1, activation.stockAtActivation);
            assertFalse(activation.effectActive);
            assertAssetsEqual(before);
        }
        assertEquals(3, activation.activationCalls);

        activation.activationResult = ItemUseResult.SUCCESS;
        assertEquals(ItemUseResult.SUCCESS, service.tryUse(ITEM_ID));
        assertEquals(1, assets.getItemQuantity(ITEM_ID));
    }

    @Test
    void rejectedUseRestoresLastUnitAfterInventoryEntryIsRemoved() {
        assertTrue(assets.removeItem(ITEM_ID, 1));
        activation.activationResult = ItemUseResult.EFFECT_REJECTED;
        activation.onActivate = () -> assertFalse(assets.hasItem(ITEM_ID));
        PlayerAssetsState before = assets.snapshot();

        assertEquals(ItemUseResult.EFFECT_REJECTED, service.tryUse(ITEM_ID));
        assertEquals(1, activation.validationCalls);
        assertEquals(1, activation.stockAtValidation);
        assertEquals(1, activation.activationCalls);
        assertEquals(0, activation.stockAtActivation);
        assertFalse(activation.effectActive);
        assertTrue(assets.hasItem(ITEM_ID));
        assertAssetsEqual(before);
    }

    @Test
    void refundRestoresMaximumStockWithoutOverflow() {
        assets.addItem(ITEM_ID, Integer.MAX_VALUE - 2);
        activation.activationResult = ItemUseResult.EFFECT_REJECTED;
        PlayerAssetsState before = assets.snapshot();

        assertEquals(ItemUseResult.EFFECT_REJECTED, service.tryUse(ITEM_ID));
        assertEquals(Integer.MAX_VALUE - 1, activation.stockAtActivation);
        assertAssetsEqual(before);
    }

    @Test
    void constructorCopiesActivationMapping() {
        TestActivation replacement = new TestActivation();
        activations.put(ITEM_ID, replacement);
        activations.put(OTHER_ID, replacement);

        assertEquals(ItemUseResult.SUCCESS, service.tryUse(ITEM_ID));
        assertEquals(ItemUseResult.UNSUPPORTED_ITEM, service.tryUse(OTHER_ID));
        assertEquals(1, activation.activationCalls);
        assertEquals(0, replacement.validationCalls);
        activations.clear();
        assertEquals(ItemUseResult.SUCCESS, service.tryUse(ITEM_ID));
        assertEquals(2, activation.activationCalls);
    }

    @Test
    void missingDependenciesAndInvalidMappingsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ItemUseService(null, catalog, activations));
        assertThrows(IllegalArgumentException.class,
                () -> new ItemUseService(assets, null, activations));
        assertThrows(IllegalArgumentException.class,
                () -> new ItemUseService(assets, catalog, null));
        for (String id : new String[] {null, "", " \t "}) {
            Map<String, ConsumableActivation> invalid = new HashMap<>();
            invalid.put(id, activation);
            assertThrows(IllegalArgumentException.class,
                    () -> new ItemUseService(assets, catalog, invalid));
        }
        activations.put(OTHER_ID, null);
        assertThrows(IllegalArgumentException.class,
                () -> new ItemUseService(assets, catalog, activations));
        assertDoesNotThrow(() -> new ItemUseService(assets, catalog, new HashMap<>()));
    }

    @Test
    void validationExceptionBlocksFurtherUseWithoutReservingStock() {
        RuntimeException cause = new IllegalArgumentException("Invalid test context");
        activation.validationFailure = cause;
        PlayerAssetsState before = assets.snapshot();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.tryUse(ITEM_ID));
        assertSame(cause, failure.getCause());
        assertAssetsEqual(before);
        assertEquals(0, activation.activationCalls);
        assertServiceBlocked();
    }

    @Test
    void activationExceptionKeepsReservationAndBlocksAllAdapters() {
        TestActivation other = new TestActivation();
        activations.put(OTHER_ID, other);
        service = new ItemUseService(assets, catalog, activations);
        RuntimeException cause = new IllegalArgumentException("Partial test effect");
        activation.activationFailure = cause;
        PlayerAssetsState before = assets.snapshot();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.tryUse(ITEM_ID));
        assertSame(cause, failure.getCause());
        assertTrue(activation.effectActive);
        assertEquals(1, assets.getItemQuantity(ITEM_ID));
        assertUnrelatedAssetsUnchanged(before);
        assertServiceBlocked();
        assertThrows(IllegalStateException.class, () -> service.tryUse(OTHER_ID));
        assertEquals(0, other.validationCalls);
        assertEquals(0, other.activationCalls);
    }

    @Test
    void invalidValidationResultsBlockUseWithoutChangingAssets() {
        PlayerAssetsState before = assets.snapshot();
        for (ItemUseResult result : invalidResults()) {
            activation.validationResult = result;
            service = new ItemUseService(assets, catalog, activations);
            assertThrows(IllegalStateException.class, () -> service.tryUse(ITEM_ID));
            assertAssetsEqual(before);
            assertEquals(0, activation.activationCalls);
            assertServiceBlocked();
        }
    }

    @Test
    void invalidActivationResultsDoNotRefundAnUnknownEffect() {
        for (ItemUseResult result : invalidResults()) {
            setUp();
            activation.activationResult = result;
            PlayerAssetsState before = assets.snapshot();
            assertThrows(IllegalStateException.class, () -> service.tryUse(ITEM_ID));
            assertEquals(1, assets.getItemQuantity(ITEM_ID));
            assertEquals(1, activation.activationCalls);
            assertUnrelatedAssetsUnchanged(before);
            assertServiceBlocked();
        }
    }

    @Test
    void failedRefundIsReportedAndStopsFurtherUse() {
        // An adapter that changes stock violates the contract and can prevent a refund.
        activation.activationResult = ItemUseResult.EFFECT_REJECTED;
        activation.onActivate = () -> assets.addItem(ITEM_ID, Integer.MAX_VALUE - 1);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.tryUse(ITEM_ID));
        assertEquals("Could not restore reserved stock.", failure.getCause().getMessage());
        assertEquals(Integer.MAX_VALUE, assets.getItemQuantity(ITEM_ID));
        assertServiceBlocked();
    }

    @Test
    void reentrantValidationCannotConsumeStockEvenIfAdapterCatchesTheError() {
        activation.onValidate = () -> assertThrows(IllegalStateException.class,
                () -> service.tryUse(ITEM_ID));
        PlayerAssetsState before = assets.snapshot();

        assertThrows(IllegalStateException.class, () -> service.tryUse(ITEM_ID));
        assertEquals(0, activation.activationCalls);
        assertAssetsEqual(before);
        assertServiceBlocked();
    }

    @Test
    void reentrantActivationCannotConsumeAnotherUnit() {
        activation.onActivate = () -> assertThrows(IllegalStateException.class,
                () -> service.tryUse(ITEM_ID));

        assertThrows(IllegalStateException.class, () -> service.tryUse(ITEM_ID));
        assertEquals(1, activation.activationCalls);
        assertEquals(1, assets.getItemQuantity(ITEM_ID));
        assertServiceBlocked();
    }

    @Test
    void activationErrorIsRethrownAndKeepsTheServiceBlocked() {
        AssertionError cause = new AssertionError("Test activation error");
        activation.onActivate = () -> { throw cause; };

        assertSame(cause, assertThrows(AssertionError.class, () -> service.tryUse(ITEM_ID)));
        assertEquals(1, assets.getItemQuantity(ITEM_ID));
        assertServiceBlocked();
    }

    private void assertServiceBlocked() {
        int validationCalls = activation.validationCalls;
        int activationCalls = activation.activationCalls;
        assertThrows(IllegalStateException.class, () -> service.tryUse(ITEM_ID));
        assertEquals(validationCalls, activation.validationCalls);
        assertEquals(activationCalls, activation.activationCalls);
    }

    private void assertAssetsEqual(PlayerAssetsState before) {
        PlayerAssetsState after = assets.snapshot();
        assertEquals(before.getBalance(), after.getBalance());
        assertEquals(before.getUnlockedContentIds(), after.getUnlockedContentIds());
        assertEquals(before.getItemQuantities(), after.getItemQuantities());
    }

    private void assertUnrelatedAssetsUnchanged(PlayerAssetsState before) {
        assertEquals(before.getBalance(), assets.getBalance());
        assertEquals(before.getUnlockedContentIds(), assets.snapshot().getUnlockedContentIds());
        Map<String, Integer> expected = new HashMap<>(before.getItemQuantities());
        Map<String, Integer> actual = new HashMap<>(assets.snapshot().getItemQuantities());
        expected.remove(ITEM_ID);
        actual.remove(ITEM_ID);
        assertEquals(expected, actual);
    }

    private ItemUseResult[] rejections() {
        return new ItemUseResult[] {ItemUseResult.INVALID_CONTEXT,
                ItemUseResult.ALREADY_ACTIVE, ItemUseResult.EFFECT_REJECTED};
    }

    private ItemUseResult[] invalidResults() {
        return new ItemUseResult[] {null, ItemUseResult.INVALID_ITEM_ID,
                ItemUseResult.UNSUPPORTED_ITEM, ItemUseResult.OUT_OF_STOCK};
    }

    private class TestActivation implements ConsumableActivation {
        private ItemUseResult validationResult = ItemUseResult.SUCCESS;
        private ItemUseResult activationResult = ItemUseResult.SUCCESS;
        private RuntimeException validationFailure;
        private RuntimeException activationFailure;
        private Runnable onValidate;
        private Runnable onActivate;
        private int validationCalls;
        private int activationCalls;
        private int stockAtValidation;
        private int stockAtActivation;
        private boolean effectActive;

        @Override
        public ItemUseResult validate() {
            validationCalls++;
            stockAtValidation = assets.getItemQuantity(ITEM_ID);
            if (onValidate != null) {
                onValidate.run();
            }
            if (validationFailure != null) {
                throw validationFailure;
            }
            return validationResult;
        }

        @Override
        public ItemUseResult activate() {
            activationCalls++;
            stockAtActivation = assets.getItemQuantity(ITEM_ID);
            if (onActivate != null) {
                onActivate.run();
            }
            if (activationFailure != null) {
                effectActive = true;
                throw activationFailure;
            }
            if (activationResult == ItemUseResult.SUCCESS) {
                effectActive = true;
            }
            return activationResult;
        }
    }
}
