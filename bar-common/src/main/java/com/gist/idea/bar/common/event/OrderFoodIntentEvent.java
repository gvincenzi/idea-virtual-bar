package com.gist.idea.bar.common.event;

import java.time.Instant;
import java.util.UUID;

public record OrderFoodIntentEvent(
    UUID correlationId,
    String item,
    Instant timestamp
) implements DomainEvent {
    public OrderFoodIntentEvent(UUID correlationId, String item) {
        this(correlationId, item, Instant.now());
    }

    @Override
    public String intentName() {
        return "intent.orderFood";
    }
}
