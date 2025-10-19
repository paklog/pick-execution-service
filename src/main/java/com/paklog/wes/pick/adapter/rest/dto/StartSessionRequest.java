package com.paklog.wes.pick.adapter.rest.dto;

import com.paklog.wes.pick.domain.valueobject.PickStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request to start a new pick session
 */
public record StartSessionRequest(
        @NotBlank(message = "Task ID is required")
        String taskId,

        @NotBlank(message = "Worker ID is required")
        String workerId,

        @NotBlank(message = "Warehouse ID is required")
        String warehouseId,

        @NotNull(message = "Strategy is required")
        PickStrategy strategy,

        @NotBlank(message = "Cart ID is required")
        String cartId,

        @NotEmpty(message = "Instructions are required")
        List<PickInstructionDto> instructions
) {
}
