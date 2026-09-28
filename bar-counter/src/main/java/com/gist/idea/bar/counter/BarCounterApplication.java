package com.gist.idea.bar.counter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Bar-Counter worker microservice.
 */
@SpringBootApplication
@EnableRabbit
public class BarCounterApplication {

    private static final Logger log = LoggerFactory.getLogger(BarCounterApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BarCounterApplication.class, args);
        log.info("Bar-Counter (Drink Worker) successfully started. Listening on q.counter.drinks.");
    }
}
