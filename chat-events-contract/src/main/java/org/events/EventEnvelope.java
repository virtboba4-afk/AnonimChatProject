package org.events;

public record EventEnvelope<T>(
        EventMetadata metadata,
        T payload
) {}
