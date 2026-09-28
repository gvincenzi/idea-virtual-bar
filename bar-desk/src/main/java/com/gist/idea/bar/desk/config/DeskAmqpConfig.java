package com.gist.idea.bar.desk.config;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AMQP topology configuration for the Desk-Service.
 */
@Configuration
public class DeskAmqpConfig {

    @Bean
    public TopicExchange barExchange() {
        return ExchangeBuilder.topicExchange(AmqpTopology.BAR_EXCHANGE)
                .durable(true)
                .build();
    }

    // --- 1. Lifecycle Events Queue ---
    @Bean
    public Queue deskEventsQueue() {
        return QueueBuilder.durable(AmqpTopology.QUEUE_DESK_EVENTS).build();
    }

    // Binding Drink Ready
    @Bean
    public Binding deskDrinkReadyBinding(Queue deskEventsQueue, TopicExchange barExchange) {
        return BindingBuilder.bind(deskEventsQueue)
                .to(barExchange)
                .with(AmqpTopology.ROUTING_EVENT_DRINK_READY); // "event.drinkReady"
    }

    // Binding Food Ready
    @Bean
    public Binding deskFoodReadyBinding(Queue deskEventsQueue, TopicExchange barExchange) {
        return BindingBuilder.bind(deskEventsQueue)
                .to(barExchange)
                .with(AmqpTopology.ROUTING_EVENT_FOOD_READY);  // "event.foodReady"
    }

    // Binding Item Failed
    @Bean
    public Binding deskItemFailedBinding(Queue deskEventsQueue, TopicExchange barExchange) {
        return BindingBuilder.bind(deskEventsQueue)
                .to(barExchange)
                .with(AmqpTopology.ROUTING_EVENT_ITEM_FAILED); // "event.itemFailed"
    }


    @Bean
    public Binding deskWorkerFailedEventsBinding(Queue deskEventsQueue, TopicExchange barExchange) {
        return BindingBuilder.bind(deskEventsQueue)
                .to(barExchange)
                .with(AmqpTopology.ROUTING_EVENT_ITEM_FAILED); // "event.itemFailed"
    }

    // Binding : intent.orderDrink, intent.orderFood
    @Bean
    public Binding deskOrderIntentsBinding(Queue deskEventsQueue, TopicExchange barExchange) {
        return BindingBuilder.bind(deskEventsQueue)
                .to(barExchange)
                .with(AmqpTopology.ROUTING_PATTERN_ALL_ORDERS); // "intent.order*"
    }

    // --- 2. Query Queue (intent.checkStatus) ---
    @Bean
    public Queue deskQueriesQueue() {
        return QueueBuilder.durable(AmqpTopology.QUEUE_DESK_QUERIES).build();
    }

    @Bean
    public Binding deskQueriesBinding(Queue deskQueriesQueue, TopicExchange barExchange) {
        return BindingBuilder.bind(deskQueriesQueue)
                .to(barExchange)
                .with(AmqpTopology.ROUTING_INTENT_CHECK_STATUS);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
