package com.example.rabbitmq.step3_routing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Step 3: Routing Model 컨트롤러
 *
 * Direct, Topic, Fanout Exchange를 테스트하기 위한 REST API를 제공합니다.
 *
 * 사용 예시:
 *
 * 1. Direct Exchange 테스트
 * POST http://localhost:8080/api/v1/routing/direct?routingKey=high&content=긴급작업
 *
 * 2. Topic Exchange 테스트
 * POST http://localhost:8080/api/v1/routing/topic?routingKey=order.created&content=주문생성
 *
 * 3. Fanout Exchange 테스트
 * POST http://localhost:8080/api/v1/routing/fanout?content=전체알림
 */
@RestController
@RequestMapping("/api/v1/routing")
@RequiredArgsConstructor
public class RoutingController {

    private final RoutingProducer routingProducer;

    /**
     * Direct Exchange 메시지 전송
     *
     * @param routingKey Routing Key (high, medium, low)
     * @param content 메시지 내용
     */
    @PostMapping("/direct")
    public ResponseEntity<Map<String, Object>> sendToDirectExchange(
            @RequestParam String routingKey,
            @RequestParam String content) {

        routingProducer.sendToDirectExchange(routingKey, content);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("exchangeType", "DIRECT");
        response.put("routingKey", routingKey);
        response.put("message", "Direct Exchange로 메시지가 전송되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * Topic Exchange 메시지 전송
     *
     * @param routingKey Routing Key (order.*, *.payment.*, 등)
     * @param content 메시지 내용
     */
    @PostMapping("/topic")
    public ResponseEntity<Map<String, Object>> sendToTopicExchange(
            @RequestParam String routingKey,
            @RequestParam String content) {

        routingProducer.sendToTopicExchange(routingKey, content);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("exchangeType", "TOPIC");
        response.put("routingKey", routingKey);
        response.put("message", "Topic Exchange로 메시지가 전송되었습니다.");
        response.put("matchingPatterns", getMatchingPatterns(routingKey));

        return ResponseEntity.ok(response);
    }

    /**
     * Fanout Exchange 메시지 전송
     *
     * @param content 메시지 내용
     */
    @PostMapping("/fanout")
    public ResponseEntity<Map<String, Object>> sendToFanoutExchange(
            @RequestParam String content) {

        routingProducer.sendToFanoutExchange(content);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("exchangeType", "FANOUT");
        response.put("message", "Fanout Exchange로 메시지가 전송되었습니다.");
        response.put("note", "모든 연결된 Queue에 메시지가 전달됩니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Routing Service is running!");
    }

    /**
     * Topic Exchange의 매칭 패턴 설명
     */
    private Map<String, String> getMatchingPatterns(String routingKey) {
        Map<String, String> patterns = new HashMap<>();

        if (routingKey.startsWith("order.") && routingKey.split("\\.").length == 2) {
            patterns.put("order.*", "Order Queue에 전달됩니다.");
        }

        if (routingKey.contains(".payment.")) {
            patterns.put("*.payment.*", "Payment Queue에 전달됩니다.");
        }

        patterns.put("#", "All Queue에 전달됩니다.");

        return patterns;
    }
}
