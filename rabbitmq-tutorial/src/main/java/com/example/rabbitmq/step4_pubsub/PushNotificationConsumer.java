package com.example.rabbitmq.step4_pubsub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Step 4: 푸시 알림 Consumer
 *
 * 알림 메시지를 수신하여 푸시 알림을 발송합니다.
 */
@Slf4j
@Component
public class PushNotificationConsumer {

    /**
     * 푸시 알림 수신 및 발송
     */
    @RabbitListener(queues = "${app.rabbitmq.notification.push-queue}")
    public void sendPushNotification(NotificationMessage message) {
        log.info("========================================");
        log.info("[PUSH] 알림 수신");
        log.info("Message ID: {}", message.getMessageId());
        log.info("Type: {}", message.getType());
        log.info("Recipient: {}", message.getRecipientId());
        log.info("Title: {}", message.getTitle());
        log.info("Content: {}", message.getContent());
        log.info("========================================");

        try {
            // 푸시 알림 발송 로직
            sendPush(message);
            log.info("[PUSH] 푸시 알림 발송 성공");
        } catch (Exception e) {
            log.error("[PUSH] 푸시 알림 발송 실패", e);
            throw new RuntimeException("푸시 알림 발송 실패", e);
        }
    }

    /**
     * 실제 푸시 알림 발송 로직
     *
     * 실제 환경에서는 다음과 같은 작업들이 수행됩니다:
     * - FCM/APNS 서버 연결
     * - 디바이스 토큰 조회
     * - 알림 페이로드 구성
     * - 푸시 알림 발송
     * - 발송 결과 저장
     */
    private void sendPush(NotificationMessage message) {
        log.info("[PUSH] 푸시 알림 발송 중...");
        log.info("[PUSH] User: {}", message.getRecipientId());
        log.info("[PUSH] Notification: {} - {}", message.getTitle(), message.getContent());
        log.info("[PUSH] Platform: iOS/Android");

        // 푸시 알림 발송 시뮬레이션
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
