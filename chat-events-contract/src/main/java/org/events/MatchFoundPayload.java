package org.events;


import java.util.UUID;

public record MatchFoundPayload(
        UUID user1Id,
        UUID user2Id,
        String roomId
) {}
