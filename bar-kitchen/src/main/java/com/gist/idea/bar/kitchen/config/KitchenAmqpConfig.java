package com.gist.idea.bar.kitchen.config;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AMQP topology configuration for the Kitchen-Service.
 * Binds q.kitchen.food to intent.orderFood on the main topic exchange,
 * and routes unprocessable messages to the Dead Letter Exchange.
 */
@Configuration
public class KitchenAmqpConfig {

    @Bean
    public TopicExchange barExchange() {
        return ExchangeBuilder.topicExchange(AmqpTopology.BAR_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Kitchen queue with DLX routing configured.
     */
    @Bean
    public Queue kitchenFoodQueue() {
        return QueueBuilder.durable(AmqpTopology.QUEUE_KITCHEN_FOOD)
                .withArgument("x-dead-letter-exchange", AmqpTopology.BAR_DEAD_LETTER_EXCHANGE)
                .build();
    }

    /**
     * Binds the queue to only receive food ordering intents.
     */
    @Bean
    public Binding kitchenFoodBinding(Queue kitchenFoodQueue, TopicExchange barExchange) {
        return BindingBuilder.bind(kitchenFoodQueue)
                .to(barExchange)
                .with(AmqpTopology.ROUTING_INTENT_ORDER_FOOD);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
