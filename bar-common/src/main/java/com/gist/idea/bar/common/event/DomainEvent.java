package com.gist.idea.bar.common.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID correlationId();
    Instant timestamp();
    String intentName();
}

