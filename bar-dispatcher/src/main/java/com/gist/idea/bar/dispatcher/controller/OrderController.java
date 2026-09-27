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
import org.springframework.web.context.request.async.DeferredResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.gist.idea.bar.dispatcher.controller.DeferredResultHelper.fromFuture;
import static com.gist.idea.bar.dispatcher.controller.DeferredResultHelper.immediate;

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
    public DeferredResult<ResponseEntity<?>> handleIntent(@Valid @RequestBody IntentRequest request) {
        log.info("[Spike Entry] Ingested message: '{}'", request.message());

        MultiIntentResult result = intentClassifier.classify(request.message());

        if (!result.hasIntents()) {
            log.warn("Jev detected no actionable intent for message: '{}'", request.message());
            return immediate(ResponseEntity.badRequest().body("Could not understand your request."));
        }

        Set<IntentEnum> intents = result.detectedIntents();
        log.info("[Spike Decision] Detected intents: {}", intents);

        // --- CASE 1: AWAIT READY (Long-poll intent) ---
        if (intents.contains(IntentEnum.AWAIT_READY)) {
            if (request.correlationId() == null) {
                return immediate(ResponseEntity.badRequest().body("Correlation ID is required to await order completion."));
            }
            UUID correlationId = request.correlationId();
            log.info("[Spike Long-Poll] Client awaiting completion for correlationId: {}", correlationId);

            CompletableFuture<OrderReadyEvent> readyFuture = responseTracker.registerReadyWait(correlationId);
            return fromFuture(readyFuture, 32000L, "Order is still in preparation. Please retry or check status.");
        }

        // --- CASE 2: STATUS QUERY ---
        if (intents.contains(IntentEnum.CHECK_STATUS)) {
            if (request.correlationId() == null) {
                return immediate(ResponseEntity.badRequest().body("Correlation ID is required to check order status."));
            }
            UUID correlationId = request.correlationId();
            CompletableFuture<OrderStatusReportedEvent> statusFuture = responseTracker.registerStatusWait(correlationId);

            var queryEvent = new CheckStatusIntentEvent(correlationId);
            rabbitTemplate.convertAndSend(AmqpTopology.BAR_EXCHANGE, AmqpTopology.ROUTING_INTENT_CHECK_STATUS, queryEvent);

            return fromFuture(statusFuture, 6000L, "Order status query timed out.");
        }

        // --- CASE 3: PAY BILL ---
        if (intents.contains(IntentEnum.PAY_BILL)) {
            if (request.correlationId() == null) {
                return immediate(ResponseEntity.badRequest().body("Correlation ID is required to pay the bill."));
            }
            UUID correlationId = request.correlationId();
            CompletableFuture<ReceiptIssuedEvent> receiptFuture = responseTracker.registerReceiptWait(correlationId);

            var payIntent = new PayBillIntentEvent(correlationId, "STANDARD");
            rabbitTemplate.convertAndSend(AmqpTopology.BAR_EXCHANGE, AmqpTopology.ROUTING_INTENT_PAY_BILL, payIntent);

            return fromFuture(receiptFuture, 6000L, "Payment processing timed out.");
        }

        // --- CASE 4: ORDER CREATION (Drink, Food, or Both) ---
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

        return immediate(ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new OrderAcceptedResponse(
                        correlationId,
                        "RECEIVED",
                        dispatchedIntents,
                        Instant.now()
                )));
    }
}
