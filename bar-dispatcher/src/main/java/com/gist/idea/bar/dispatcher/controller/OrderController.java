package com.gist.idea.bar.dispatcher.controller;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import com.gist.idea.bar.common.event.*;
import com.gist.idea.bar.common.model.IntentEnum;
import com.gist.idea.bar.dispatcher.dto.IntentRequest;
import com.gist.idea.bar.dispatcher.dto.OrderAcceptedResponse;
import com.gist.idea.bar.dispatcher.service.IntentClassifierService;
import com.gist.idea.bar.dispatcher.service.IntentClassifierService.MultiIntentResult;
import com.gist.idea.bar.dispatcher.service.ResponseTrackerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Single universal entry point (The Spike) for the Intent-Driven Virtual Bar molecule.
 * 
 * Supports compound customer intents (e.g. ordering drink AND food simultaneously)
 * using Jev speculative fan-out in a single evaluation.
 */
@RestController
@RequestMapping("/intent")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final IntentClassifierService intentClassifier;
    private final RabbitTemplate rabbitTemplate;
    private final ResponseTrackerService responseTracker;

    public OrderController(IntentClassifierService intentClassifier,
                           RabbitTemplate rabbitTemplate,
                           ResponseTrackerService responseTracker) {
        this.intentClassifier = intentClassifier;
        this.rabbitTemplate = rabbitTemplate;
        this.responseTracker = responseTracker;
    }

    @PostMapping
    public ResponseEntity<?> handleIntent(@Valid @RequestBody IntentRequest request) {
        log.info("[Spike Entry] Ingested message: '{}'", request.message());

        // 1. Evaluate multiple intents via Jev fan-out
        MultiIntentResult result = intentClassifier.classify(request.message());

        if (!result.hasIntents()) {
            log.warn("Jev detected no actionable intent for message: '{}'", request.message());
            return ResponseEntity.badRequest().body("Could not understand your request. Try asking for coffee, food, status, or bill.");
        }

        Set<IntentEnum> intents = result.detectedIntents();
        log.info("[Spike Decision] Detected intents: {}", intents);

        // --- CASE A: STATUS QUERY ---
        if (intents.contains(IntentEnum.CHECK_STATUS)) {
            if (request.correlationId() == null) {
                return ResponseEntity.badRequest().body("Correlation ID is required to check order status.");
            }
            UUID correlationId = request.correlationId();
            var responseFuture = responseTracker.registerStatusWait(correlationId);
            var queryEvent = new CheckStatusIntentEvent(correlationId);
            rabbitTemplate.convertAndSend(AmqpTopology.BAR_EXCHANGE, AmqpTopology.ROUTING_INTENT_CHECK_STATUS, queryEvent);

            try {
                OrderStatusReportedEvent statusReport = responseFuture.get();
                return ResponseEntity.ok(statusReport);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Timeout checking status for correlationId: {}", correlationId);
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Order status query timed out.");
            }
        }

        // --- CASE B: PAY BILL ---
        if (intents.contains(IntentEnum.PAY_BILL)) {
            if (request.correlationId() == null) {
                return ResponseEntity.badRequest().body("Correlation ID is required to pay the bill.");
            }
            UUID correlationId = request.correlationId();
            var responseFuture = responseTracker.registerReceiptWait(correlationId);
            var payIntent = new PayBillIntentEvent(correlationId, "STANDARD");
            rabbitTemplate.convertAndSend(AmqpTopology.BAR_EXCHANGE, AmqpTopology.ROUTING_INTENT_PAY_BILL, payIntent);

            try {
                ReceiptIssuedEvent receipt = responseFuture.get();
                return ResponseEntity.ok(receipt);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Timeout processing payment for correlationId: {}", correlationId);
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Payment processing timed out.");
            }
        }

        // --- CASE C: ORDER CREATION (Can be Drink, Food, or BOTH!) ---
        UUID correlationId = request.correlationId() != null ? request.correlationId() : UUID.randomUUID();
        List<IntentEnum> dispatchedIntents = new ArrayList<>();

        if (intents.contains(IntentEnum.ORDER_DRINK)) {
            var event = new OrderDrinkIntentEvent(correlationId, request.message());
            rabbitTemplate.convertAndSend(AmqpTopology.BAR_EXCHANGE, IntentEnum.ORDER_DRINK.getRoutingKey(), event);
            log.info("Dispatched OrderDrinkIntentEvent [correlationId: {}] to Counter", correlationId);
            dispatchedIntents.add(IntentEnum.ORDER_DRINK);
        }

        if (intents.contains(IntentEnum.ORDER_FOOD)) {
            var event = new OrderFoodIntentEvent(correlationId, request.message());
            rabbitTemplate.convertAndSend(AmqpTopology.BAR_EXCHANGE, IntentEnum.ORDER_FOOD.getRoutingKey(), event);
            log.info("Dispatched OrderFoodIntentEvent [correlationId: {}] to Kitchen", correlationId);
            dispatchedIntents.add(IntentEnum.ORDER_FOOD);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new OrderAcceptedResponse(
                        correlationId,
                        "RECEIVED",
                        dispatchedIntents,
                        Instant.now()
                ));
    }
}
