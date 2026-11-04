package org.example.rest.client;

import java.time.Instant;

public record ActivityEntry(String eventType, Instant timestamp) {
}
