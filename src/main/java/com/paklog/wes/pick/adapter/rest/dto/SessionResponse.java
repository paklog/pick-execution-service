package com.paklog.wes.pick.adapter.rest.dto;

import com.paklog.wes.pick.domain.valueobject.PickStrategy;
import com.paklog.wes.pick.domain.valueobject.SessionStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response with pick session details
 */
public record SessionResponse(
        String sessionId,
        String taskId,
        String workerId,
        String warehouseId,
        PickStrategy strategy,
        SessionStatus status,
        String cartId,
        List<PickInstructionDto> instructions,
        int currentInstructionIndex,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        double progress,
        double accuracy,
        int completedInstructions,
        int totalInstructions,
        int shortPicks
) {
}
