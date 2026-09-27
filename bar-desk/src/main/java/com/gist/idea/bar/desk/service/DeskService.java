package com.gist.idea.bar.desk.service;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import com.gist.idea.bar.common.event.OrderReadyEvent;
import com.gist.idea.bar.common.event.OrderStatusReportedEvent;
import com.gist.idea.bar.common.model.OrderStatus;
import com.gist.idea.bar.desk.domain.Order;
import com.gist.idea.bar.desk.repository.OrderRepository;

/**
 * Business service orchestrating state aggregation, queries and bill settlements.
 */
@Service
public class DeskService {

    private static final double FOOD_PRICE_DEFAULT = 1.80;
	private static final double BEVERAGE_PRICE_DEFAULT = 2.50;
	private static final Logger log = LoggerFactory.getLogger(DeskService.class);

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    public DeskService(OrderRepository orderRepository, RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
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
        checkAndEmitOrderReady(order);
    }

    public void processFoodReady(UUID correlationId, String item) {
        log.info("[Desk] Recording Food Ready: '{}' [correlationId: {}]", item, correlationId);
        Order order = orderRepository.findOrCreate(correlationId);
        order.recordItemReady(item, FOOD_PRICE_DEFAULT);
        checkAndEmitOrderReady(order);
    }
    
    private void checkAndEmitOrderReady(Order order) {
        if (order.getStatus() == OrderStatus.READY) {
            log.info("[Desk] Order is completely READY! Emitting OrderReadyEvent for correlationId: {}", order.getCorrelationId());
            var event = new OrderReadyEvent(order.getCorrelationId(), order.getTotalAmount());
            rabbitTemplate.convertAndSend(AmqpTopology.BAR_EXCHANGE, AmqpTopology.ROUTING_EVENT_ORDER_READY, event);
        }
    }

    public OrderStatusReportedEvent getOrderStatus(UUID correlationId) {
        log.info("[Desk] Aggregating status for correlationId: {}", correlationId);
        return orderRepository.findById(correlationId)
                .map(order -> new OrderStatusReportedEvent(
                        order.getCorrelationId(),
                        order.getStatus(),
                        order.getItems(),
                        order.getTotalAmount()
                ))
                .orElseGet(() -> new OrderStatusReportedEvent(
                        correlationId,
                        OrderStatus.NOT_FOUND,
                        Map.of(),
                        0.0
                ));
    }
}
