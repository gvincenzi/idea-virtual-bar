package com.gist.idea.bar.common.model;

/**
 * Lifecycle states of an individual order item.
 * Implements monotonic state transition guards to prevent regressions caused by out-of-order events.
 */
public enum ItemState {
    ORDERED,
    PREPARING,
    READY,
    FAILED;

    /**
     * Guard verifying if a state transition is legal.
     * Prevents out-of-order events from regressing terminal or advanced states.
     *
     * @param targetState the candidate next state
     * @return true if the transition is monotonically valid
     */
    public boolean canTransitionTo(ItemState targetState) {
        if (this == targetState) {
            return true; // Idempotent self-transition
        }
        return switch (this) {
            case ORDERED -> true; // Can transition to any subsequent state
            case PREPARING -> targetState == READY || targetState == FAILED;
            case FAILED -> targetState == READY; // Allows retry/self-healing
            case READY -> false; // Terminal: cannot regress to ORDERED, PREPARING, or FAILED
        };
    }
}
