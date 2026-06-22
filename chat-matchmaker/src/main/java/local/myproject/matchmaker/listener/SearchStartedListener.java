package local.myproject.matchmaker.listener;

import org.events.EventEnvelope;
import org.events.ProfilePayload;
import local.myproject.matchmaker.service.MatchmakingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SearchStartedListener {

    private final MatchmakingService matchmakingService;

    public SearchStartedListener(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @RabbitListener(queues = "chat.matchmaker.queue")
    public void handleSearchStarted(EventEnvelope<ProfilePayload> event) {


        ProfilePayload profile = event.payload();

        if ("profile.search.started".equals(event.metadata().eventType())) {
            matchmakingService.addPlayerToQueue(profile);
        }
    }
}
