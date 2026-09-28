package com.gist.idea.bar.counter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Business service simulating drink preparation times and inventory checks.
 */
@Service
public class DrinkPreparationService {

    private static final Logger log = LoggerFactory.getLogger(DrinkPreparationService.class);
    private static final long PREPARATION_DELAY_MS = 2000;

    /**
     * Simulates preparing a beverage.
     *
     * @param correlationId order lifecycle identifier
     * @param item requested drink
     * @throws IllegalArgumentException if the requested drink is unavailable (simulates partial failure)
     */
    public void prepare(UUID correlationId, String item) throws InterruptedException {
        log.info("[Counter Service] Starting preparation of '{}' [correlationId: {}]", item, correlationId);

        // Simulated partial failure scenario for architectural testing
        if (item != null && item.toLowerCase().contains("champagne")) {
            throw new IllegalArgumentException("Beverage out of stock: champagne is currently unavailable.");
        }

        // Simulate physical brewing / preparation delay
        Thread.sleep(PREPARATION_DELAY_MS);

        log.info("[Counter Service] Completed preparation of '{}' [correlationId: {}]", item, correlationId);
    }
}
