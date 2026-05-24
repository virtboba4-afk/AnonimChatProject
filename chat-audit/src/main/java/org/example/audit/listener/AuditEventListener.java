package org.example.audit.listener;

import org.example.audit.model.AuditEntry;
import org.example.audit.storage.AuditStorage;
import org.events.EventEnvelope;
import org.events.ProfilePayload;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener {

    private final AuditStorage storage;

    public AuditEventListener(AuditStorage storage) {
        this.storage = storage;
    }

    // Слушаем нашу стандартную очередь, используя профессорскую фабрику конфигураций
    @RabbitListener(queues = "q.chat.audit.events", containerFactory = "rabbitListenerContainerFactory")
    public void handleProfileEvent(EventEnvelope<ProfilePayload> envelope) {
        AuditEntry entry = new AuditEntry(
                envelope.metadata().eventId(),
                envelope.metadata().eventType(),
                envelope.metadata().timestamp(),
                envelope.payload().profileId(),
                envelope.payload().nickname()
        );
        storage.save(entry);
        System.out.println(" [АУДИТ] Успешно записан лог: " + entry.eventType() + " для " + entry.nickname());
    }
}