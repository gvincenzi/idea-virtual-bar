package com.gist.idea.bar.common.event;

import java.time.Instant;
import java.util.UUID;

public record PayBillIntentEvent(
    UUID correlationId,
    String paymentMethod,
    Instant timestamp
) implements DomainEvent {
    public PayBillIntentEvent(UUID correlationId, String paymentMethod) {
        this(correlationId, paymentMethod, Instant.now());
    }

    @Override
    public String intentName() {
        return "intent.payBill";
    }
}
