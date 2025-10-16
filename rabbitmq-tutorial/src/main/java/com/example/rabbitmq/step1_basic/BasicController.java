package com.example.rabbitmq.step1_basic;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Step 1: 기본 메시지 전송 컨트롤러
 *
 * REST API를 통해 메시지를 RabbitMQ에 전송하는 엔드포인트를 제공합니다.
 *
 * 사용 예시:
 * POST http://localhost:8080/api/v1/basic/send
 * Content-Type: application/json
 * {
 *   "content": "Hello RabbitMQ!",
 *   "sender": "user1"
 * }
 */
@RestController
@RequestMapping("/api/v1/basic")
@RequiredArgsConstructor
public class BasicController {

    private final BasicProducer basicProducer;

    /**
     * 메시지 전송 API
     *
     * @param message 전송할 메시지
     * @return 응답 메시지
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody BasicMessage message) {
        basicProducer.sendMessage(message);
        return ResponseEntity.ok("메시지가 성공적으로 전송되었습니다: " + message.getMessageId());
    }

    /**
     * 간단한 텍스트 메시지 전송 API
     *
     * @param content 메시지 내용
     * @param sender 발신자 (선택적)
     * @return 응답 메시지
     */
    @PostMapping("/send/simple")
    public ResponseEntity<String> sendSimpleMessage(
            @RequestParam String content,
            @RequestParam(defaultValue = "anonymous") String sender) {
        basicProducer.sendSimpleMessage(content, sender);
        return ResponseEntity.ok("메시지가 성공적으로 전송되었습니다: " + content);
    }

    /**
     * 헬스 체크 API
     *
     * @return 상태 메시지
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Basic Message Service is running!");
    }
}
