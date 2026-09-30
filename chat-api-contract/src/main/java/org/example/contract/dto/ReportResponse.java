package org.example.contract.dto;


import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        UUID reporterId,
        UUID reportedId,
        String reason,
        String status,
        Instant createdAt
) {}
