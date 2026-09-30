package org.example.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReportRequest(
        @NotNull UUID reporterId,
        @NotNull UUID reportedId,
        @NotBlank String reason
) {}
