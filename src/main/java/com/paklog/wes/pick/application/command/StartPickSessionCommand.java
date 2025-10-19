package com.paklog.wes.pick.application.command;

import com.paklog.wes.pick.domain.entity.PickInstruction;
import com.paklog.wes.pick.domain.valueobject.PickStrategy;

import java.util.List;
import java.util.Objects;

/**
 * Command to start a new pick session
 */
public record StartPickSessionCommand(
        String taskId,
        String workerId,
        String warehouseId,
        PickStrategy strategy,
        String cartId,
        List<PickInstruction> instructions
) {
    public StartPickSessionCommand {
        Objects.requireNonNull(taskId, "Task ID cannot be null");
        Objects.requireNonNull(workerId, "Worker ID cannot be null");
        Objects.requireNonNull(warehouseId, "Warehouse ID cannot be null");
        Objects.requireNonNull(strategy, "Strategy cannot be null");
        Objects.requireNonNull(cartId, "Cart ID cannot be null");
        Objects.requireNonNull(instructions, "Instructions cannot be null");

        if (instructions.isEmpty()) {
            throw new IllegalArgumentException("Instructions cannot be empty");
        }
    }
}
