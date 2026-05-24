package org.example.audit.model;

import java.time.Instant;

public record AuditEntry(
        String eventId,
        String eventType,
        Instant timestamp,
        Long profileId,
        String nickname
) {}