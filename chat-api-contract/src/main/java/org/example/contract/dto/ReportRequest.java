package org.example.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportRequest(
        @NotNull Long reporterId,
        @NotNull Long reportedId,
        @NotBlank String reason
) {}
