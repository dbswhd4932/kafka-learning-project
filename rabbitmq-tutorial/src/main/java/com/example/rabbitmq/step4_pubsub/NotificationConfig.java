package com.example.rabbitmq.step4_pubsub;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Step 4: Pub/Sub 실시간 알람 시스템 설정
 *
 * Fanout Exchange를 사용하여 하나의 이벤트를 여러 채널로 전송합니다.
 *
 * 사용 사례:
 * - 주문 완료 시 이메일, SMS, 푸시 알림을 동시에 전송
 * - 시스템 알림을 모든 채널로 브로드캐스트
 * - 실시간 이벤트를 여러 구독자에게 전달
 *
 * 동작 방식:
 * Producer → Fanout Exchange → Email Queue
 *                           → SMS Queue
 *                           → Push Queue
 *
 * 각 Consumer는 독립적으로 알림을 처리합니다.
 */
@Configuration
public class NotificationConfig {

    @Value("${app.rabbitmq.notification.exchange-name}")
    private String exchangeName;

    @Value("${app.rabbitmq.notification.email-queue}")
    private String emailQueueName;

    @Value("${app.rabbitmq.notification.sms-queue}")
    private String smsQueueName;

    @Value("${app.rabbitmq.notification.push-queue}")
    private String pushQueueName;

    /**
     * 알림 Fanout Exchange 생성
     */
    @Bean
    public FanoutExchange notificationExchange() {
        return new FanoutExchange(exchangeName);
    }

    /**
     * 이메일 알림 Queue 생성
     */
    @Bean
    public Queue emailNotificationQueue() {
        return new Queue(emailQueueName, true);
    }

    /**
     * SMS 알림 Queue 생성
     */
    @Bean
    public Queue smsNotificationQueue() {
        return new Queue(smsQueueName, true);
    }

    /**
     * 푸시 알림 Queue 생성
     */
    @Bean
    public Queue pushNotificationQueue() {
        return new Queue(pushQueueName, true);
    }

    /**
     * 이메일 알림 Binding
     */
    @Bean
    public Binding emailNotificationBinding(Queue emailNotificationQueue, FanoutExchange notificationExchange) {
        return BindingBuilder.bind(emailNotificationQueue).to(notificationExchange);
    }

    /**
     * SMS 알림 Binding
     */
    @Bean
    public Binding smsNotificationBinding(Queue smsNotificationQueue, FanoutExchange notificationExchange) {
        return BindingBuilder.bind(smsNotificationQueue).to(notificationExchange);
    }

    /**
     * 푸시 알림 Binding
     */
    @Bean
    public Binding pushNotificationBinding(Queue pushNotificationQueue, FanoutExchange notificationExchange) {
        return BindingBuilder.bind(pushNotificationQueue).to(notificationExchange);
    }
}
