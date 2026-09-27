package com.gist.idea.bar.desk.repository;

import com.gist.idea.bar.desk.domain.Order;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository indexing order aggregates by Correlation ID.
 */
@Repository
public class OrderRepository {

    private final Map<UUID, Order> storage = new ConcurrentHashMap<>();

    public Order findOrCreate(UUID correlationId) {
        return storage.computeIfAbsent(correlationId, Order::new);
    }

    public Optional<Order> findById(UUID correlationId) {
        return Optional.ofNullable(storage.get(correlationId));
    }
}
