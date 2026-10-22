package currency;

/**
 * Connects a consumable to its gameplay effect.
 * Both methods run synchronously and must not change assets or start another use.
 * Return SUCCESS, INVALID_CONTEXT, ALREADY_ACTIVE or EFFECT_REJECTED.
 */
public interface ConsumableActivation {
    /** Checks whether use is allowed without changing the effect or its timer. */
    ItemUseResult validate();

    /**
     * Applies the effect and its timer for a purchased unit or collected field item.
     * SUCCESS commits the effect; a rejection must leave effect state unchanged.
     * Throw unexpected failures rather than reporting a safe rejection.
     */
    ItemUseResult activate();
}
