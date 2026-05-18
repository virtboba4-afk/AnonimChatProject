package org.example.contract.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.contract.validation.ValidLanguageCode;

public record ProfileRequest(
        @NotBlank String nickname,
        @NotNull Integer age,
        @NotBlank @ValidLanguageCode String preferredLanguage
) {}
