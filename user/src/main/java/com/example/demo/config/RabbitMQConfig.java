package com.example.demo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "user-exchange";
    public static final String CREATE_QUEUE = "user.create.queue";
    public static final String DELETE_QUEUE = "user.delete.user-queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue createQueue() {
        return new Queue(CREATE_QUEUE);
    }

    @Bean
    public Queue deleteQueue() {
        return new Queue(DELETE_QUEUE);
    }

    @Bean
    public Binding bindingCreate(Queue createQueue, TopicExchange exchange) {
        return BindingBuilder.bind(createQueue).to(exchange).with("user.create");
    }

    @Bean
    public Binding bindingDelete(Queue deleteQueue, TopicExchange exchange) {
        return BindingBuilder.bind(deleteQueue).to(exchange).with("user.delete");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
