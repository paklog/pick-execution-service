package com.paklog.wes.pick.domain.event;

import com.paklog.wes.pick.domain.shared.DomainEvent;

/**
 * Domain event published when a pick session is cancelled
 */
public class PickSessionCancelledEvent extends DomainEvent {

    private final String sessionId;
    private final String taskId;
    private final String workerId;
    private final String warehouseId;
    private final String reason;
    private final int completedInstructions;
    private final int totalInstructions;

    public PickSessionCancelledEvent(
            String sessionId,
            String taskId,
            String workerId,
            String warehouseId,
            String reason,
            int completedInstructions,
            int totalInstructions
    ) {
        super();
        this.sessionId = sessionId;
        this.taskId = taskId;
        this.workerId = workerId;
        this.warehouseId = warehouseId;
        this.reason = reason;
        this.completedInstructions = completedInstructions;
        this.totalInstructions = totalInstructions;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getReason() {
        return reason;
    }

    public int getCompletedInstructions() {
        return completedInstructions;
    }

    public int getTotalInstructions() {
        return totalInstructions;
    }

    public double getCompletionPercentage() {
        if (totalInstructions == 0) {
            return 0.0;
        }
        return (completedInstructions / (double) totalInstructions) * 100.0;
    }

    @Override
    public String eventType() {
        return "PickSessionCancelledEvent";
    }
}
