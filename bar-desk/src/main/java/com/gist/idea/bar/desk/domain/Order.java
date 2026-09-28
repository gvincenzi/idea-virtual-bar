package com.gist.idea.bar.desk.domain;

import com.gist.idea.bar.common.model.IntentEnum;
import com.gist.idea.bar.common.model.ItemState;
import com.gist.idea.bar.common.model.OrderStatus;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Order aggregate tracking intent lifecycles.
 * Items are indexed by composite key: IntentEnum.name() + ":" + correlationId.
 */
public class Order {

    private final UUID correlationId;
    private final Map<String, ItemState> items = new ConcurrentHashMap<>();
    private OrderStatus status;

    public Order(UUID correlationId) {
        this.correlationId = correlationId;
        this.status = OrderStatus.RECEIVED;
    }

    private String buildKey(IntentEnum intent) {
        return intent.name() + ":" + correlationId;
    }

    public synchronized void recordItemOrdered(IntentEnum intent) {
        String key = buildKey(intent);
        items.compute(key, (k, currentState) -> {
            if (currentState == null) {
                return ItemState.ORDERED;
            }
            return currentState; // Monotonic: does not regress if READY arrived early
        });
        this.status = checkOrderReady(this.items);
    }

    public synchronized void recordItemReady(IntentEnum intent) {
        String key = buildKey(intent);
        items.compute(key, (k, currentState) -> {
            if (currentState == null || currentState.canTransitionTo(ItemState.READY)) {
                return ItemState.READY;
            }
            return currentState;
        });
        this.status = checkOrderReady(this.items);
    }

    public synchronized void recordItemFailed(IntentEnum intent) {
        String key = buildKey(intent);
        items.compute(key, (k, currentState) -> {
            if (currentState == null || currentState.canTransitionTo(ItemState.FAILED)) {
                return ItemState.FAILED;
            }
            return currentState;
        });
        this.status = checkOrderReady(this.items);
    }

    private static OrderStatus checkOrderReady(Map<String, ItemState> items) {
        if (items.isEmpty()) return OrderStatus.RECEIVED;
        for (Entry<String, ItemState> entry : items.entrySet()) {
            if (entry.getValue() == ItemState.FAILED) return OrderStatus.FAILED;
            if (entry.getValue() != ItemState.READY) return OrderStatus.IN_PROGRESS;
        }
        return OrderStatus.READY;
    }

    public UUID getCorrelationId() { return correlationId; }
    public Map<String, ItemState> getItems() { return Collections.unmodifiableMap(items); }
    public OrderStatus getStatus() { return status; }
}
