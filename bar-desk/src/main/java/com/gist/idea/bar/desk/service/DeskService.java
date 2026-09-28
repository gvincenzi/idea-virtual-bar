package com.gist.idea.bar.desk.service;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import com.gist.idea.bar.common.event.OrderFailedEvent;
import com.gist.idea.bar.common.event.OrderReadyEvent;
import com.gist.idea.bar.common.event.OrderStatusReportedEvent;
import com.gist.idea.bar.common.model.IntentEnum;
import com.gist.idea.bar.common.model.OrderStatus;
import com.gist.idea.bar.desk.domain.Order;
import com.gist.idea.bar.desk.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Business service orchestrating order lifecycle tracking and query handling.
 */
@Service
public class DeskService {

    private static final Logger log = LoggerFactory.getLogger(DeskService.class);

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    public DeskService(OrderRepository orderRepository, RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    // --- Ingestion Intents ---
    public void processDrinkOrdered(UUID correlationId) {
        log.info("[Desk] Registering ordered drink [correlationId: {}]", correlationId);
        Order order = orderRepository.findOrCreate(correlationId);
        order.recordItemOrdered(IntentEnum.ORDER_DRINK);
    }

    public void processFoodOrdered(UUID correlationId) {
        log.info("[Desk] Registering ordered food [correlationId: {}]", correlationId);
        Order order = orderRepository.findOrCreate(correlationId);
        order.recordItemOrdered(IntentEnum.ORDER_FOOD);
    }

    // --- Worker Completions ---
    public void processDrinkReady(UUID correlationId) {
        log.info("[Desk] Recording Drink Ready [correlationId: {}]", correlationId);
        Order order = orderRepository.findOrCreate(correlationId);
        order.recordItemReady(IntentEnum.ORDER_DRINK);
        checkAndEmitOrderReady(order);
    }

    public void processFoodReady(UUID correlationId) {
        log.info("[Desk] Recording Food Ready [correlationId: {}]", correlationId);
        Order order = orderRepository.findOrCreate(correlationId);
        order.recordItemReady(IntentEnum.ORDER_FOOD);
        checkAndEmitOrderReady(order);
    }

    // --- Worker Failure ---
    public void processItemFailed(UUID correlationId, IntentEnum failedIntent, String reason) {
        log.warn("[Desk] Recording Failure for intent '{}' [correlationId: {}]", failedIntent, correlationId);
        Order order = orderRepository.findOrCreate(correlationId);
        order.recordItemFailed(failedIntent);

        var failedEvent = new OrderFailedEvent(correlationId, failedIntent.name(), reason);
        rabbitTemplate.convertAndSend(AmqpTopology.BAR_EXCHANGE, AmqpTopology.ROUTING_EVENT_ORDER_FAILED, failedEvent);
        log.info("[Desk] Emitted OrderFailedEvent for correlationId: {}", correlationId);
    }

    private void checkAndEmitOrderReady(Order order) {
        if (order.getStatus() == OrderStatus.READY) {
            log.info("[Desk] Order is completely READY! Emitting OrderReadyEvent for correlationId: {}", order.getCorrelationId());
            var event = new OrderReadyEvent(order.getCorrelationId());
            rabbitTemplate.convertAndSend(AmqpTopology.BAR_EXCHANGE, AmqpTopology.ROUTING_EVENT_ORDER_READY, event);
        }
    }

    public OrderStatusReportedEvent getOrderStatus(UUID correlationId) {
        log.info("[Desk] Aggregating status for correlationId: {}", correlationId);
        return orderRepository.findById(correlationId)
                .map(order -> new OrderStatusReportedEvent(
                        order.getCorrelationId(),
                        order.getStatus(),
                        order.getItems()
                ))
                .orElseGet(() -> new OrderStatusReportedEvent(
                        correlationId,
                        OrderStatus.NOT_FOUND,
                        Map.of()
                ));
    }
}
