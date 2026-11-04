package org.example.rest.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RequestIdsTest {

    private static final String VALID = "11111111-1111-4111-8111-111111111111";

    @Test
    void keepsValidValue() {
        assertEquals(VALID, RequestIds.normalize(List.of(VALID)));
    }

    @Test
    void lowercasesValidValue() {
        String upper = "AAAAAAAA-1111-4111-8111-111111111111";
        assertEquals(upper.toLowerCase(), RequestIds.normalize(List.of(upper)));
    }

    @Test
    void replacesShortUuidAcceptedByJavaParser() {
        String normalized = RequestIds.normalize(List.of("1-1-1-1-1"));
        assertNotEquals("1-1-1-1-1", normalized);
        assertEquals(36, normalized.length());
    }

    @Test
    void replacesRepeatedHeader() {
        assertNotEquals(VALID, RequestIds.normalize(List.of(VALID, VALID)));
    }

    @Test
    void generatesWhenMissing() {
        assertEquals(36, RequestIds.normalize(List.of()).length());
        assertEquals(36, RequestIds.normalize(null).length());
    }

    @Test
    void replacesLogInjectionAttempt() {
        String attack = "11111111-1111-4111-8111-11111111111\n";
        assertNotEquals(attack, RequestIds.normalize(List.of(attack)));
    }
}
