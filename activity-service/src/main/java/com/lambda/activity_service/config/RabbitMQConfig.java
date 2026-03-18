package com.lambda.activity_service.activitymodule;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue activityCreateQueue() {
        return new Queue("activity.create", true); // durable=true
    }

    @Bean
    public Queue activityCreatedQueue() {
        return new Queue("activity.created", true);
    }

    @Bean
    public Queue activityErrorQueue() {
        return new Queue("activity.error", true);
    }

    // Converte mensagens para/de JSON automaticamente
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
