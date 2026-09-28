package com.gist.idea.bar.desk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Bar-Desk microservice (Read Model & Cashier).
 * Acts as the observability aggregator and settlement worker for the molecule.
 */
@SpringBootApplication
@EnableRabbit
public class BarDeskApplication {

    private static final Logger log = LoggerFactory.getLogger(BarDeskApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BarDeskApplication.class, args);
        log.info("Bar-Desk (Read Model) successfully started. Listening for events, queries.");
    }
}
