package com.paklog.wes.pick.domain.event;

import com.paklog.domain.shared.DomainEvent;

import java.time.Duration;

/**
 * Domain event published when a pick session is completed
 */
public class PickSessionCompletedEvent extends DomainEvent {

    private final String sessionId;
    private final String taskId;
    private final String workerId;
    private final String warehouseId;
    private final int totalInstructions;
    private final int completedInstructions;
    private final int shortPicks;
    private final double accuracy;
    private final Duration duration;

    public PickSessionCompletedEvent(
            String sessionId,
            String taskId,
            String workerId,
            String warehouseId,
            int totalInstructions,
            int completedInstructions,
            int shortPicks,
            double accuracy,
            Duration duration
    ) {
        super();
        this.sessionId = sessionId;
        this.taskId = taskId;
        this.workerId = workerId;
        this.warehouseId = warehouseId;
        this.totalInstructions = totalInstructions;
        this.completedInstructions = completedInstructions;
        this.shortPicks = shortPicks;
        this.accuracy = accuracy;
        this.duration = duration;
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

    public int getTotalInstructions() {
        return totalInstructions;
    }

    public int getCompletedInstructions() {
        return completedInstructions;
    }

    public int getShortPicks() {
        return shortPicks;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public Duration getDuration() {
        return duration;
    }

    public double getPicksPerHour() {
        if (duration == null || duration.isZero()) {
            return 0.0;
        }
        double hours = duration.toMinutes() / 60.0;
        return completedInstructions / hours;
    }
}
