package com.gist.idea.bar.desk.listener;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import com.gist.idea.bar.common.event.DrinkReadyEvent;
import com.gist.idea.bar.common.event.FoodReadyEvent;
import com.gist.idea.bar.common.event.OrderDrinkIntentEvent;
import com.gist.idea.bar.common.event.OrderFoodIntentEvent;
import com.gist.idea.bar.desk.service.DeskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens on q.desk.events for completed preparation events from Counter and Kitchen.
 */
@Component
@RabbitListener(queues = AmqpTopology.QUEUE_DESK_EVENTS)
public class DeskEventListener {

    private static final Logger log = LoggerFactory.getLogger(DeskEventListener.class);
    private final DeskService deskService;

    public DeskEventListener(DeskService deskService) {
        this.deskService = deskService;
    }

    // From Binding 1: event.*Ready
    @RabbitHandler
    public void onDrinkReady(DrinkReadyEvent event) {
        log.info("[Desk Listener] Drink ready: '{}' [correlationId: {}]", event.item(), event.correlationId());
        deskService.processDrinkReady(event.correlationId(), event.item());
    }

    // From Binding 1: event.*Ready
    @RabbitHandler
    public void onFoodReady(FoodReadyEvent event) {
        log.info("[Desk Listener] Food ready: '{}' [correlationId: {}]", event.item(), event.correlationId());
        deskService.processFoodReady(event.correlationId(), event.item());
    }
    
    // From Binding 2: intent.order*
    @RabbitHandler
    public void onOrderDrinkIntent(OrderDrinkIntentEvent event) {
        log.info("[Desk Listener] Drink ordered: '{}' [correlationId: {}]", event.item(), event.correlationId());
        deskService.processItemOrdered(event.correlationId(), event.item());
    }

    // From Binding 2: intent.order*
    @RabbitHandler
    public void onOrderFoodIntent(OrderFoodIntentEvent event) {
        log.info("[Desk Listener] Food ordered: '{}' [correlationId: {}]", event.item(), event.correlationId());
        deskService.processItemOrdered(event.correlationId(), event.item());
    }
}
