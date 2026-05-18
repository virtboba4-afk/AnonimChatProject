package org.example.rest.storage;

import org.example.contract.dto.ProfileResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryStorage {
    public final Map<Long, ProfileResponse> profiles = new ConcurrentHashMap<>();
    public final AtomicLong profileSequence = new AtomicLong(0);
}
