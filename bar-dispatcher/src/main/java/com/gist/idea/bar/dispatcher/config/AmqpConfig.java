package com.gist.idea.bar.dispatcher.config;

import com.gist.idea.bar.common.amqp.AmqpTopology;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AMQP configuration for the Bar-Dispatcher module.
 * 
 * Sets up the primary topic exchange, the dispatcher response queue,
 * binding declarations for return events, and JSON message serialization.
 */
@Configuration
public class AmqpConfig {

    /**
     * Declares the primary durable topic exchange for the entire molecule.
     *
     * @return the configured TopicExchange
     */
    @Bean
    public TopicExchange barExchange() {
        return ExchangeBuilder.topicExchange(AmqpTopology.BAR_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Declares the dedicated response queue for this Dispatcher instance.
     * Used by ResponseTrackerService to capture asynchronous replies from Desk-Service.
     *
     * @return the durable response Queue
     */
    @Bean
    public Queue dispatcherResponseQueue() {
        return QueueBuilder.durable(AmqpTopology.QUEUE_DISPATCHER_RESP)
                .build();
    }

    /**
     * Binds the status report event routing key to the dispatcher response queue.
     *
     * @param dispatcherResponseQueue the response queue bean
     * @param barExchange the topic exchange bean
     * @return the created Binding
     */
    @Bean
    public Binding statusReportBinding(Queue dispatcherResponseQueue, TopicExchange barExchange) {
        return BindingBuilder.bind(dispatcherResponseQueue)
                .to(barExchange)
                .with(AmqpTopology.ROUTING_EVENT_STATUS_REPORT);
    }

    /**
     * Binds the receipt issued event routing key to the dispatcher response queue.
     *
     * @param dispatcherResponseQueue the response queue bean
     * @param barExchange the topic exchange bean
     * @return the created Binding
     */
    @Bean
    public Binding receiptIssuedBinding(Queue dispatcherResponseQueue, TopicExchange barExchange) {
        return BindingBuilder.bind(dispatcherResponseQueue)
                .to(barExchange)
                .with(AmqpTopology.ROUTING_EVENT_RECEIPT_ISSUED);
    }

    /**
     * Configures Jackson JSON converter for transparent serialization
     * and deserialization of Java records across AMQP queues.
     *
     * @return message converter supporting Java records and java.time types
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
