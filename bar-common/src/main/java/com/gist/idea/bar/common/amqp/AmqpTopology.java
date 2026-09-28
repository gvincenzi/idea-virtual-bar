package com.gist.idea.bar.common.amqp;

/**
 * Central AMQP topology definitions for the Virtual Bar molecule.
 * 
 * Declares exchange names, queue names, publisher routing keys,
 * and consumer wildcard patterns to eliminate magic strings across microservices.
 */
public final class AmqpTopology {

    private AmqpTopology() {
        // Prevent instantiation
    }

    // ==========================================
    // EXCHANGES
    // ==========================================

    /**
     * Primary topic exchange connecting all services in the molecule.
     */
    public static final String BAR_EXCHANGE = "bar.exchange";

    /**
     * Dead letter exchange for unroutable or repeatedly failing messages.
     */
    public static final String BAR_DEAD_LETTER_EXCHANGE = "bar.dlx";

    // ==========================================
    // ROUTING KEYS: INTENTS (Published by Dispatcher)
    // ==========================================

    public static final String ROUTING_INTENT_ORDER_DRINK  = "intent.orderDrink";
    public static final String ROUTING_INTENT_ORDER_FOOD   = "intent.orderFood";
    public static final String ROUTING_INTENT_CHECK_STATUS = "intent.checkStatus";

    // ==========================================
    // ROUTING KEYS: DOMAIN EVENTS (Published by Workers and Desk)
    // ==========================================

    /** Emitted by Counter-Service when a beverage is ready. */
    public static final String ROUTING_EVENT_DRINK_READY    = "event.drinkReady";

    /** Emitted by Kitchen-Service when food is ready. */
    public static final String ROUTING_EVENT_FOOD_READY     = "event.foodReady";

    /** Emitted by any worker when an item fails preparation. */
    public static final String ROUTING_EVENT_ITEM_FAILED    = "event.itemFailed";

    /** Emitted by Desk-Service when all items in an order are READY. */
    public static final String ROUTING_EVENT_ORDER_READY    = "event.orderReady";

    /** Emitted by Desk-Service when an order fails due to an item failure. */
    public static final String ROUTING_EVENT_ORDER_FAILED   = "event.orderFailed";

    /** Emitted by Desk-Service in response to a checkStatus query. */
    public static final String ROUTING_EVENT_STATUS_REPORT  = "event.orderStatusReported";

    // ==========================================
    // WILDCARD PATTERNS (Used for Queue Bindings)
    // ==========================================

    /** Matches all order intents (intent.orderDrink, intent.orderFood). */
    public static final String ROUTING_PATTERN_ALL_ORDERS   = "intent.order*";

    /** Matches all domain completion and failure events (event.*). */
    public static final String ROUTING_PATTERN_ALL_EVENTS   = "event.*";

    // ==========================================
    // QUEUE NAMES
    // ==========================================

    public static final String QUEUE_COUNTER_DRINKS  = "q.counter.drinks";
    public static final String QUEUE_KITCHEN_FOOD    = "q.kitchen.food";
    public static final String QUEUE_DESK_EVENTS     = "q.desk.events";
    public static final String QUEUE_DESK_QUERIES    = "q.desk.queries";
    public static final String QUEUE_DISPATCHER_RESP = "q.dispatcher.responses";
    public static final String QUEUE_DEAD_LETTER     = "q.bar.dead-letter";
}
