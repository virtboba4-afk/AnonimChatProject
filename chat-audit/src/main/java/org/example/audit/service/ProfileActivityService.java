package org.example.audit.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.example.audit.diagnostics.DiagnosticMode;
import org.example.audit.model.AuditEntry;
import org.example.audit.storage.AuditStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProfileActivityService {

    private static final Logger log = LoggerFactory.getLogger(ProfileActivityService.class);

    private final AuditStorage storage;
    private final DiagnosticMode diagnosticMode;
    private final MeterRegistry registry;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer successTimer;
    private final Timer failureTimer;
    private final AtomicInteger active = new AtomicInteger();

    public ProfileActivityService(AuditStorage storage, DiagnosticMode diagnosticMode, MeterRegistry registry) {
        this.storage = storage;
        this.diagnosticMode = diagnosticMode;
        this.registry = registry;
        this.successCounter = registry.counter("audit.activity.requests", "result", "success");
        this.failureCounter = registry.counter("audit.activity.requests", "result", "failure");
        this.successTimer = timer("success");
        this.failureTimer = timer("failure");
        Gauge.builder("audit.activity.active", active, AtomicInteger::get).register(registry);
    }

    private Timer timer(String result) {
        return Timer.builder("audit.activity.processing")
                .tag("result", result)
                .serviceLevelObjectives(Duration.ofMillis(500), Duration.ofSeconds(1))
                .publishPercentileHistogram()
                .register(registry);
    }

    public List<AuditEntry> activityOf(UUID profileId) {
        active.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        boolean success = false;
        try {
            diagnosticMode.apply();
            List<AuditEntry> entries = storage.findByProfileId(profileId);
            success = true;
            try (MDC.MDCCloseable event = MDC.putCloseable("event", "audit.activity.read");
                 MDC.MDCCloseable outcome = MDC.putCloseable("outcome", "success")) {
                log.info("Profile activity read");
            }
            return entries;
        } catch (RuntimeException failure) {
            try (MDC.MDCCloseable event = MDC.putCloseable("event", "audit.activity.failed");
                 MDC.MDCCloseable outcome = MDC.putCloseable("outcome", "failure");
                 MDC.MDCCloseable type = MDC.putCloseable("failureType", failure.getClass().getSimpleName())) {
                log.warn("Profile activity failed");
            }
            throw failure;
        } finally {
            sample.stop(success ? successTimer : failureTimer);
            (success ? successCounter : failureCounter).increment();
            active.decrementAndGet();
        }
    }
}
