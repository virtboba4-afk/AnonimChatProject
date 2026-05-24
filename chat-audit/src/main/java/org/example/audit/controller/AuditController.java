package org.example.audit.controller;

import org.example.audit.model.AuditEntry;
import org.example.audit.storage.AuditStorage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditStorage storage;

    public AuditController(AuditStorage storage) {
        this.storage = storage;
    }

    @GetMapping
    public List<AuditEntry> getLogs() {
        return storage.findAll();
    }
}