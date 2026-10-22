package engine;

/** Last save result for one shared asset state, updated on the gameplay thread. */
public final class AssetSaveState {
    private boolean attempted;
    private boolean failed;

    public boolean hasAttemptedSave() {
        return attempted;
    }

    public boolean hasFailure() {
        return failed;
    }

    public void recordSuccess() {
        attempted = true;
        failed = false;
    }

    public void recordFailure() {
        attempted = true;
        failed = true;
    }
}
