package com.gist.idea.bar.desk.listener;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import com.gist.idea.bar.common.event.*;
import com.gist.idea.bar.desk.service.DeskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = AmqpTopology.QUEUE_DESK_EVENTS)
public class DeskEventListener {

    private static final Logger log = LoggerFactory.getLogger(DeskEventListener.class);
    private final DeskService deskService;

    public DeskEventListener(DeskService deskService) {
        this.deskService = deskService;
    }

    @RabbitHandler
    public void onOrderDrinkIntent(OrderDrinkIntentEvent event) {
        log.info("[Desk Listener] Drink intent received [correlationId: {}]", event.correlationId());
        deskService.processDrinkOrdered(event.correlationId());
    }

    @RabbitHandler
    public void onOrderFoodIntent(OrderFoodIntentEvent event) {
        log.info("[Desk Listener] Food intent received [correlationId: {}]", event.correlationId());
        deskService.processFoodOrdered(event.correlationId());
    }

    @RabbitHandler
    public void onDrinkReady(DrinkReadyEvent event) {
        log.info("[Desk Listener] Drink ready received [correlationId: {}]", event.correlationId());
        deskService.processDrinkReady(event.correlationId());
    }

    @RabbitHandler
    public void onFoodReady(FoodReadyEvent event) {
        log.info("[Desk Listener] Food ready received [correlationId: {}]", event.correlationId());
        deskService.processFoodReady(event.correlationId());
    }

    @RabbitHandler
    public void onItemFailed(ItemFailedEvent event) {
        log.warn("[Desk Listener] Item failure received for intent '{}' [correlationId: {}]", 
                event.failedIntent(), event.correlationId());
        deskService.processItemFailed(event.correlationId(), event.failedIntent(), event.reason());
    }

    @RabbitHandler(isDefault = true)
    public void onUnknownEvent(Object event) {
        log.debug("[Desk Listener] Safely ignored unhandled event: {}", 
                event != null ? event.getClass().getSimpleName() : "null");
    }
}
