package com.gist.idea.bar.common.event;

import com.gist.idea.bar.common.model.IntentEnum;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted by a worker when an item cannot be prepared.
 * Explicitly carries the strongly-typed failedIntent.
 */
public record ItemFailedEvent(
    UUID correlationId,
    String item,
    String reason,
    IntentEnum failedIntent,
    Instant timestamp
) implements DomainEvent {

    public ItemFailedEvent(UUID correlationId, String item, String reason, IntentEnum failedIntent) {
        this(correlationId, item, reason, failedIntent, Instant.now());
    }

    @Override
    public String intentName() {
        return "event.itemFailed";
    }
}
