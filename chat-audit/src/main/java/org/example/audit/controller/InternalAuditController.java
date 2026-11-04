package org.example.audit.controller;

import org.example.audit.diagnostics.DiagnosticMode;
import org.example.audit.model.AuditEntry;
import org.example.audit.service.ProfileActivityService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class InternalAuditController {

    private final ProfileActivityService activityService;
    private final DiagnosticMode diagnosticMode;

    public InternalAuditController(ProfileActivityService activityService, DiagnosticMode diagnosticMode) {
        this.activityService = activityService;
        this.diagnosticMode = diagnosticMode;
    }

    @GetMapping("/audit/{profileId}")
    public List<AuditEntry> activity(@PathVariable UUID profileId) {
        return activityService.activityOf(profileId);
    }

    @PutMapping("/diagnostics/mode")
    public Map<String, String> setMode(@RequestParam DiagnosticMode.Mode mode,
                                       @RequestParam(defaultValue = "60") int seconds) {
        if (seconds < 1 || seconds > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "seconds must be between 1 and 120");
        }
        diagnosticMode.set(mode, seconds);
        return Map.of("mode", diagnosticMode.current().name());
    }
}
