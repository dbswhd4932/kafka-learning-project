package com.example.rabbitmq.step6_dlq;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Step 6: DLQ 테스트 컨트롤러
 *
 * Dead Letter Queue와 Retry 메커니즘을 테스트하기 위한 API를 제공합니다.
 *
 * 사용 예시:
 *
 * 1. 성공 케이스 테스트
 * POST http://localhost:8080/api/v1/dlq/task/success?content=정상작업
 *
 * 2. 실패 케이스 테스트 (DLQ로 이동)
 * POST http://localhost:8080/api/v1/dlq/task/fail?content=실패작업
 */
@RestController
@RequestMapping("/api/v1/dlq")
@RequiredArgsConstructor
public class DlqController {

    private final TaskProducer taskProducer;

    /**
     * 성공 작업 전송
     */
    @PostMapping("/task/success")
    public ResponseEntity<Map<String, Object>> sendSuccessTask(@RequestParam String content) {
        taskProducer.sendSuccessTask(content);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "성공 작업이 전송되었습니다.");
        response.put("content", content);
        response.put("shouldFail", false);

        return ResponseEntity.ok(response);
    }

    /**
     * 실패 작업 전송 (DLQ 테스트용)
     *
     * 이 작업은 의도적으로 실패하여 DLQ로 이동합니다.
     */
    @PostMapping("/task/fail")
    public ResponseEntity<Map<String, Object>> sendFailTask(@RequestParam String content) {
        taskProducer.sendFailTask(content);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "실패 작업이 전송되었습니다. (의도적 실패)");
        response.put("content", content);
        response.put("shouldFail", true);
        response.put("note", "이 작업은 Main Queue에서 처리 실패 후 DLQ로 이동합니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 커스텀 작업 전송
     */
    @PostMapping("/task")
    public ResponseEntity<Map<String, Object>> sendTask(@RequestBody TaskMessage taskMessage) {
        taskProducer.sendTask(taskMessage);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "작업이 전송되었습니다.");
        response.put("taskId", taskMessage.getTaskId());

        return ResponseEntity.ok(response);
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("DLQ Service is running!");
    }

    /**
     * DLQ 설명 API
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getDlqInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("title", "Dead Letter Queue (DLQ) 설명");
        info.put("description", "처리 실패한 메시지가 이동하는 특별한 큐");

        Map<String, String> flow = new HashMap<>();
        flow.put("1", "Producer가 Main Queue로 메시지 전송");
        flow.put("2", "Consumer가 메시지 처리 시도");
        flow.put("3", "처리 실패 시 재시도 (최대 3회)");
        flow.put("4", "재시도 실패 시 DLQ로 자동 이동");
        flow.put("5", "DLQ Consumer가 재처리 시도");
        flow.put("6", "재처리 실패 시 Parking Lot으로 이동");
        flow.put("7", "Parking Lot에서 수동 처리 대기");

        info.put("flow", flow);

        return ResponseEntity.ok(info);
    }
}
