package com.paklog.wes.pick.domain.valueobject;

/**
 * Pick session status
 * Represents the lifecycle state of a picking session
 */
public enum SessionStatus {

    /**
     * Session created, waiting to start
     */
    CREATED,

    /**
     * Session actively being executed
     */
    IN_PROGRESS,

    /**
     * Session temporarily paused
     */
    PAUSED,

    /**
     * All picks completed successfully
     */
    COMPLETED,

    /**
     * Session cancelled before completion
     */
    CANCELLED,

    /**
     * Session failed due to error
     */
    FAILED;

    public boolean isActive() {
        return this == IN_PROGRESS || this == PAUSED;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == FAILED;
    }

    public boolean canTransitionTo(SessionStatus newStatus) {
        return switch (this) {
            case CREATED -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS -> newStatus == PAUSED || newStatus == COMPLETED || newStatus == FAILED;
            case PAUSED -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case COMPLETED, CANCELLED, FAILED -> false; // Terminal states
        };
    }
}
