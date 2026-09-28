package com.gist.idea.bar.desk.domain;

import com.gist.idea.bar.common.model.ItemState;
import com.gist.idea.bar.common.model.OrderStatus;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Order aggregate tracking item lifecycle.
 * Immune to out-of-order event arrivals and duplicate deliveries.
 */
public class Order {

    private final UUID correlationId;
    private final Map<String, ItemState> items = new ConcurrentHashMap<>();
    private OrderStatus status;

    public Order(UUID correlationId) {
        this.correlationId = correlationId;
        this.status = OrderStatus.RECEIVED;
    }

    /**
     * Registers an incoming order intent.
     * If a ready or failed event arrived out-of-order BEFORE this intent,
     * the existing state is preserved and NOT regressed.
     */
    public synchronized void recordItemOrdered(String item) {
        items.compute(item, (key, currentState) -> {
            if (currentState == null) {
                return ItemState.ORDERED;
            }
            // State already exists (e.g. READY arrived early): do NOT regress to ORDERED
            return currentState;
        });
        this.status = checkOrderReady(this.items);
    }

    /**
     * Transitions an item to READY.
     */
    public synchronized void recordItemReady(String item) {
        items.compute(item, (key, currentState) -> {
            if (currentState == null || currentState.canTransitionTo(ItemState.READY)) {
                return ItemState.READY;
            }
            return currentState;
        });
        this.status = checkOrderReady(this.items);
    }

    /**
     * Transitions an item to FAILED.
     */
    public synchronized void recordItemFailed(String item) {
        items.compute(item, (key, currentState) -> {
            if (currentState == null || currentState.canTransitionTo(ItemState.FAILED)) {
                return ItemState.FAILED;
            }
            return currentState;
        });
        this.status = checkOrderReady(this.items);
    }

    private static OrderStatus checkOrderReady(Map<String, ItemState> items) {
        if (items.isEmpty()) return OrderStatus.RECEIVED;

        boolean hasFailures = false;
        boolean allReady = true;

        for (Entry<String, ItemState> entry : items.entrySet()) {
            if (entry.getValue() == ItemState.FAILED) {
                hasFailures = true;
            }
            if (entry.getValue() != ItemState.READY) {
                allReady = false;
            }
        }

        if (hasFailures) return OrderStatus.FAILED;
        if (allReady) return OrderStatus.READY;
        return OrderStatus.IN_PROGRESS;
    }

    public UUID getCorrelationId() { return correlationId; }
    public Map<String, ItemState> getItems() { return Collections.unmodifiableMap(items); }
    public OrderStatus getStatus() { return status; }
}
