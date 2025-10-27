package com.paklog.wes.pick.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when pick path is optimized using TSP algorithm
 * CloudEvent Type: com.paklog.wes.pick-execution.picking.pick-path.optimized.v1
 */
public record PickPathOptimizedEvent(
    @JsonProperty("pick_task_id") String pickTaskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("original_distance_meters") double originalDistanceMeters,
    @JsonProperty("optimized_distance_meters") double optimizedDistanceMeters,
    @JsonProperty("distance_saved_meters") double distanceSavedMeters,
    @JsonProperty("improvement_percentage") double improvementPercentage,
    @JsonProperty("algorithm_used") String algorithmUsed,
    @JsonProperty("optimization_time_ms") long optimizationTimeMs,
    @JsonProperty("optimized_at") Instant optimizedAt
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pick-execution.picking.pick-path.optimized.v1";
}
