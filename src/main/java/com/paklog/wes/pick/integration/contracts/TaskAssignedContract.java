package com.paklog.wes.pick.integration.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Contract for TaskAssignedEvent from task-execution-service
 * Anti-Corruption Layer for external events
 */
public record TaskAssignedContract(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("assigned_at") Instant assignedAt,
    @JsonProperty("priority") String priority,
    @JsonProperty("zone_id") String zoneId
) {
    public static final String EVENT_TYPE = "com.paklog.wes.task-execution.task.task.assigned.v1";
}
