package com.gist.idea.bar.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted by a worker (Counter or Kitchen) when an item cannot be prepared
 * (e.g., out of ingredients, equipment failure, validation error).
 */
public record ItemFailedEvent(
    UUID correlationId,
    String item,
    String reason,
    String failedBy, // "COUNTER" or "KITCHEN"
    Instant timestamp
) implements DomainEvent {

    public ItemFailedEvent(UUID correlationId, String item, String reason, String failedBy) {
        this(correlationId, item, reason, failedBy, Instant.now());
    }

    @Override
    public String intentName() {
        return "event.itemFailed";
    }
}
