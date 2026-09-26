package com.gist.idea.bar.dispatcher.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.gist.idea.bar.common.model.IntentEnum;

/**
 * Immediate asynchronous response returned by the Dispatcher (HTTP 202 Accepted).
 *
 * @param correlationId unique end-to-end identifier minted for this intent lifecycle
 * @param status high-level lifecycle indicator (e.g. "RECEIVED")
 * @param classifiedIntents list of recognized intents and their confidence scores from Jev
 * @param timestamp instant when the request was accepted
 */
public record OrderAcceptedResponse(
    UUID correlationId,
    String status,
    List<IntentEnum> classifiedIntents,
    Instant timestamp
) {}
