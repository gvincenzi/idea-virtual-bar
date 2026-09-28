package com.gist.idea.bar.kitchen.listener;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import com.gist.idea.bar.common.event.FoodReadyEvent;
import com.gist.idea.bar.common.event.ItemFailedEvent;
import com.gist.idea.bar.common.event.OrderFoodIntentEvent;
import com.gist.idea.bar.kitchen.service.FoodPreparationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * AMQP listener consuming food order intents and dispatching outcome domain events.
 */
@Component
@RabbitListener(queues = AmqpTopology.QUEUE_KITCHEN_FOOD)
public class OrderFoodListener {

    private static final Logger log = LoggerFactory.getLogger(OrderFoodListener.class);
    private static final String WORKER_NAME = "KITCHEN";

    private final FoodPreparationService preparationService;
    private final RabbitTemplate rabbitTemplate;

    public OrderFoodListener(FoodPreparationService preparationService, RabbitTemplate rabbitTemplate) {
        this.preparationService = preparationService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitHandler
    public void handleOrderFood(OrderFoodIntentEvent event) {
        log.info("[Kitchen Listener] Ingested orderFood intent: '{}' [correlationId: {}]",
                event.item(), event.correlationId());

        try {
            preparationService.prepare(event.correlationId(), event.item());

            // Success outcome: publish FoodReadyEvent
            var readyEvent = new FoodReadyEvent(event.correlationId(), event.item());
            rabbitTemplate.convertAndSend(
                    AmqpTopology.BAR_EXCHANGE,
                    AmqpTopology.ROUTING_EVENT_FOOD_READY,
                    readyEvent
            );
            log.info("[Kitchen Listener] Published FoodReadyEvent for '{}' [correlationId: {}]",
                    event.item(), event.correlationId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Kitchen Listener] Thread interrupted during preparation of '{}'", event.item());
        } catch (Exception ex) {
            // Partial failure outcome: publish ItemFailedEvent to trigger reactive failure handling
            log.warn("[Kitchen Listener] Preparation failed for '{}': {}", event.item(), ex.getMessage());
            var failedEvent = new ItemFailedEvent(
                    event.correlationId(),
                    event.item(),
                    ex.getMessage(),
                    WORKER_NAME
            );
            rabbitTemplate.convertAndSend(
                    AmqpTopology.BAR_EXCHANGE,
                    AmqpTopology.ROUTING_EVENT_ITEM_FAILED,
                    failedEvent
            );
            log.info("[Kitchen Listener] Published ItemFailedEvent for '{}' [correlationId: {}]",
                    event.item(), event.correlationId());
        }
    }
}

