package com.paklog.wes.pick.domain.shared;

import java.time.Instant;

/**
 * Base class for all domain events in Pick Execution bounded context
 * Copied from paklog-domain to eliminate compilation dependency
 */
public abstract class DomainEvent {

    private final Instant occurredOn;

    protected DomainEvent() {
        this.occurredOn = Instant.now();
    }

    /**
     * When the event occurred
     */
    public Instant occurredOn() {
        return occurredOn;
    }

    /**
     * Type of the event
     */
    public abstract String eventType();
}
