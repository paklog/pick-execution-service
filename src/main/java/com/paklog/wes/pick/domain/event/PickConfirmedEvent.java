package com.paklog.wes.pick.domain.event;

import com.paklog.wes.pick.domain.shared.DomainEvent;
import com.paklog.wes.pick.domain.valueobject.Location;

/**
 * Domain event published when a pick is confirmed
 */
public class PickConfirmedEvent extends DomainEvent {

    private final String sessionId;
    private final String instructionId;
    private final String itemSku;
    private final int quantity;
    private final Location location;
    private final String workerId;

    public PickConfirmedEvent(
            String sessionId,
            String instructionId,
            String itemSku,
            int quantity,
            Location location,
            String workerId
    ) {
        super();
        this.sessionId = sessionId;
        this.instructionId = instructionId;
        this.itemSku = itemSku;
        this.quantity = quantity;
        this.location = location;
        this.workerId = workerId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getInstructionId() {
        return instructionId;
    }

    public String getItemSku() {
        return itemSku;
    }

    public int getQuantity() {
        return quantity;
    }

    public Location getLocation() {
        return location;
    }

    public String getWorkerId() {
        return workerId;
    }

    @Override
    public String eventType() {
        return "PickConfirmedEvent";
    }
}
