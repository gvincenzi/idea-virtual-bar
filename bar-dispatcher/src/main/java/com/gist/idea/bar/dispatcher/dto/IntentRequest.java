package com.gist.idea.bar.dispatcher.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Universal entry payload for the Intent-Driven Architecture Spike.
 *
 * @param message natural language statement in any language
 * @param correlationId optional ID identifying an ongoing order (required for status check or payment)
 */
public record IntentRequest(
    @NotBlank(message = "Message statement cannot be empty")
    String message,
    UUID correlationId
) {}

