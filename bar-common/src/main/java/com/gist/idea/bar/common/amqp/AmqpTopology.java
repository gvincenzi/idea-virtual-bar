package com.gist.idea.bar.common.amqp;

public final class AmqpTopology {

    private AmqpTopology() {
        // Prevent instantiation
    }

    // Exchange
    public static final String BAR_EXCHANGE = "bar.exchange";

    // Routing Keys (Intents)
    public static final String ROUTING_INTENT_ORDER_DRINK  = "intent.orderDrink";
    public static final String ROUTING_INTENT_ORDER_FOOD   = "intent.orderFood";
    public static final String ROUTING_INTENT_CHECK_STATUS = "intent.checkStatus";

    // Routing Keys (Domain Events)
    public static final String ROUTING_EVENT_DRINK_READY    = "event.drinkReady";
    public static final String ROUTING_EVENT_FOOD_READY     = "event.foodReady";
    public static final String ROUTING_EVENT_ORDER_READY    = "event.orderReady";
    public static final String ROUTING_EVENT_STATUS_REPORT  = "event.orderStatusReported";

    // Pattern jolly for binding (ex. Desk listens drinkReady and foodReady and drink or food ordered)
    public static final String ROUTING_PATTERN_ALL_READY  = "event.*Ready";
    public static final String ROUTING_PATTERN_ALL_ORDERS = "intent.order*";

    // Queue names
    public static final String QUEUE_COUNTER_DRINKS  = "q.counter.drinks";
    public static final String QUEUE_KITCHEN_FOOD    = "q.kitchen.food";
    public static final String QUEUE_DESK_EVENTS     = "q.desk.events";
    public static final String QUEUE_DESK_QUERIES    = "q.desk.queries";
    public static final String QUEUE_DISPATCHER_RESP = "q.dispatcher.responses";
}

