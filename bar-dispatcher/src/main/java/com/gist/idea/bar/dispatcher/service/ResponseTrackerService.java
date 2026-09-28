package com.gist.idea.bar.dispatcher.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import com.gist.idea.bar.common.event.OrderFailedEvent;
import com.gist.idea.bar.common.event.OrderReadyEvent;
import com.gist.idea.bar.common.event.OrderStatusReportedEvent;

/**
 * Service managing asynchronous response correlation between AMQP return events
 * and waiting HTTP request threads using unique Correlation IDs.
 */
@Service
@RabbitListener(queues = AmqpTopology.QUEUE_DISPATCHER_RESP)
public class ResponseTrackerService {

    private static final Logger log = LoggerFactory.getLogger(ResponseTrackerService.class);
    private static final long TIMEOUT_SECONDS = 5, TIMEOUT_ORDER_SECONDS=30;

    private final Map<UUID, CompletableFuture<OrderStatusReportedEvent>> statusWaiters = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<OrderReadyEvent>> readyWaiters = new ConcurrentHashMap<>();

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
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("FAILED/TIMEOUT status wait for correlationId: {} - Error: {}", correlationId, ex.getMessage());
                    } else {
                        log.debug("COMPLETE status wait for correlationId: {}", correlationId);
                    }
                    statusWaiters.remove(correlationId);
                });
    }
    
    /**
     * Registers an expectation for a OrderReadyEvent matching the given correlationId.
     * 
     * @param correlationId unique tracking ID
     * @return CompletableFuture that will complete when the order ready event arrives from AMQP, or timeout
     */
    public CompletableFuture<OrderReadyEvent> registerReadyWait(UUID correlationId) {
        log.debug("Registering await-ready wait for correlationId: {}", correlationId);
        CompletableFuture<OrderReadyEvent> future = new CompletableFuture<>();
        readyWaiters.put(correlationId, future);
        return future.orTimeout(TIMEOUT_ORDER_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("FAILED/TIMEOUT await-ready for correlationId: {} - Error: {}", correlationId, ex.getMessage());
                    } else {
                        log.debug("COMPLETE await-ready for correlationId: {}", correlationId);
                    }
                    readyWaiters.remove(correlationId);
                });
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
     * AMQP listener handler for order ready responses arriving from Desk-Service.
     *
     * @param event the reported status event
     */
    @RabbitHandler
    public void onOrderReady(OrderReadyEvent event) {
        log.info("Received OrderReadyEvent for correlationId: {}", event.correlationId());
        CompletableFuture<OrderReadyEvent> future = readyWaiters.remove(event.correlationId());
        if (future != null) {
            future.complete(event);
        } else {
            log.debug("No active await-ready waiter for correlationId: {}", event.correlationId());
        }
    }
    
    /**
     * AMQP listener handler for order failure events arriving from Desk-Service.
     * Immediately unblocks waiting clients with a failure outcome.
     *
     * @param event the order failed event
     */
    @RabbitHandler
    public void onOrderFailed(OrderFailedEvent event) {
        log.warn("Received OrderFailedEvent for correlationId: {} (reason: '{}')",
                event.correlationId(), event.reason());

        CompletableFuture<OrderReadyEvent> readyFuture = readyWaiters.remove(event.correlationId());
        if (readyFuture != null) {
            readyFuture.completeExceptionally(
                new IllegalStateException("Order processing failed: " + event.reason())
            );
        } else {
            log.debug("No active await-ready waiter for failed correlationId: {}", event.correlationId());
        }
    }


}
