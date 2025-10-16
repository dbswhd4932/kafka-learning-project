package com.example.rabbitmq.step4_pubsub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Step 4: 이메일 알림 Consumer
 *
 * 알림 메시지를 수신하여 이메일을 발송합니다.
 */
@Slf4j
@Component
public class EmailNotificationConsumer {

    /**
     * 이메일 알림 수신 및 발송
     */
    @RabbitListener(queues = "${app.rabbitmq.notification.email-queue}")
    public void sendEmailNotification(NotificationMessage message) {
        log.info("========================================");
        log.info("[EMAIL] 알림 수신");
        log.info("Message ID: {}", message.getMessageId());
        log.info("Type: {}", message.getType());
        log.info("Recipient: {}", message.getRecipientId());
        log.info("Title: {}", message.getTitle());
        log.info("Content: {}", message.getContent());
        log.info("========================================");

        try {
            // 이메일 발송 로직
            sendEmail(message);
            log.info("[EMAIL] 이메일 발송 성공");
        } catch (Exception e) {
            log.error("[EMAIL] 이메일 발송 실패", e);
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    /**
     * 실제 이메일 발송 로직
     *
     * 실제 환경에서는 다음과 같은 작업들이 수행됩니다:
     * - SMTP 서버 연결
     * - 이메일 템플릿 렌더링
     * - 첨부파일 처리
     * - 이메일 발송
     * - 발송 이력 저장
     */
    private void sendEmail(NotificationMessage message) {
        log.info("[EMAIL] 이메일 발송 중...");
        log.info("[EMAIL] To: {}@example.com", message.getRecipientId());
        log.info("[EMAIL] Subject: {}", message.getTitle());
        log.info("[EMAIL] Body: {}", message.getContent());

        // 이메일 발송 시뮬레이션
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
