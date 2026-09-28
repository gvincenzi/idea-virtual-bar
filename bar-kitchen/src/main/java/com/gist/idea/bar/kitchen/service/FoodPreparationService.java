package com.gist.idea.bar.kitchen.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Business service simulating food preparation times and inventory checks.
 */
@Service
public class FoodPreparationService {

    private static final Logger log = LoggerFactory.getLogger(FoodPreparationService.class);
    private static final long PREPARATION_DELAY_MS = 3000;

    /**
     * Simulates preparing or baking a food item.
     *
     * @param correlationId order lifecycle identifier
     * @param item requested food item
     * @throws IllegalArgumentException if the requested food is unavailable (simulates partial failure)
     */
    public void prepare(UUID correlationId, String item) throws InterruptedException {
        log.info("[Kitchen Service] Starting preparation of '{}' [correlationId: {}]", item, correlationId);

        // Simulated partial failure scenario for architectural testing
        if (item != null && (item.toLowerCase().contains("caviale") || item.toLowerCase().contains("caviar"))) {
            throw new IllegalArgumentException("Food out of stock: caviar is currently unavailable.");
        }

        // Simulate physical baking / heating delay (slightly longer than drinks)
        Thread.sleep(PREPARATION_DELAY_MS);

        log.info("[Kitchen Service] Completed preparation of '{}' [correlationId: {}]", item, correlationId);
    }
}
