package org.example.audit.model;

import java.time.Instant;
import java.util.UUID;

public record AuditEntry(
        String eventId,
        String eventType,
        Instant timestamp,
        UUID profileId,
        String nickname
) {}