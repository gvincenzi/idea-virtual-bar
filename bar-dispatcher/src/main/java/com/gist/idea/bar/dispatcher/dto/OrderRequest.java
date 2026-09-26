package com.gist.idea.bar.dispatcher.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound request payload for customer natural language orders.
 *
 * @param message free-text statement submitted by the customer (e.g. "I'd like an espresso and a croissant")
 */
public record OrderRequest(
    @NotBlank(message = "Order message cannot be empty")
    String message
) {}
