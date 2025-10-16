package com.example.rabbitmq.step4_pubsub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Step 4: 알림 메시지 Producer
 *
 * 하나의 이벤트를 여러 알림 채널로 전송합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.notification.exchange-name}")
    private String exchangeName;

    /**
     * 알림 메시지 전송
     *
     * Fanout Exchange를 통해 이메일, SMS, 푸시 알림이 동시에 전송됩니다.
     *
     * @param message 알림 메시지
     */
    public void sendNotification(NotificationMessage message) {
        log.info("=== 알림 메시지 전송 시작 ===");
        log.info("Exchange: {}", exchangeName);
        log.info("Type: {}", message.getType());
        log.info("Recipient: {}", message.getRecipientId());
        log.info("Title: {}", message.getTitle());
        log.info("Content: {}", message.getContent());

        // Fanout Exchange로 전송 (Routing Key 무시)
        rabbitTemplate.convertAndSend(exchangeName, "", message);

        log.info("알림 메시지 전송 완료 - 모든 채널에 브로드캐스트됨");
        log.info("=== 알림 메시지 전송 완료 ===");
    }

    /**
     * 간편 알림 전송
     */
    public void sendSimpleNotification(NotificationMessage.NotificationType type, String recipientId, String title, String content) {
        NotificationMessage message = new NotificationMessage(type, recipientId, title, content);
        sendNotification(message);
    }
}
