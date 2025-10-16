package com.example.rabbitmq.step4_pubsub;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Step 4: 알림 시스템 컨트롤러
 *
 * Pub/Sub 패턴을 사용한 실시간 알림 시스템 API를 제공합니다.
 *
 * 사용 예시:
 * POST http://localhost:8080/api/v1/notifications
 * Content-Type: application/json
 * {
 *   "type": "ORDER_COMPLETED",
 *   "recipientId": "USER001",
 *   "title": "주문이 완료되었습니다",
 *   "content": "주문번호 ORD-001이 성공적으로 처리되었습니다."
 * }
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationProducer notificationProducer;

    /**
     * 알림 전송 API
     *
     * 하나의 요청으로 이메일, SMS, 푸시 알림이 동시에 전송됩니다.
     *
     * @param message 알림 메시지
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody NotificationMessage message) {
        notificationProducer.sendNotification(message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("messageId", message.getMessageId());
        response.put("message", "알림이 전송되었습니다.");
        response.put("channels", new String[]{"EMAIL", "SMS", "PUSH"});

        return ResponseEntity.ok(response);
    }

    /**
     * 간단한 알림 전송 API
     */
    @PostMapping("/simple")
    public ResponseEntity<Map<String, Object>> sendSimpleNotification(
            @RequestParam NotificationMessage.NotificationType type,
            @RequestParam String recipientId,
            @RequestParam String title,
            @RequestParam String content) {

        notificationProducer.sendSimpleNotification(type, recipientId, title, content);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "알림이 전송되었습니다.");
        response.put("type", type);
        response.put("recipient", recipientId);

        return ResponseEntity.ok(response);
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running!");
    }
}
