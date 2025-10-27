package com.paklog.wes.pick.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Event published when worker starts picking items
 * CloudEvent Type: com.paklog.wes.pick-execution.picking.pick-task.started.v1
 */
public record PickingStartedEvent(
    @JsonProperty("pick_task_id") String pickTaskId,
    @JsonProperty("task_id") String taskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("location_sequence") List<String> locationSequence,
    @JsonProperty("total_items") int totalItems,
    @JsonProperty("started_at") Instant startedAt,
    @JsonProperty("optimized_path_distance_meters") double optimizedPathDistanceMeters
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pick-execution.picking.pick-task.started.v1";
}
