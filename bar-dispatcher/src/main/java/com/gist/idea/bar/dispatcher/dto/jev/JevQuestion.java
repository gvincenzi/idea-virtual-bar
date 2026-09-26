package com.gist.idea.bar.dispatcher.dto.jev;

import java.util.Map;

public record JevQuestion(
    String type,                // "choice" for intent classification
    Map<String, String> choices // Intent ID -> Semantic description
) {
    public static JevQuestion choice(Map<String, String> choices) {
        return new JevQuestion("choice", choices);
    }
}
