package org.example.contract.dto;


import org.example.contract.validation.ValidLanguageCode;

public record PatchProfileRequest(
        String nickname,
        Integer age,
        @ValidLanguageCode String preferredLanguage
) {}

