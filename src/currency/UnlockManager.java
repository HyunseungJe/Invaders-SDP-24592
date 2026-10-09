package currency;

import java.util.HashSet;
import java.util.Set;

public class UnlockManager {
    private final Set<String> unlockedContentIds;

    public UnlockManager() {
        this.unlockedContentIds = new HashSet<>();
    }

    private boolean isInvalidId(String id) {
        return id == null || id.trim().isEmpty();
    }

    public boolean isUnlocked(String contentId) {
        if (isInvalidId(contentId)) {
            return false;
        }
        return this.unlockedContentIds.contains(contentId);
    }

    public boolean unlock(String contentId) {
        if (isInvalidId(contentId)) {
            return false;
        }
        if (this.unlockedContentIds.contains(contentId)) {
            return false;
        }
        this.unlockedContentIds.add(contentId);
        return true;
    }

    /** Returns a detached copy for asset snapshots within this package. */
    Set<String> copyUnlockedContentIds() {
        return new HashSet<>(this.unlockedContentIds);
    }
}
