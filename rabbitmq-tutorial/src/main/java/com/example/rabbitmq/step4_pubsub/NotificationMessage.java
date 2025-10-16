package com.example.rabbitmq.step4_pubsub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 메시지 DTO
 *
 * Pub/Sub 패턴을 사용한 실시간 알림 시스템에서 사용하는 메시지 구조
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {

    /**
     * 메시지 ID
     */
    private String messageId;

    /**
     * 알림 유형
     */
    private NotificationType type;

    /**
     * 수신자 ID
     */
    private String recipientId;

    /**
     * 제목
     */
    private String title;

    /**
     * 내용
     */
    private String content;

    /**
     * 전송 시간
     */
    private LocalDateTime sentAt;

    /**
     * 간편 생성자
     */
    public NotificationMessage(NotificationType type, String recipientId, String title, String content) {
        this.messageId = java.util.UUID.randomUUID().toString();
        this.type = type;
        this.recipientId = recipientId;
        this.title = title;
        this.content = content;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * 알림 유형
     */
    public enum NotificationType {
        ORDER_CREATED,      // 주문 생성
        ORDER_COMPLETED,    // 주문 완료
        PAYMENT_SUCCESS,    // 결제 성공
        PAYMENT_FAILED,     // 결제 실패
        SHIPPING_STARTED,   // 배송 시작
        SHIPPING_COMPLETED, // 배송 완료
        SYSTEM_ALERT        // 시스템 알림
    }
}
