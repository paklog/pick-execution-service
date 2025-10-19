package com.paklog.wes.pick.domain.event;

import com.paklog.domain.shared.DomainEvent;
import com.paklog.wes.pick.domain.valueobject.PickStrategy;

/**
 * Domain event published when a pick session starts
 */
public class PickSessionStartedEvent extends DomainEvent {

    private final String sessionId;
    private final String taskId;
    private final String workerId;
    private final String warehouseId;
    private final PickStrategy strategy;
    private final String cartId;
    private final int totalInstructions;

    public PickSessionStartedEvent(
            String sessionId,
            String taskId,
            String workerId,
            String warehouseId,
            PickStrategy strategy,
            String cartId,
            int totalInstructions
    ) {
        super();
        this.sessionId = sessionId;
        this.taskId = taskId;
        this.workerId = workerId;
        this.warehouseId = warehouseId;
        this.strategy = strategy;
        this.cartId = cartId;
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

    public PickStrategy getStrategy() {
        return strategy;
    }

    public String getCartId() {
        return cartId;
    }

    public int getTotalInstructions() {
        return totalInstructions;
    }
}
