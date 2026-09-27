package com.gist.idea.bar.desk.listener;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import com.gist.idea.bar.common.event.PayBillIntentEvent;
import com.gist.idea.bar.common.event.ReceiptIssuedEvent;
import com.gist.idea.bar.desk.service.DeskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Handles payment intents, marks orders as paid, and publishes the receipt to the Dispatcher.
 */
@Component
@RabbitListener(queues = AmqpTopology.QUEUE_DESK_PAYMENTS)
public class DeskPaymentListener {

    private static final Logger log = LoggerFactory.getLogger(DeskPaymentListener.class);

    private final DeskService deskService;
    private final RabbitTemplate rabbitTemplate;

    public DeskPaymentListener(DeskService deskService, RabbitTemplate rabbitTemplate) {
        this.deskService = deskService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitHandler
    public void onPayBill(PayBillIntentEvent event) {
        log.info("[Desk Payment Listener] Processing payment for correlationId: {}", event.correlationId());
        ReceiptIssuedEvent receipt = deskService.settleBill(event.correlationId(), event.paymentMethod());
        rabbitTemplate.convertAndSend(AmqpTopology.BAR_EXCHANGE, AmqpTopology.ROUTING_EVENT_RECEIPT_ISSUED, receipt);
        log.info("[Desk Payment Listener] Published ReceiptIssuedEvent for correlationId: {}", event.correlationId());
    }
}
