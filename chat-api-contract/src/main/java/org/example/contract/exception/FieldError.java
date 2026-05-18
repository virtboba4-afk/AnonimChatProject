package org.example.contract.exception;

public record FieldError(
        String field,
        String rejectedValue,
        String message
) {}