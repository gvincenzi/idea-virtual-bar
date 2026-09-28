package com.gist.idea.bar.counter.listener;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import com.gist.idea.bar.common.event.DrinkReadyEvent;
import com.gist.idea.bar.common.event.ItemFailedEvent;
import com.gist.idea.bar.common.event.OrderDrinkIntentEvent;
import com.gist.idea.bar.counter.service.DrinkPreparationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * AMQP listener consuming drink order intents and dispatching outcome domain events.
 */
@Component
@RabbitListener(queues = AmqpTopology.QUEUE_COUNTER_DRINKS)
public class OrderDrinkListener {

    private static final Logger log = LoggerFactory.getLogger(OrderDrinkListener.class);
    private static final String WORKER_NAME = "COUNTER";

    private final DrinkPreparationService preparationService;
    private final RabbitTemplate rabbitTemplate;

    public OrderDrinkListener(DrinkPreparationService preparationService, RabbitTemplate rabbitTemplate) {
        this.preparationService = preparationService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitHandler
    public void handleOrderDrink(OrderDrinkIntentEvent event) {
        log.info("[Counter Listener] Ingested orderDrink intent: '{}' [correlationId: {}]",
                event.item(), event.correlationId());

        try {
            preparationService.prepare(event.correlationId(), event.item());

            // Success outcome: publish DrinkReadyEvent
            var readyEvent = new DrinkReadyEvent(event.correlationId(), event.item());
            rabbitTemplate.convertAndSend(
                    AmqpTopology.BAR_EXCHANGE,
                    AmqpTopology.ROUTING_EVENT_DRINK_READY,
                    readyEvent
            );
            log.info("[Counter Listener] Published DrinkReadyEvent for '{}' [correlationId: {}]",
                    event.item(), event.correlationId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Counter Listener] Thread interrupted during preparation of '{}'", event.item());
        } catch (Exception ex) {
            // Partial failure outcome: publish ItemFailedEvent to trigger reactive failure handling
            log.warn("[Counter Listener] Preparation failed for '{}': {}", event.item(), ex.getMessage());
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
            log.info("[Counter Listener] Published ItemFailedEvent for '{}' [correlationId: {}]",
                    event.item(), event.correlationId());
        }
    }
}
