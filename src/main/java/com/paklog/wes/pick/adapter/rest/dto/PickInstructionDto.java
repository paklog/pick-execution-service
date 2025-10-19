package com.paklog.wes.pick.adapter.rest.dto;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.pick.domain.valueobject.InstructionStatus;
import com.paklog.wes.pick.domain.valueobject.Location;

/**
 * DTO for pick instruction
 */
public record PickInstructionDto(
        String instructionId,
        String itemSku,
        String itemDescription,
        int expectedQuantity,
        int pickedQuantity,
        Location location,
        String orderId,
        InstructionStatus status,
        int sequenceNumber,
        Priority priority
) {
}
