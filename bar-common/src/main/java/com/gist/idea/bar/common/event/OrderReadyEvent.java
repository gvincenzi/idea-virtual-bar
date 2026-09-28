package com.gist.idea.bar.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted by the Desk-Service when all items of an order reach the READY state.
 */
public record OrderReadyEvent(
    UUID correlationId,
    Instant timestamp
) implements DomainEvent {

    public OrderReadyEvent(UUID correlationId) {
        this(correlationId, Instant.now());
    }

    @Override
    public String intentName() {
        return "event.orderReady";
    }
}
