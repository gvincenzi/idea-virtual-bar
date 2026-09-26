package com.gist.idea.bar.dispatcher.dto.jev;

import java.util.Map;

public record JevAnswer(
    String choice,                     // Classified intent ID
    Map<String, Double> probabilities, // Probability distribution
    Double confidence                  // Confidence level (0.0 to 1.0)
) {}
