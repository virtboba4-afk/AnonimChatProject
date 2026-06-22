package org.example.rest.event;

import org.example.contract.dto.ProfileResponse;
import org.events.EventEnvelope;
import org.events.EventMetadata;
import org.events.ProfilePayload;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ProfileEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;


    public ProfileEventPublisher(RabbitTemplate rabbitTemplate,
                                 @Value("${rabbitmq.exchange.name}") String exchangeName) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
    }

    public void publishProfileCreated(ProfileResponse profile) {
        EventMetadata metadata = new EventMetadata(
                UUID.randomUUID().toString(),
                Instant.now(),
                "chat-rest",
                "profile.created"
        );



        ProfilePayload payload = new ProfilePayload(
                profile.getId(),
                profile.getNickname(),
                profile.getPreferredLanguage(),
                profile.getAge()
        );

        EventEnvelope<ProfilePayload> envelope = new EventEnvelope<>(metadata, payload);

        rabbitTemplate.convertAndSend(exchangeName, "chat.profile.created", envelope);
    }

    public void publishSearchStarted(ProfileResponse profile) {
        EventMetadata metadata = new EventMetadata(
                UUID.randomUUID().toString(), Instant.now(), "chat-rest", "profile.search.started"
        );
        ProfilePayload payload = new ProfilePayload(
                profile.getId(), profile.getNickname(), profile.getPreferredLanguage(), profile.getAge()
        );
        rabbitTemplate.convertAndSend(exchangeName, "chat.profile.search.started", new EventEnvelope<>(metadata, payload));
    }
}
