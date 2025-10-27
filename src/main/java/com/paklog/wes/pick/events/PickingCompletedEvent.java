package com.paklog.wes.pick.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when all items picked successfully
 * CloudEvent Type: com.paklog.wes.pick-execution.picking.pick-task.completed.v1
 */
public record PickingCompletedEvent(
    @JsonProperty("pick_task_id") String pickTaskId,
    @JsonProperty("task_id") String taskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("completed_at") Instant completedAt,
    @JsonProperty("duration_seconds") long durationSeconds,
    @JsonProperty("items_picked") int itemsPicked,
    @JsonProperty("locations_visited") int locationsVisited,
    @JsonProperty("actual_distance_meters") double actualDistanceMeters,
    @JsonProperty("pick_rate_items_per_hour") int pickRateItemsPerHour
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pick-execution.picking.pick-task.completed.v1";
}
