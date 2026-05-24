package org.events;

import java.time.Instant;

public record EventMetadata(
        String eventId,
        Instant timestamp,
        String source,
        String eventType
) {}