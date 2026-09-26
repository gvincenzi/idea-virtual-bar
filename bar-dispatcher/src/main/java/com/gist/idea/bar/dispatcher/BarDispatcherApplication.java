package com.gist.idea.bar.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Bar-Dispatcher microservice.
 * 
 * Acting as the Spike (Alpha component) in the Intent-Driven Architecture:
 * - Exposes the single universal entry point for customer requests.
 * - Integrates TypeSafe AI Jev for speculative fan-out intent classification.
 * - Injects unique Correlation IDs across the distributed lifecycle.
 * - Dispatches discrete domain events over LavinMQ (AMQP).
 */
@SpringBootApplication
@EnableRabbit
public class BarDispatcherApplication {

    private static final Logger log = LoggerFactory.getLogger(BarDispatcherApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BarDispatcherApplication.class, args);
        log.info("Bar-Dispatcher (Spike) successfully started. Ready to accept customer intents.");
    }
}
