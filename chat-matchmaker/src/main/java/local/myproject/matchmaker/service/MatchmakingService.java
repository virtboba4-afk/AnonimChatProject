package local.myproject.matchmaker.service;



import org.events.EventEnvelope;
import org.events.EventMetadata;
import org.events.MatchFoundPayload;
import org.events.ProfilePayload;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MatchmakingService {

    private final Queue<ProfilePayload> waitingRoom = new ConcurrentLinkedQueue<>();
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    public MatchmakingService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void addPlayerToQueue(ProfilePayload profile) {
        waitingRoom.add(profile);
        System.out.println("[поиск]" +profile.nickname() + " добавлен в очередь. Всего ждет: " + waitingRoom.size());
    }

    @Scheduled(fixedRate = 1000)
    public void tryMatchPlayers() {while (waitingRoom.size() >= 2) {
            ProfilePayload user1 = waitingRoom.poll();
            ProfilePayload user2 = waitingRoom.poll();

            String roomId = "room-" + UUID.randomUUID().toString().substring(0, 8);
            System.out.println(" пара: " + user1.nickname() + " и " + user2.nickname() + " (Комната: " + roomId + ")");


            EventMetadata meta = new EventMetadata(UUID.randomUUID().toString(), Instant.now(), "matchmaker", "match.found");
            MatchFoundPayload payload = new MatchFoundPayload(user1.profileId(), user2.profileId(), roomId);

            rabbitTemplate.convertAndSend(exchangeName, "chat.match.found", new EventEnvelope<>(meta, payload));
        }
    }
}
