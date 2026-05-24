package org.example.enrichment.listener;

import org.example.enrichment.publisher.EnrichmentEventPublisher;
import org.events.EventEnvelope;
import org.events.ProfilePayload;
import org.example.grpc.analytics.AnalyzeProfileRequest;
import org.example.grpc.analytics.AnalyzeProfileResponse;
import org.example.grpc.analytics.ProfileAnalyticsServiceGrpc;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProfileCreatedListener {

    private final ProfileAnalyticsServiceGrpc.ProfileAnalyticsServiceBlockingStub analyticsClient;
    private final EnrichmentEventPublisher publisher;

    public ProfileCreatedListener(ProfileAnalyticsServiceGrpc.ProfileAnalyticsServiceBlockingStub analyticsClient,
                                  EnrichmentEventPublisher publisher) {
        this.analyticsClient = analyticsClient;
        this.publisher = publisher;
    }

    @RabbitListener(queues = "chat.enrichment.queue")
    public void handleProfileCreated(EventEnvelope<ProfilePayload> event) {
        ProfilePayload payload = event.payload();
        System.out.println("📥 [ОБОГАЩЕНИЕ] Поймано событие создания профиля: " + payload.nickname());

        AnalyzeProfileRequest request = AnalyzeProfileRequest.newBuilder()
                .setProfileId(payload.profileId())
                .setAge(payload.age())
                .setNickname(payload.nickname())
                .setPreferredLanguage(payload.preferredLanguage())
                .build();

        AnalyzeProfileResponse response = analyticsClient.analyzeProfile(request);

        publisher.publishEnrichedProfile(payload, response.getMatchingScore(), response.getCategoryLevel());
    }
}