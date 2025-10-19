package com.paklog.wes.pick.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request to handle a short pick
 */
public record ShortPickRequest(
        @NotBlank(message = "Instruction ID is required")
        String instructionId,

        @PositiveOrZero(message = "Actual quantity cannot be negative")
        int actualQuantity,

        @NotBlank(message = "Reason is required")
        String reason
) {
}
