package com.gist.idea.bar.common.model;

import com.gist.idea.bar.common.amqp.AmqpTopology;

import java.util.Arrays;
import java.util.Optional;

/**
 * Universal business intent enumeration for the Virtual Bar molecule.
 * 
 * Maps business intents to their Jev question keys, LavinMQ routing keys,
 * and semantic instructions for intent evaluation.
 */
public enum IntentEnum {

    ORDER_DRINK(
            "order_drink",
            AmqpTopology.ROUTING_INTENT_ORDER_DRINK,
            "Customer wants to order beverages (coffee, espresso, cappuccino, tea, water, juice, soda, etc.)"
    ),

    ORDER_FOOD(
            "order_food",
            AmqpTopology.ROUTING_INTENT_ORDER_FOOD,
            "Customer wants to order food items (croissant, brioche, sandwich, toast, snack, pastry, etc.)"
    ),

    CHECK_STATUS(
            "check_status",
            AmqpTopology.ROUTING_INTENT_CHECK_STATUS,
            "Customer is asking about order progress, waiting time, or whether items are ready"
    ),

    PAY_BILL(
            "pay_bill",
            AmqpTopology.ROUTING_INTENT_PAY_BILL,
            "Customer wants the bill, check, receipt, or wants to pay and settle the account"
    );

    private final String jevKey;
    private final String routingKey;
    private final String description;

    IntentEnum(String jevKey, String routingKey, String description) {
        this.jevKey = jevKey;
        this.routingKey = routingKey;
        this.description = description;
    }

    public String getJevKey() {
        return jevKey;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Resolves an IntentEnum by its Jev question identifier.
     *
     * @param key the string key returned by Jev
     * @return Optional containing the matching IntentEnum, or empty if unknown
     */
    public static Optional<IntentEnum> fromJevKey(String key) {
        return Arrays.stream(values())
                .filter(intent -> intent.jevKey.equalsIgnoreCase(key))
                .findFirst();
    }
}
