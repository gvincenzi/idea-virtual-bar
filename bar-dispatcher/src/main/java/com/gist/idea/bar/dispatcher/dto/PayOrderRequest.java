package com.gist.idea.bar.dispatcher.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound request payload for settling the bill of an existing order.
 *
 * @param paymentMethod chosen settlement mechanism (e.g. "CARD", "CASH")
 */
public record PayOrderRequest(
    @NotBlank(message = "Payment method is required (e.g. CARD, CASH)")
    String paymentMethod
) {}

