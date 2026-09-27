package com.gist.idea.bar.desk.domain;

import com.gist.idea.bar.common.model.ItemState;
import com.gist.idea.bar.common.model.OrderStatus;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean tracking the end-to-end lifecycle of an order under a single Correlation ID.
 */
public class Order {

    private final UUID correlationId;
    private final Map<String, ItemState> items = new ConcurrentHashMap<>();
    private OrderStatus status;
    private double totalAmount;

    public Order(UUID correlationId) {
        this.correlationId = correlationId;
        this.status = OrderStatus.RECEIVED;
        this.totalAmount = 0.0;
    }

    /**
     * Called when an intent is dispatched: registers the item as ORDERED.
     */
    public synchronized void recordItemOrdered(String item) {
        this.items.putIfAbsent(item, ItemState.ORDERED);
        this.status = OrderStatus.IN_PROGRESS;
    }

    /**
     * Called when a worker finishes: transitions the item to READY and recalculates total & status.
     */
    public synchronized void recordItemReady(String item, double price) {
        this.items.put(item, ItemState.READY);
        this.totalAmount += price;
        this.status = checkOrderReady(this.items);
    }

    private static OrderStatus checkOrderReady(Map<String, ItemState> items) {
        if (items.isEmpty()) return OrderStatus.RECEIVED;
        for (Entry<String, ItemState> item : items.entrySet()) {
            if (!ItemState.READY.equals(item.getValue())) {
                return OrderStatus.IN_PROGRESS;
            }
        }
        return OrderStatus.READY;
    }

    public synchronized void markPaid() {
        this.status = OrderStatus.PAID;
    }

    public UUID getCorrelationId() { return correlationId; }
    public Map<String, ItemState> getItems() { return Collections.unmodifiableMap(items); }
    public OrderStatus getStatus() { return status; }
    public double getTotalAmount() { return totalAmount; }
    public boolean isPaid() { return OrderStatus.PAID.equals(getStatus()); }
}
