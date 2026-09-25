package com.gist.idea.bar.common.event;

import java.time.Instant;
import java.util.UUID;

public record CheckStatusIntentEvent(
    UUID correlationId,
    Instant timestamp
) implements DomainEvent {
    public CheckStatusIntentEvent(UUID correlationId) {
        this(correlationId, Instant.now());
    }

    @Override
    public String intentName() {
        return "intent.checkStatus";
    }
}
