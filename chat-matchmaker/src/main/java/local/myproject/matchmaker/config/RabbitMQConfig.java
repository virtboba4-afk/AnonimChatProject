package local.myproject.matchmaker.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange("chat.events.exchange");
    }


    @Bean
    public Queue matchmakerQueue() {
        return QueueBuilder.durable("chat.matchmaker.queue").build();
    }


    @Bean
    public Binding matchmakerBinding(Queue matchmakerQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(matchmakerQueue).to(chatExchange).with("chat.profile.search.started");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
