package com.paklog.wes.pick.domain.valueobject;

/**
 * Status of an individual pick instruction
 */
public enum InstructionStatus {

    /**
     * Instruction not yet attempted
     */
    PENDING,

    /**
     * Currently being picked
     */
    IN_PROGRESS,

    /**
     * Successfully picked full quantity
     */
    PICKED,

    /**
     * Picked less than expected quantity
     */
    SHORT_PICKED,

    /**
     * Instruction skipped by worker
     */
    SKIPPED,

    /**
     * Instruction cancelled
     */
    CANCELLED;

    public boolean isComplete() {
        return this == PICKED || this == SHORT_PICKED || this == SKIPPED || this == CANCELLED;
    }

    public boolean requiresAction() {
        return this == PENDING || this == IN_PROGRESS;
    }
}
