package com.gist.idea.bar.desk.listener;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import com.gist.idea.bar.common.event.CheckStatusIntentEvent;
import com.gist.idea.bar.common.event.OrderStatusReportedEvent;
import com.gist.idea.bar.desk.service.DeskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Handles status queries, builds the reported status, and publishes it back to the Dispatcher.
 */
@Component
@RabbitListener(queues = AmqpTopology.QUEUE_DESK_QUERIES)
public class DeskQueryListener {

    private static final Logger log = LoggerFactory.getLogger(DeskQueryListener.class);

    private final DeskService deskService;
    private final RabbitTemplate rabbitTemplate;

    public DeskQueryListener(DeskService deskService, RabbitTemplate rabbitTemplate) {
        this.deskService = deskService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitHandler
    public void onCheckStatus(CheckStatusIntentEvent event) {
        log.info("[Desk Query Listener] Processing status query for correlationId: {}", event.correlationId());
        OrderStatusReportedEvent response = deskService.getOrderStatus(event.correlationId());
        rabbitTemplate.convertAndSend(AmqpTopology.BAR_EXCHANGE, AmqpTopology.ROUTING_EVENT_STATUS_REPORT, response);
        log.info("[Desk Query Listener] Published OrderStatusReportedEvent for correlationId: {}", event.correlationId());
    }
}
