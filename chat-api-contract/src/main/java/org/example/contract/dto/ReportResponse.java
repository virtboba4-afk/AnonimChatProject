package org.example.contract.dto;


import java.time.Instant;

public record ReportResponse(
        Long id,
        Long reporterId,
        Long reportedId,
        String reason,
        String status,
        Instant createdAt
) {}
