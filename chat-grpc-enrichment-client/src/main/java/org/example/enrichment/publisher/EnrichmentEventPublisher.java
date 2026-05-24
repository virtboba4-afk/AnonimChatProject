package org.example.enrichment.publisher;

import org.events.EventEnvelope;
import org.events.EventMetadata;
import org.events.ProfilePayload;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class EnrichmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;

    public EnrichmentEventPublisher(RabbitTemplate rabbitTemplate,
                                    @Value("${rabbitmq.exchange.name}") String exchangeName) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
    }

    public void publishEnrichedProfile(ProfilePayload payload, double score, String category) {
        EventMetadata metadata = new EventMetadata(
                UUID.randomUUID().toString(), Instant.now(), "grpc-enrichment-client", "profile.enriched"
        );
        EventEnvelope<ProfilePayload> envelope = new EventEnvelope<>(metadata, payload);

        rabbitTemplate.convertAndSend(exchangeName, "chat.profile.enriched", envelope);
        System.out.println(" [ОБОГАЩЕНИЕ] Обогащенный профиль отправлен в RabbitMQ (score: " + score + ")");
    }
}