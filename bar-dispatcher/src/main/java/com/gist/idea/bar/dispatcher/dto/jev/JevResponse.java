package com.gist.idea.bar.dispatcher.dto.jev;

import java.util.Map;

public record JevResponse(
    String model,
    Map<String, JevAnswer> results
) {}
