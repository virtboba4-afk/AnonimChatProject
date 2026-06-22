package org.events;


public record MatchFoundPayload(
        Long user1Id,
        Long user2Id,
        String roomId
) {}
