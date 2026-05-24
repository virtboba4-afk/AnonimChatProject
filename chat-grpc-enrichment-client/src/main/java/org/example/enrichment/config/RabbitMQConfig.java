package org.example.enrichment.config;

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
    public Queue enrichmentQueue() {
        return QueueBuilder.durable("chat.enrichment.queue").build();
    }

    @Bean
    public Binding enrichmentBinding(Queue enrichmentQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(enrichmentQueue).to(chatExchange).with("chat.profile.created");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}