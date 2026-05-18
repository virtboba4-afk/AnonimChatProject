package org.example.contract.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        String type,
        String title,
        String detail,
        String instance,
        Instant timestamp,
        List<FieldError> fieldErrors
) {}
