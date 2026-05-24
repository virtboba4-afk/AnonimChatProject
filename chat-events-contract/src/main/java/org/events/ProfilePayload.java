package org.events;


public record ProfilePayload(
        Long profileId,
        String nickname,
        String preferredLanguage,
        Integer age
) {}
