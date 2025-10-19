package com.paklog.wes.pick.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request to confirm a pick
 */
public record ConfirmPickRequest(
        @NotBlank(message = "Instruction ID is required")
        String instructionId,

        @Positive(message = "Quantity must be positive")
        int quantity
) {
}
