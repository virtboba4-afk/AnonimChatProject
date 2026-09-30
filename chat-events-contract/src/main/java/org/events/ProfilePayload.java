package org.events;


import java.util.UUID;

public record ProfilePayload(
        UUID profileId,
        String nickname,
        String preferredLanguage,
        Integer age
) {}
