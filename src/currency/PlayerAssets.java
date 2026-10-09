package currency;

import java.util.Map;

/**
 * Provides unified access to the player's currency, unlocks and inventory.
 * State validation is delegated to the component that owns each asset.
 * File storage and gameplay policies are handled outside this class.
 */
public class PlayerAssets {
    private CurrencyBalance currencyBalance;
    private UnlockManager unlockManager;
    private InventoryManager inventoryManager;

    /** Creates an empty asset state. */
    public PlayerAssets() {
        this.currencyBalance = new CurrencyBalance();
        this.unlockManager = new UnlockManager();
        this.inventoryManager = new InventoryManager();
    }

    public int getBalance() {
        return this.currencyBalance.getBalance();
    }

    public boolean canAfford(int amount) {
        return this.currencyBalance.canAfford(amount);
    }

    public boolean isUnlocked(String contentId) {
        return this.unlockManager.isUnlocked(contentId);
    }

    public boolean hasItem(String itemId) {
        return this.inventoryManager.has(itemId);
    }

    public int getItemQuantity(String itemId) {
        return this.inventoryManager.getQuantity(itemId);
    }

    boolean addCurrency(int amount) {
        return this.currencyBalance.add(amount);
    }

    boolean deductCurrency(int amount) {
        return this.currencyBalance.deduct(amount);
    }

    boolean unlock(String contentId) {
        return this.unlockManager.unlock(contentId);
    }

    boolean addItem(String itemId, int quantity) {
        return this.inventoryManager.add(itemId, quantity);
    }

    boolean removeItem(String itemId, int quantity) {
        return this.inventoryManager.remove(itemId, quantity);
    }

    /** Returns an immutable snapshot independent of subsequent asset changes. */
    public PlayerAssetsState snapshot() {
        return new PlayerAssetsState(getBalance(),
                this.unlockManager.copyUnlockedContentIds(),
                this.inventoryManager.copyItemQuantities());
    }

    /**
     * Replaces all assets after the entire snapshot has been validated.
     * Returns false for null or rejected state, leaving current assets unchanged.
     */
    public boolean restore(PlayerAssetsState state) {
        if (state == null) {
            return false;
        }

        CurrencyBalance restoredBalance;
        try {
            restoredBalance = new CurrencyBalance(state.getBalance());
        } catch (IllegalArgumentException exception) {
            return false;
        }
        UnlockManager restoredUnlocks = new UnlockManager();
        for (String contentId : state.getUnlockedContentIds()) {
            if (!restoredUnlocks.unlock(contentId)) {
                return false;
            }
        }
        InventoryManager restoredInventory = new InventoryManager();
        for (Map.Entry<String, Integer> item : state.getItemQuantities().entrySet()) {
            if (!restoredInventory.add(item.getKey(), item.getValue())) {
                return false;
            }
        }

        this.currencyBalance = restoredBalance;
        this.unlockManager = restoredUnlocks;
        this.inventoryManager = restoredInventory;
        return true;
    }
}
