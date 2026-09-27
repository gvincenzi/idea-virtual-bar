package com.gist.idea.bar.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted by the Desk-Service when all items of an order reach the READY state.
 */
public record OrderReadyEvent(
    UUID correlationId,
    double totalAmount,
    Instant timestamp
) implements DomainEvent {

    public OrderReadyEvent(UUID correlationId, double totalAmount) {
        this(correlationId, totalAmount, Instant.now());
    }

    @Override
    public String intentName() {
        return "event.orderReady";
    }
}
