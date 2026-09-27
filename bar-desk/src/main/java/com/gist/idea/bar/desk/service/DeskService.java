package com.gist.idea.bar.desk.service;

import com.gist.idea.bar.common.event.OrderStatusReportedEvent;
import com.gist.idea.bar.common.event.ReceiptIssuedEvent;
import com.gist.idea.bar.common.model.OrderStatus;
import com.gist.idea.bar.desk.domain.Order;
import com.gist.idea.bar.desk.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Business service orchestrating state aggregation, queries and bill settlements.
 */
@Service
public class DeskService {

    private static final double FOOD_PRICE_DEFAULT = 1.80;
	private static final double BEVERAGE_PRICE_DEFAULT = 2.50;
	private static final Logger log = LoggerFactory.getLogger(DeskService.class);

    private final OrderRepository orderRepository;

    public DeskService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void processItemOrdered(UUID correlationId, String item) {
        log.info("[Desk] Registering ordered item: '{}' [correlationId: {}]", item, correlationId);
        Order order = orderRepository.findOrCreate(correlationId);
        order.recordItemOrdered(item);
    }

    public void processDrinkReady(UUID correlationId, String item) {
        log.info("[Desk] Recording Drink Ready: '{}' [correlationId: {}]", item, correlationId);
        Order order = orderRepository.findOrCreate(correlationId);
        order.recordItemReady(item, BEVERAGE_PRICE_DEFAULT);
    }

    public void processFoodReady(UUID correlationId, String item) {
        log.info("[Desk] Recording Food Ready: '{}' [correlationId: {}]", item, correlationId);
        Order order = orderRepository.findOrCreate(correlationId);
        order.recordItemReady(item, FOOD_PRICE_DEFAULT);
    }

    public OrderStatusReportedEvent getOrderStatus(UUID correlationId) {
        log.info("[Desk] Aggregating status for correlationId: {}", correlationId);
        return orderRepository.findById(correlationId)
                .map(order -> new OrderStatusReportedEvent(
                        order.getCorrelationId(),
                        order.getStatus(),
                        order.getItems(),
                        order.getTotalAmount(),
                        order.isPaid()
                ))
                .orElseGet(() -> new OrderStatusReportedEvent(
                        correlationId,
                        OrderStatus.NOT_FOUND,
                        Map.of(),
                        0.0,
                        false
                ));
    }

    public ReceiptIssuedEvent settleBill(UUID correlationId, String paymentMethod) {
        log.info("[Desk] Settling bill for correlationId: {} via {}", correlationId, paymentMethod);
        Order order = orderRepository.findOrCreate(correlationId);
        order.markPaid();
        return new ReceiptIssuedEvent(correlationId, order.getTotalAmount(), true);
    }
}
