package com.gist.idea.bar.common.event;

import java.time.Instant;
import java.util.UUID;

public record DrinkReadyEvent(
    UUID correlationId,
    String item,
    Instant timestamp
) implements DomainEvent {
    public DrinkReadyEvent(UUID correlationId, String item) {
        this(correlationId, item, Instant.now());
    }

    @Override
    public String intentName() {
        return "drinkReady";
    }
}
