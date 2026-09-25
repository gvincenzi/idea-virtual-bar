package com.gist.idea.bar.common.event;

import java.time.Instant;
import java.util.UUID;

public record ReceiptIssuedEvent(
    UUID correlationId,
    double totalAmount,
    boolean paid,
    Instant timestamp
) implements DomainEvent {
    public ReceiptIssuedEvent(UUID correlationId, double totalAmount, boolean paid) {
        this(correlationId, totalAmount, paid, Instant.now());
    }

    @Override
    public String intentName() {
        return "receiptIssued";
    }
}
