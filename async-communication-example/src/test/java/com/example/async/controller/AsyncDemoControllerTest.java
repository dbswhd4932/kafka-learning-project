package com.example.async.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class AsyncDemoControllerTest {

    @Autowired
    private AsyncDemoController asyncDemoController;

    @Test
    @DisplayName("비동기 데모 API 테스트 - 3개 작업 병렬 실행으로 약 3초 소요")
    void testAsyncDemo() throws ExecutionException, InterruptedException {
        // Given
        String testEmail = "test@example.com";
        log.info("=== 비동기 데모 API 테스트 시작 ===");
        log.info("테스트 이메일: {}", testEmail);

        // When
        long startTime = System.currentTimeMillis();

        CompletableFuture<ResponseEntity<Map<String, Object>>> futureResponse =
                asyncDemoController.asyncDemo(testEmail);

        // 비동기 작업 완료 대기
        ResponseEntity<Map<String, Object>> response = futureResponse.get();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then
        log.info("=== 비동기 데모 API 응답 확인 ===");

        // 1. HTTP 상태 코드 검증
        assertEquals(200, response.getStatusCodeValue(), "HTTP 상태 코드는 200이어야 함");
        log.info("✅ HTTP 상태 코드: {}", response.getStatusCodeValue());

        // 2. 응답 본문 검증
        Map<String, Object> body = response.getBody();
        assertNotNull(body, "응답 본문이 null이 아니어야 함");

        // 3. 응답 데이터 검증
        assertEquals("asynchronous", body.get("method"), "method는 'asynchronous'이어야 함");
        assertEquals(testEmail, body.get("email"), "email이 일치해야 함");
        assertEquals(3, body.get("tasks"), "tasks는 3이어야 함");

        log.info("✅ method: {}", body.get("method"));
        log.info("✅ email: {}", body.get("email"));
        log.info("✅ tasks: {}", body.get("tasks"));

        // 4. 응답 시간 검증 (3개 작업이 병렬로 실행되므로 약 3초)
        Object durationMs = body.get("duration_ms");
        assertNotNull(durationMs, "duration_ms가 존재해야 함");

        long apiDuration = ((Number) durationMs).longValue();
        log.info("✅ API 내부 소요 시간: {}ms ({}초)", apiDuration, apiDuration / 1000.0);
        log.info("✅ 전체 테스트 소요 시간: {}ms ({}초)", duration, duration / 1000.0);

        // 병렬 실행으로 3000ms ~ 4000ms 사이에 완료되어야 함
        assertTrue(apiDuration >= 3000,
                "비동기 방식은 최소 3초 이상 소요되어야 함 (실제: " + apiDuration + "ms)");
        assertTrue(apiDuration < 5000,
                "비동기 방식은 5초 미만이어야 함 (병렬 실행) (실제: " + apiDuration + "ms)");

        // 5. 결과 데이터 검증
        @SuppressWarnings("unchecked")
        Map<String, Boolean> results = (Map<String, Boolean>) body.get("results");
        assertNotNull(results, "results가 존재해야 함");

        assertTrue(results.containsKey("welcome"), "welcome 결과가 있어야 함");
        assertTrue(results.containsKey("verification"), "verification 결과가 있어야 함");
        assertTrue(results.containsKey("promotion"), "promotion 결과가 있어야 함");

        assertTrue(results.get("welcome"), "welcome 이메일 전송 성공해야 함");
        assertTrue(results.get("verification"), "verification 이메일 전송 성공해야 함");
        assertTrue(results.get("promotion"), "promotion 이메일 전송 성공해야 함");

        log.info("✅ 이메일 전송 결과:");
        log.info("   - welcome: {}", results.get("welcome"));
        log.info("   - verification: {}", results.get("verification"));
        log.info("   - promotion: {}", results.get("promotion"));

        // 6. 성능 검증 요약
        log.info("\n" + "=".repeat(60));
        log.info("📊 비동기 데모 API 성능 검증 결과");
        log.info("=".repeat(60));
        log.info("작업 개수: 3개 (각 작업당 3초 소요)");
        log.info("실행 방식: 병렬 실행 (비동기)");
        log.info("API 내부 소요 시간: {}ms ({}초)", apiDuration, apiDuration / 1000.0);
        log.info("전체 테스트 소요 시간: {}ms ({}초)", duration, duration / 1000.0);
        log.info("예상 시간: 약 3초 (순차 실행 시 9초)");
        log.info("성능 개선: 약 {}% 빠름", ((9000 - apiDuration) * 100 / 9000));
        log.info("=".repeat(60));

        log.info("\n✅ 비동기 데모 API 테스트 완료");
        log.info("   - 3개 작업이 병렬로 실행되어 약 3초에 완료");
        log.info("   - 순차 실행 대비 약 6초 단축 (9초 → 3초)");
    }
}
