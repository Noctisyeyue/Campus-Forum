package com.campus.forum.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息队列配置
 */
@Configuration
public class RabbitConfiguration {

    /** 邮件发送队列（持久化） */
    @Bean("mailQueue")
    public Queue queue() {
        return QueueBuilder
                .durable("mail")
                .build();
    }
}
