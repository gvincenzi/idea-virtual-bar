package com.gist.idea.bar.common.event;

import com.gist.idea.bar.common.model.ItemState;
import com.gist.idea.bar.common.model.OrderStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OrderStatusReportedEvent(
    UUID correlationId,
    OrderStatus status,
    Map<String, ItemState> items,
    double totalAmount,
    Instant timestamp
) implements DomainEvent {
    public OrderStatusReportedEvent(UUID correlationId, OrderStatus status, Map<String, ItemState> items, double totalAmount) {
        this(correlationId, status, items, totalAmount, Instant.now());
    }

    @Override
    public String intentName() {
        return "orderStatusReported";
    }
}
