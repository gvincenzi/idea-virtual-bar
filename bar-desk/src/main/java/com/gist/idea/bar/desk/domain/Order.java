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
        ItemState previousState = this.items.put(item, ItemState.READY);
        
        // Add price only if the item was not already accounted for as READY
        if (previousState != ItemState.READY) {
            this.totalAmount += price;
        }
        
        this.status = checkOrderReady(this.items);
    }
    
    /**
     * Called when a worker reports a failure for an item.
     */
    public synchronized void recordItemFailed(String item) {
        this.items.put(item, ItemState.FAILED);
        this.status = OrderStatus.FAILED;
    }

    private static OrderStatus checkOrderReady(Map<String, ItemState> items) {
        if (items.isEmpty()) return OrderStatus.RECEIVED;
        for (Entry<String, ItemState> item : items.entrySet()) {
            if (ItemState.FAILED.equals(item.getValue())) {
                return OrderStatus.FAILED;
            }
            if (!ItemState.READY.equals(item.getValue())) {
                return OrderStatus.IN_PROGRESS;
            }
        }
        return OrderStatus.READY;
    }

    public UUID getCorrelationId() { return correlationId; }
    public Map<String, ItemState> getItems() { return Collections.unmodifiableMap(items); }
    public OrderStatus getStatus() { return status; }
    public double getTotalAmount() { return totalAmount; }
}
