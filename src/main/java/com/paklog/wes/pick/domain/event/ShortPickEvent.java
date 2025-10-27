package com.paklog.wes.pick.domain.event;

import com.paklog.wes.pick.domain.shared.DomainEvent;
import com.paklog.wes.pick.domain.valueobject.Location;

/**
 * Domain event published when a short pick occurs
 */
public class ShortPickEvent extends DomainEvent {

    private final String sessionId;
    private final String instructionId;
    private final String itemSku;
    private final int expectedQuantity;
    private final int actualQuantity;
    private final Location location;
    private final String reason;
    private final String workerId;

    public ShortPickEvent(
            String sessionId,
            String instructionId,
            String itemSku,
            int expectedQuantity,
            int actualQuantity,
            Location location,
            String reason,
            String workerId
    ) {
        super();
        this.sessionId = sessionId;
        this.instructionId = instructionId;
        this.itemSku = itemSku;
        this.expectedQuantity = expectedQuantity;
        this.actualQuantity = actualQuantity;
        this.location = location;
        this.reason = reason;
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

    public int getExpectedQuantity() {
        return expectedQuantity;
    }

    public int getActualQuantity() {
        return actualQuantity;
    }

    public int getShortageQuantity() {
        return expectedQuantity - actualQuantity;
    }

    public Location getLocation() {
        return location;
    }

    public String getReason() {
        return reason;
    }

    public String getWorkerId() {
        return workerId;
    }

    @Override
    public String eventType() {
        return "ShortPickEvent";
    }
}
