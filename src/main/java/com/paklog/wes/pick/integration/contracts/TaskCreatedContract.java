package com.paklog.wes.pick.integration.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Contract for TaskCreatedEvent from task-execution-service
 * Anti-Corruption Layer for external events
 */
public record TaskCreatedContract(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("task_type") String taskType,
    @JsonProperty("priority") String priority,
    @JsonProperty("zone_id") String zoneId,
    @JsonProperty("created_at") Instant createdAt
) {
    public static final String EVENT_TYPE = "com.paklog.wes.task-execution.task.task.created.v1";

    /**
     * Check if this is a picking task
     */
    public boolean isPickTask() {
        return "PICK".equalsIgnoreCase(taskType);
    }
}
