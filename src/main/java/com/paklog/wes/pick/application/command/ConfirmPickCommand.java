package com.paklog.wes.pick.application.command;

import java.util.Objects;

/**
 * Command to confirm a pick
 */
public record ConfirmPickCommand(
        String sessionId,
        String instructionId,
        int quantity
) {
    public ConfirmPickCommand {
        Objects.requireNonNull(sessionId, "Session ID cannot be null");
        Objects.requireNonNull(instructionId, "Instruction ID cannot be null");

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
