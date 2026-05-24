package org.example.rest.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name:chat.events.exchange}")
    private String exchangeName;

    // Настраиваем отправку в формате JSON
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Создаем только точку обмена (Exchange)
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(exchangeName);
    }
}