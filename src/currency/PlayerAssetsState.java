package currency;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents an immutable snapshot of the player's persistent asset state.
 */
public final class PlayerAssetsState {

    private final int balance;
    private final Set<String> unlockedContentIds;
    private final Map<String, Integer> itemQuantities;

    /**
     * Constructs a PlayerAssetsState snapshot with defensive copying and invariant validation.
     *
     * @param balance              the non-negative currency balance
     * @param unlockedContentIds   set of unlocked content IDs (non-null, non-blank)
     * @param itemQuantities       map of item IDs to positive quantities (non-null, non-blank keys)
     * @throws IllegalArgumentException if balance < 0, or any ID is invalid, or any quantity <= 0
     */
    public PlayerAssetsState(int balance, Set<String> unlockedContentIds, Map<String, Integer> itemQuantities) {
        if (balance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative.");
        }
        this.balance = balance;

        // Validate and defensively copy unlocked content IDs
        Set<String> copyUnlocks = new HashSet<>();
        if (unlockedContentIds != null) {
            for (String id : unlockedContentIds) {
                if (id == null || id.trim().isEmpty()) {
                    throw new IllegalArgumentException("Unlocked content ID cannot be null or blank.");
                }
                copyUnlocks.add(id);
            }
        }
        this.unlockedContentIds = Collections.unmodifiableSet(copyUnlocks);

        // Validate and defensively copy item quantities
        Map<String, Integer> copyItems = new HashMap<>();
        if (itemQuantities != null) {
            for (Map.Entry<String, Integer> entry : itemQuantities.entrySet()) {
                String itemId = entry.getKey();
                Integer quantity = entry.getValue();

                if (itemId == null || itemId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Item ID cannot be null or blank.");
                }
                if (quantity == null || quantity <= 0) {
                    throw new IllegalArgumentException("Item quantity must be greater than 0.");
                }
                copyItems.put(itemId, quantity);
            }
        }
        this.itemQuantities = Collections.unmodifiableMap(copyItems);
    }

    public int getBalance() {
        return balance;
    }

    public Set<String> getUnlockedContentIds() {
        return unlockedContentIds;
    }

    public Map<String, Integer> getItemQuantities() {
        return itemQuantities;
    }
}
