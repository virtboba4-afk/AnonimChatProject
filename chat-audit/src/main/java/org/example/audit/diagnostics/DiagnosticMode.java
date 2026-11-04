package org.example.audit.diagnostics;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DiagnosticMode {

    public enum Mode {
        NORMAL, SLOW, FAIL
    }

    private record State(Mode mode, Instant expiresAt) {
    }

    private static final Duration SLOW_DELAY = Duration.ofMillis(750);
    private static final State NORMAL_STATE = new State(Mode.NORMAL, Instant.MAX);

    private final AtomicReference<State> state = new AtomicReference<>(NORMAL_STATE);

    public void set(Mode mode, int seconds) {
        state.set(mode == Mode.NORMAL ? NORMAL_STATE : new State(mode, Instant.now().plusSeconds(seconds)));
    }

    public Mode current() {
        State current = state.get();
        return Instant.now().isBefore(current.expiresAt()) ? current.mode() : Mode.NORMAL;
    }

    public void apply() {
        switch (current()) {
            case SLOW -> sleep();
            case FAIL -> throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Diagnostic failure");
            case NORMAL -> {
            }
        }
    }

    private void sleep() {
        try {
            Thread.sleep(SLOW_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
