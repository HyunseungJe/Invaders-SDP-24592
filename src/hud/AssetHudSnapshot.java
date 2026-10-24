package hud;

/** Immutable HUD data containing only the current spendable Credits. */
public final class AssetHudSnapshot {
    private final int credits;

    public AssetHudSnapshot(int credits) {
        if (credits < 0) throw new IllegalArgumentException("Credits cannot be negative.");
        this.credits = credits;
    }

    /** Rewards still awaiting settlement are not included. */
    public int getCredits() {
        return credits;
    }
}
