package com.example.async.controller;

import com.example.async.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@Slf4j
public class AsyncDemoController {

    private final EmailService emailService;

    /**
     * 동기 방식 데모 - 순차적 실행
     * GET /api/demo/sync?email=test@example.com
     * 약 9초 소요 (3개의 작업 각 3초)
     */
    @GetMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncDemo(@RequestParam String email) {
        log.info("Starting synchronous demo for: {}", email);
        long startTime = System.currentTimeMillis();

        // 작업 1: 환영 이메일 (3초)
        sendEmailSync(email, "Welcome", "Welcome to our service!");

        // 작업 2: 확인 이메일 (3초)
        sendEmailSync(email, "Verification", "Please verify your email");

        // 작업 3: 프로모션 이메일 (3초)
        sendEmailSync(email, "Promotion", "Check out our latest offers!");

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("method", "synchronous");
        response.put("email", email);
        response.put("tasks", 3);
        response.put("duration_ms", duration);

        log.info("Synchronous demo completed in {}ms", duration);
        return ResponseEntity.ok(response);
    }

    /**
     * 비동기 방식 데모 - 병렬 실행
     * GET /api/demo/async?email=test@example.com
     * 약 3초 소요 (3개의 작업이 병렬로 실행)
     */
    @GetMapping("/async")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> asyncDemo(@RequestParam String email) {
        log.info("Starting asynchronous demo for: {}", email);
        long startTime = System.currentTimeMillis();

        // 3개의 이메일을 병렬로 전송
        CompletableFuture<Boolean> future1 = emailService.sendEmailWithResult(
                email, "Welcome", "Welcome to our service!");
        CompletableFuture<Boolean> future2 = emailService.sendEmailWithResult(
                email, "Verification", "Please verify your email");
        CompletableFuture<Boolean> future3 = emailService.sendEmailWithResult(
                email, "Promotion", "Check out our latest offers!");

        // 모든 작업이 완료될 때까지 대기
        return CompletableFuture.allOf(future1, future2, future3)
                .thenApply(v -> {
                    long duration = System.currentTimeMillis() - startTime;

                    Map<String, Object> response = new HashMap<>();
                    response.put("method", "asynchronous");
                    response.put("email", email);
                    response.put("tasks", 3);
                    response.put("duration_ms", duration);
                    response.put("results", Map.of(
                            "welcome", future1.join(),
                            "verification", future2.join(),
                            "promotion", future3.join()
                    ));

                    log.info("Asynchronous demo completed in {}ms", duration);
                    return ResponseEntity.ok(response);
                });
    }

    /**
     * 대량 이메일 전송 데모
     * GET /api/demo/bulk?count=5
     */
    @GetMapping("/bulk")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> bulkDemo(
            @RequestParam(defaultValue = "5") int count) {
        log.info("Starting bulk email demo for {} recipients", count);
        long startTime = System.currentTimeMillis();

        String[] recipients = new String[count];
        for (int i = 0; i < count; i++) {
            recipients[i] = "user" + (i + 1) + "@example.com";
        }

        return emailService.sendBulkEmails(recipients, "Bulk Message", "This is a bulk email")
                .thenApply(successCount -> {
                    long duration = System.currentTimeMillis() - startTime;

                    Map<String, Object> response = new HashMap<>();
                    response.put("method", "bulk_async");
                    response.put("total_recipients", count);
                    response.put("success_count", successCount);
                    response.put("duration_ms", duration);

                    log.info("Bulk demo completed in {}ms", duration);
                    return ResponseEntity.ok(response);
                });
    }

    /**
     * 비동기 체이닝 데모
     * GET /api/demo/chain?email=test@example.com
     */
    @GetMapping("/chain")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> chainDemo(@RequestParam String email) {
        log.info("Starting async chaining demo for: {}", email);
        long startTime = System.currentTimeMillis();

        // 작업 체이닝: 각 작업이 순차적으로 완료됨
        return emailService.sendEmailWithResult(email, "Step 1", "First email")
                .thenCompose(result1 -> {
                    log.info("Step 1 completed: {}", result1);
                    return emailService.sendEmailWithResult(email, "Step 2", "Second email");
                })
                .thenCompose(result2 -> {
                    log.info("Step 2 completed: {}", result2);
                    return emailService.sendEmailWithResult(email, "Step 3", "Third email");
                })
                .thenApply(result3 -> {
                    long duration = System.currentTimeMillis() - startTime;

                    Map<String, Object> response = new HashMap<>();
                    response.put("method", "async_chaining");
                    response.put("email", email);
                    response.put("steps", 3);
                    response.put("duration_ms", duration);
                    response.put("final_result", result3);

                    log.info("Chain demo completed in {}ms", duration);
                    return ResponseEntity.ok(response);
                });
    }

    /**
     * 동기 이메일 전송 헬퍼 메서드
     */
    private void sendEmailSync(String to, String subject, String body) {
        try {
            log.info("Sending sync email to: {} - {}", to, subject);
            Thread.sleep(3000); // 이메일 전송 시뮬레이션
            log.info("Sync email sent to: {}", to);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
