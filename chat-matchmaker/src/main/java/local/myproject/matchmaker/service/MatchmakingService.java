package local.myproject.matchmaker.service;

import org.events.EventEnvelope;
import org.events.EventMetadata;
import org.events.MatchFoundPayload;
import org.events.ProfilePayload;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MatchmakingService {


    private final List<ProfilePayload> waitingRoom = new ArrayList<>();
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    public MatchmakingService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public synchronized void addPlayerToQueue(ProfilePayload profile) {
        waitingRoom.add(profile);
        System.out.println("[поиск] " + profile.nickname() + " добавлен в очередь. Всего ждет: " + waitingRoom.size());

        tryMatchPlayers();
    }

    private synchronized void tryMatchPlayers() {
        boolean matched = true;

        while (matched) {
            matched = false;
            if (waitingRoom.size() < 2) return;

            for (int i = 0; i < waitingRoom.size(); i++) {
                ProfilePayload user1 = waitingRoom.get(i);

                for (int j = i + 1; j < waitingRoom.size(); j++) {
                    ProfilePayload user2 = waitingRoom.get(j);


                    boolean isSameLanguage = user1.preferredLanguage().equalsIgnoreCase(user2.preferredLanguage());
                    boolean isCloseAge = Math.abs(user1.age() - user2.age()) <= 5;


                    if (isSameLanguage && isCloseAge) {
                        waitingRoom.remove(user2);
                        waitingRoom.remove(user1);

                        String roomId = "room-" + UUID.randomUUID().toString().substring(0, 8);
                        System.out.println(" ПАРА: " + user1.nickname() + " и " + user2.nickname() +
                                " (Язык: " + user1.preferredLanguage() +
                                ", Возраст: " + user1.age() + "/" + user2.age() + ")");

                        EventMetadata meta = new EventMetadata(UUID.randomUUID().toString(), Instant.now(), "matchmaker", "match.found");
                        MatchFoundPayload payload = new MatchFoundPayload(user1.profileId(), user2.profileId(), roomId);

                        rabbitTemplate.convertAndSend(exchangeName, "chat.match.found", new EventEnvelope<>(meta, payload));

                        matched = true;
                        break;
                    }
                }
                if (matched) break;
            }
        }
    }
}