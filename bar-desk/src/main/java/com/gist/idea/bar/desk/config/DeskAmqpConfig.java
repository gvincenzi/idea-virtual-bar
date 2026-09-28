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

    // Binding 1: drinkReady, foodReady, itemFailed
    @Bean
    public Binding deskWorkerEventsBinding(Queue deskEventsQueue, TopicExchange barExchange) {
        return BindingBuilder.bind(deskEventsQueue)
                .to(barExchange)
                .with(AmqpTopology.ROUTING_PATTERN_ALL_EVENTS);
    }

    // Binding 2: intent.orderDrink, intent.orderFood
    @Bean
    public Binding deskOrderIntentsBinding(Queue deskEventsQueue, TopicExchange barExchange) {
        return BindingBuilder.bind(deskEventsQueue)
                .to(barExchange)
                .with(AmqpTopology.ROUTING_PATTERN_ALL_ORDERS);
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
