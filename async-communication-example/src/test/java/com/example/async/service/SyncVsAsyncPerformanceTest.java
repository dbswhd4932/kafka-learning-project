package com.example.async.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Slf4j
class SyncVsAsyncPerformanceTest {

    @Autowired
    private PerformanceTestService performanceTestService;

    @Test
    @DisplayName("동기 방식 성능 테스트 - 5개 작업 순차 실행")
    void testSynchronousPerformance() {
        log.info("=== 동기 방식 성능 테스트 시작 ===");

        long startTime = System.currentTimeMillis();

        // 5개 작업을 순차적으로 실행 (각 작업 1초)
        for (int i = 1; i <= 5; i++) {
            performanceTestService.syncTask("Task-" + i);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("=== 동기 방식 성능 테스트 완료 ===");
        log.info("총 소요 시간: {}ms ({}초)", duration, duration / 1000.0);
        log.info("예상 시간: 약 5000ms (5초)");
        log.info("실제 소요 시간: {}ms", duration);

        // 5개 작업 * 1초 = 약 5초 소요 (5000ms ~ 5500ms)
        assertTrue(duration >= 5000, "동기 방식은 최소 5초 이상 소요되어야 함");
        assertTrue(duration < 6000, "동기 방식은 6초 미만이어야 함");

        log.info("✅ 동기 방식 검증 완료: 순차 실행으로 {}ms 소요", duration);
    }

    @Test
    @DisplayName("비동기 방식 성능 테스트 - 5개 작업 병렬 실행")
    void testAsynchronousPerformance() {
        log.info("=== 비동기 방식 성능 테스트 시작 ===");

        long startTime = System.currentTimeMillis();

        // 5개 작업을 비동기로 동시 실행 (각 작업 1초, 병렬 처리)
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            CompletableFuture<String> future = performanceTestService.asyncTask("Task-" + i);
            futures.add(future);
        }

        // 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );
        allFutures.join(); // 모든 작업 완료 대기

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("=== 비동기 방식 성능 테스트 완료 ===");
        log.info("총 소요 시간: {}ms ({}초)", duration, duration / 1000.0);
        log.info("예상 시간: 약 1000ms (1초) - 병렬 실행");
        log.info("실제 소요 시간: {}ms", duration);

        // 병렬 실행으로 약 1초만 소요 (1000ms ~ 2000ms)
        assertTrue(duration >= 1000, "비동기 방식은 최소 1초 이상 소요되어야 함");
        assertTrue(duration < 2000, "비동기 방식은 2초 미만이어야 함 (병렬 실행)");

        log.info("✅ 비동기 방식 검증 완료: 병렬 실행으로 {}ms 소요", duration);
    }

    @Test
    @DisplayName("동기 vs 비동기 성능 비교 - 10개 작업")
    void testSyncVsAsyncComparison() {
        log.info("\n" + "=".repeat(60));
        log.info("동기 vs 비동기 성능 비교 테스트 시작");
        log.info("=".repeat(60));

        int taskCount = 10;

        // 1. 동기 방식 테스트
        log.info("\n[1] 동기 방식 - {}개 작업 순차 실행", taskCount);
        long syncStartTime = System.currentTimeMillis();

        for (int i = 1; i <= taskCount; i++) {
            performanceTestService.syncTask("Sync-Task-" + i);
        }

        long syncEndTime = System.currentTimeMillis();
        long syncDuration = syncEndTime - syncStartTime;

        log.info("동기 방식 소요 시간: {}ms ({}초)", syncDuration, syncDuration / 1000.0);

        // 2. 비동기 방식 테스트
        log.info("\n[2] 비동기 방식 - {}개 작업 병렬 실행", taskCount);
        long asyncStartTime = System.currentTimeMillis();

        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 1; i <= taskCount; i++) {
            futures.add(performanceTestService.asyncTask("Async-Task-" + i));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long asyncEndTime = System.currentTimeMillis();
        long asyncDuration = asyncEndTime - asyncStartTime;

        log.info("비동기 방식 소요 시간: {}ms ({}초)", asyncDuration, asyncDuration / 1000.0);

        // 3. 성능 비교 결과
        long timeSaved = syncDuration - asyncDuration;
        double improvementPercent = ((double) timeSaved / syncDuration) * 100;

        log.info("\n" + "=".repeat(60));
        log.info("📊 성능 비교 결과");
        log.info("=".repeat(60));
        log.info("작업 개수: {}개 (각 작업당 1초 소요)", taskCount);
        log.info("동기 방식 소요 시간:   {}ms ({}초)", syncDuration, syncDuration / 1000.0);
        log.info("비동기 방식 소요 시간: {}ms ({}초)", asyncDuration, asyncDuration / 1000.0);
        log.info("단축된 시간: {}ms ({}초)", timeSaved, timeSaved / 1000.0);
        log.info("성능 개선율: {:.2f}%", improvementPercent);
        log.info("=".repeat(60));

        // 검증
        assertTrue(syncDuration >= taskCount * 1000, "동기 방식은 작업 수 * 1초 이상 소요");
        assertTrue(asyncDuration < syncDuration / 2, "비동기 방식은 동기 방식의 절반 미만 소요");

        log.info("✅ 성능 비교 검증 완료");
        log.info("   - 동기는 {}개 작업을 순차 실행하여 약 {}초 소요", taskCount, taskCount);
        log.info("   - 비동기는 {}개 작업을 병렬 실행하여 약 1-2초 소요", taskCount);
        log.info("   - 비동기 방식이 약 {:.2f}% 더 빠름\n", improvementPercent);
    }
}
