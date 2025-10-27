package com.paklog.wes.pick.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when exception occurs during picking
 * CloudEvent Type: com.paklog.wes.pick-execution.picking.pick-task.exception.v1
 */
public record PickingExceptionEvent(
    @JsonProperty("pick_task_id") String pickTaskId,
    @JsonProperty("task_id") String taskId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("sku_code") String skuCode,
    @JsonProperty("location_id") String locationId,
    @JsonProperty("exception_type") String exceptionType,
    @JsonProperty("requested_quantity") int requestedQuantity,
    @JsonProperty("actual_quantity") int actualQuantity,
    @JsonProperty("reported_at") Instant reportedAt,
    @JsonProperty("reported_by") String reportedBy
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pick-execution.picking.pick-task.exception.v1";
}
