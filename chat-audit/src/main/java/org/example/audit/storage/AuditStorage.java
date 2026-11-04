package org.example.audit.storage;

import org.example.audit.model.AuditEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class AuditStorage {
    private final List<AuditEntry> logs = new CopyOnWriteArrayList<>();

    public void save(AuditEntry entry) {
        logs.add(entry);
    }

    public List<AuditEntry> findAll() {
        return logs;
    }

    public List<AuditEntry> findByProfileId(UUID profileId) {
        return logs.stream()
                .filter(entry -> profileId.equals(entry.profileId()))
                .toList();
    }
}