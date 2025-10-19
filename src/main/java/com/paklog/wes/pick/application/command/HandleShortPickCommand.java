package com.paklog.wes.pick.application.command;

import java.util.Objects;

/**
 * Command to handle a short pick
 */
public record HandleShortPickCommand(
        String sessionId,
        String instructionId,
        int actualQuantity,
        String reason
) {
    public HandleShortPickCommand {
        Objects.requireNonNull(sessionId, "Session ID cannot be null");
        Objects.requireNonNull(instructionId, "Instruction ID cannot be null");
        Objects.requireNonNull(reason, "Reason cannot be null");

        if (actualQuantity < 0) {
            throw new IllegalArgumentException("Actual quantity cannot be negative");
        }
    }
}
