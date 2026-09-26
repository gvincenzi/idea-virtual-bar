package com.gist.idea.bar.dispatcher.dto.jev;

import java.util.Map;

public record JevRequest(
    String model,
    String state,
    Map<String, JevQuestion> questions
) {}
