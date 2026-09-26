package com.gist.idea.bar.dispatcher.service;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import com.gist.idea.bar.common.event.OrderStatusReportedEvent;
import com.gist.idea.bar.common.event.ReceiptIssuedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service managing asynchronous response correlation between AMQP return events
 * and waiting HTTP request threads using unique Correlation IDs.
 */
@Service
@RabbitListener(queues = AmqpTopology.QUEUE_DISPATCHER_RESP)
public class ResponseTrackerService {

    private static final Logger log = LoggerFactory.getLogger(ResponseTrackerService.class);
    private static final long TIMEOUT_SECONDS = 5;

    private final Map<UUID, CompletableFuture<OrderStatusReportedEvent>> statusWaiters = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<ReceiptIssuedEvent>> receiptWaiters = new ConcurrentHashMap<>();

    /**
     * Registers an expectation for an OrderStatusReportedEvent matching the given correlationId.
     *
     * @param correlationId unique tracking ID
     * @return CompletableFuture that will complete when the event arrives from AMQP, or timeout
     */
    public CompletableFuture<OrderStatusReportedEvent> registerStatusWait(UUID correlationId) {
        log.debug("Registering status wait for correlationId: {}", correlationId);
        CompletableFuture<OrderStatusReportedEvent> future = new CompletableFuture<>();
        statusWaiters.put(correlationId, future);
        return future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> statusWaiters.remove(correlationId));
    }

    /**
     * Registers an expectation for a ReceiptIssuedEvent matching the given correlationId.
     *
     * @param correlationId unique tracking ID
     * @return CompletableFuture that will complete when the receipt arrives from AMQP, or timeout
     */
    public CompletableFuture<ReceiptIssuedEvent> registerReceiptWait(UUID correlationId) {
        log.debug("Registering receipt wait for correlationId: {}", correlationId);
        CompletableFuture<ReceiptIssuedEvent> future = new CompletableFuture<>();
        receiptWaiters.put(correlationId, future);
        return future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> receiptWaiters.remove(correlationId));
    }

    /**
     * AMQP listener handler for status aggregation responses arriving from Desk-Service.
     *
     * @param event the reported status event
     */
    @RabbitHandler
    public void onStatusReported(OrderStatusReportedEvent event) {
        log.info("Received OrderStatusReportedEvent for correlationId: {}", event.correlationId());
        CompletableFuture<OrderStatusReportedEvent> future = statusWaiters.remove(event.correlationId());
        if (future != null) {
            future.complete(event);
        } else {
            log.warn("Received unexpected or timed-out status report for correlationId: {}", event.correlationId());
        }
    }

    /**
     * AMQP listener handler for receipt issued responses arriving from Desk-Service.
     *
     * @param event the issued receipt event
     */
    @RabbitHandler
    public void onReceiptIssued(ReceiptIssuedEvent event) {
        log.info("Received ReceiptIssuedEvent for correlationId: {}", event.correlationId());
        CompletableFuture<ReceiptIssuedEvent> future = receiptWaiters.remove(event.correlationId());
        if (future != null) {
            future.complete(event);
        } else {
            log.warn("Received unexpected or timed-out receipt for correlationId: {}", event.correlationId());
        }
    }
}
