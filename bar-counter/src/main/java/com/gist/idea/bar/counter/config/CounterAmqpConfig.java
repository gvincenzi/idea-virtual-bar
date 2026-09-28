package com.gist.idea.bar.counter.config;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AMQP topology configuration for the Counter-Service.
 * Binds q.counter.drinks to intent.orderDrink on the main topic exchange,
 * and routes unprocessable messages to the Dead Letter Exchange.
 */
@Configuration
public class CounterAmqpConfig {

    @Bean
    public TopicExchange barExchange() {
        return ExchangeBuilder.topicExchange(AmqpTopology.BAR_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Counter queue with DLX routing configured.
     */
    @Bean
    public Queue counterDrinksQueue() {
        return QueueBuilder.durable(AmqpTopology.QUEUE_COUNTER_DRINKS)
                .withArgument("x-dead-letter-exchange", AmqpTopology.BAR_DEAD_LETTER_EXCHANGE)
                .build();
    }

    /**
     * Binds the queue to only receive drink ordering intents.
     */
    @Bean
    public Binding counterDrinksBinding(Queue counterDrinksQueue, TopicExchange barExchange) {
        return BindingBuilder.bind(counterDrinksQueue)
                .to(barExchange)
                .with(AmqpTopology.ROUTING_INTENT_ORDER_DRINK);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
