package org.example.rest.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class AuditClient {

    private static final Logger log = LoggerFactory.getLogger(AuditClient.class);

    private final RestClient auditRestClient;

    public AuditClient(RestClient auditRestClient) {
        this.auditRestClient = auditRestClient;
    }

    public List<ActivityEntry> activityOf(UUID profileId) {
        log.info("Calling chat-audit");
        return auditRestClient.get()
                .uri("/internal/audit/{profileId}", profileId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
