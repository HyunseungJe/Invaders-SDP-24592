package currency;

/**
 * Connects a purchased consumable to its gameplay effect.
 * Both methods run synchronously and must not change assets or start another use.
 * Return SUCCESS, INVALID_CONTEXT, ALREADY_ACTIVE or EFFECT_REJECTED.
 */
public interface ConsumableActivation {
    /** Checks whether use is allowed without changing the effect or its timer. */
    ItemUseResult validate();

    /**
     * Applies the effect and its timer after one unit has been reserved.
     * SUCCESS commits the effect; a rejection must leave effect state unchanged.
     * Throw unexpected failures rather than reporting a safe rejection.
     */
    ItemUseResult activate();
}
