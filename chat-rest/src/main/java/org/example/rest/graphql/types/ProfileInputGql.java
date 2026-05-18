package org.example.rest.graphql.types;

public record ProfileInputGql(
        String nickname,
        Integer age,
        String preferredLanguage
) {}