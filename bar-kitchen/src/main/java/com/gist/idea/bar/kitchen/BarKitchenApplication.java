package com.gist.idea.bar.kitchen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Bar-Kitchen worker microservice.
 */
@SpringBootApplication
@EnableRabbit
public class BarKitchenApplication {

    private static final Logger log = LoggerFactory.getLogger(BarKitchenApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BarKitchenApplication.class, args);
        log.info("Bar-Kitchen (Food Worker) successfully started. Listening on q.kitchen.food.");
    }
}
