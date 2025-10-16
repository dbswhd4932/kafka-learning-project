package com.example.rabbitmq.step5_logging;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Step 5: 로그 수집 시스템 컨트롤러
 *
 * Topic Exchange를 활용한 로그 수집 API를 제공합니다.
 *
 * 사용 예시:
 * POST http://localhost:8080/api/v1/logs
 * Content-Type: application/json
 * {
 *   "level": "ERROR",
 *   "serviceName": "OrderService",
 *   "message": "주문 처리 중 오류 발생"
 * }
 */
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogProducer logProducer;

    /**
     * 로그 전송 API
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> sendLog(@RequestBody LogMessage logMessage) {
        logProducer.sendLog(logMessage);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("logId", logMessage.getLogId());
        response.put("routingKey", logMessage.getRoutingKey());
        response.put("message", "로그가 전송되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * Error 로그 전송
     */
    @PostMapping("/error")
    public ResponseEntity<Map<String, Object>> sendErrorLog(
            @RequestParam String serviceName,
            @RequestParam String message) {

        logProducer.error(serviceName, message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("level", "ERROR");
        response.put("message", "에러 로그가 전송되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * Warn 로그 전송
     */
    @PostMapping("/warn")
    public ResponseEntity<Map<String, Object>> sendWarnLog(
            @RequestParam String serviceName,
            @RequestParam String message) {

        logProducer.warn(serviceName, message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("level", "WARN");
        response.put("message", "경고 로그가 전송되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * Info 로그 전송
     */
    @PostMapping("/info")
    public ResponseEntity<Map<String, Object>> sendInfoLog(
            @RequestParam String serviceName,
            @RequestParam String message) {

        logProducer.info(serviceName, message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("level", "INFO");
        response.put("message", "정보 로그가 전송되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Logging Service is running!");
    }
}
