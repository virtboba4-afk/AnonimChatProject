package local.myproject.notification.listener;




import org.events.EventEnvelope;
import org.events.MatchFoundPayload;
import org.events.ProfilePayload;
import local.myproject.notification.websocket.NotificationWebSocketHandler;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class NotificationEventListener {

    private final NotificationWebSocketHandler webSocketHandler;

    public NotificationEventListener(NotificationWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange("chat.events.exchange");
    }

    @Bean
    public Queue profileNotificationQueue() {
        return QueueBuilder.durable("chat.notification.profile.queue").build();
    }

    @Bean
    public Binding profileNotificationBinding(Queue profileNotificationQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(profileNotificationQueue).to(chatExchange).with("chat.profile.#");
    }


    @Bean
    public Queue matchNotificationQueue() {
        return QueueBuilder.durable("chat.notification.match.queue").build();
    }

    @Bean
    public Binding matchNotificationBinding(Queue matchNotificationQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(matchNotificationQueue).to(chatExchange).with("chat.match.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }





    @RabbitListener(queues = "chat.notification.profile.queue")
    public void handleProfileEvent(EventEnvelope<ProfilePayload> event) {
        String eventType = event.metadata().eventType();
        String nickname = event.payload().nickname();

        if ("profile.created".equals(eventType)) {
            webSocketHandler.broadcast("{\"type\": \"NEW_USER\", \"message\": \"🎉 Новый пользователь " + nickname + " зашел в рулетку!\"}");
        } else if ("profile.enriched".equals(eventType)) {
            webSocketHandler.broadcast("{\"type\": \"ENRICHED\", \"message\": \"📊 Профиль " + nickname + " проанализирован!\"}");
        }
    }

    @RabbitListener(queues = "chat.notification.match.queue")
    public void handleMatchFound(EventEnvelope<MatchFoundPayload> event) {
        if ("match.found".equals(event.metadata().eventType())) {
            MatchFoundPayload match = event.payload();
            String msg = "{\"type\": \"MATCH_READY\", \"message\": \"⚡ Собеседник найден! Вы вдвоем переведены в " + match.roomId() + "\"}";

            webSocketHandler.sendToUser(match.user1Id(), msg);
            webSocketHandler.sendToUser(match.user2Id(), msg);
        }
    }
}