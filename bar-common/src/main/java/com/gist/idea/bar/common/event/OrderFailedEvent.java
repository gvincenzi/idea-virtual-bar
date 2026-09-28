package com.gist.idea.bar.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted by Desk-Service when a partial or total failure is registered,
 * terminating the order lifecycle and unblocking waiting clients immediately.
 */
public record OrderFailedEvent(
    UUID correlationId,
    String item,
    String reason,
    Instant timestamp
) implements DomainEvent {

    public OrderFailedEvent(UUID correlationId, String item, String reason) {
        this(correlationId, item, reason, Instant.now());
    }

    @Override
    public String intentName() {
        return "event.orderFailed";
    }
}
