package com.example.rabbitmq.step4_pubsub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Step 4: SMS 알림 Consumer
 *
 * 알림 메시지를 수신하여 SMS를 발송합니다.
 */
@Slf4j
@Component
public class SmsNotificationConsumer {

    /**
     * SMS 알림 수신 및 발송
     */
    @RabbitListener(queues = "${app.rabbitmq.notification.sms-queue}")
    public void sendSmsNotification(NotificationMessage message) {
        log.info("========================================");
        log.info("[SMS] 알림 수신");
        log.info("Message ID: {}", message.getMessageId());
        log.info("Type: {}", message.getType());
        log.info("Recipient: {}", message.getRecipientId());
        log.info("Title: {}", message.getTitle());
        log.info("Content: {}", message.getContent());
        log.info("========================================");

        try {
            // SMS 발송 로직
            sendSms(message);
            log.info("[SMS] SMS 발송 성공");
        } catch (Exception e) {
            log.error("[SMS] SMS 발송 실패", e);
            throw new RuntimeException("SMS 발송 실패", e);
        }
    }

    /**
     * 실제 SMS 발송 로직
     *
     * 실제 환경에서는 다음과 같은 작업들이 수행됩니다:
     * - SMS 게이트웨이 API 호출
     * - 발신번호 설정
     * - 메시지 길이 체크 및 분할
     * - SMS 발송
     * - 발송 이력 저장
     */
    private void sendSms(NotificationMessage message) {
        log.info("[SMS] SMS 발송 중...");
        log.info("[SMS] To: 010-XXXX-{}", message.getRecipientId().substring(Math.max(0, message.getRecipientId().length() - 4)));
        log.info("[SMS] Message: [{}] {}", message.getTitle(), message.getContent());

        // SMS 발송 시뮬레이션
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
