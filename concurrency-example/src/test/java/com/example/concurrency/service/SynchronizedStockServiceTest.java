package com.example.concurrency.service;

import com.example.concurrency.domain.Stock;
import com.example.concurrency.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Synchronized를 사용한 동시성 제어 테스트
 *
 * [테스트 목적]
 * synchronized 키워드를 사용하여 단일 JVM 환경에서 동시성 문제를 해결하는 것을 검증합니다.
 *
 * [테스트 환경]
 * - 단일 JVM (Spring Boot 테스트)
 * - 멀티 스레드 (ExecutorService)
 * - MySQL 데이터베이스
 *
 * [핵심 포인트]
 * - synchronized는 단일 서버(JVM) 환경에서만 동작
 * - 여러 Pod/서버가 있는 분산 환경에서는 사용 불가
 */
@SpringBootTest
class SynchronizedStockServiceTest {

    @Autowired
    private SynchronizedStockService synchronizedStockService;

    @Autowired
    private StockRepository stockRepository;

    /**
     * 각 테스트 실행 전 초기 재고 설정
     */
    @BeforeEach
    void setUp() {
        Stock stock = new Stock(1L, 100L); // 재고 100개로 설정
        stockRepository.saveAndFlush(stock);
    }

    /**
     * 각 테스트 실행 후 모든 재고 데이터 삭제
     */
    @AfterEach
    void tearDown() {
        stockRepository.deleteAll();
    }

    @Test
    @DisplayName("synchronized를 사용한 동시성 제어 - 100개 스레드 테스트")
    void synchronizedConcurrencyTest() throws InterruptedException {
        /*
         * ===================================================================
         * Synchronized 동시성 제어 테스트
         * ===================================================================
         *
         * [시나리오]
         * - 초기 재고: 100개
         * - 100개의 스레드가 동시에 각각 1개씩 구매 시도
         * - 예상 결과: 100 - 100 = 0개
         * - synchronized로 순차 처리되므로 정확히 0개
         *
         * [synchronized 동작 방식]
         * Thread 1: 락 획득 → 재고 감소 (100 → 99) → 락 해제
         * Thread 2: 대기... → 락 획득 → 재고 감소 (99 → 98) → 락 해제
         * Thread 3: 대기... → 락 획득 → 재고 감소 (98 → 97) → 락 해제
         * ...
         * Thread 100: 대기... → 락 획득 → 재고 감소 (1 → 0) → 락 해제
         *
         * [장점]
         * - 단일 JVM 환경에서 100% 동시성 문제 해결
         * - 구현이 매우 간단함
         * - 추가 인프라 불필요
         *
         * [단점]
         * - 모든 스레드가 순차 처리되므로 성능 저하
         * - 분산 환경(여러 Pod/서버)에서는 동작하지 않음
         */

        // given: 초기 재고 100개
        Long stockId = 1L;
        int threadCount = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //System.out.println("\n" + "=".repeat(70));
        System.out.println("📦 [Synchronized 동시성 테스트 시작]");
        //System.out.println("=".repeat(70));
        System.out.println("초기 재고: 100개");
        System.out.println("동시 요청: 100개 스레드");
        System.out.println("각 스레드당 구매 수량: 1개");
        System.out.println("예상 최종 재고: 0개");
        //System.out.println("=".repeat(70) + "\n");

        long startTime = System.currentTimeMillis();

        // when: 100개의 스레드가 동시에 재고 감소 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    synchronizedStockService.decrease(stockId, 1L);
                } catch (Exception e) {
                    System.out.println("예외 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 완료 대기
        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // then: 최종 재고 확인
        Stock stock = stockRepository.findById(stockId).orElseThrow();
        Long finalQuantity = stock.getQuantity();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("🎯 [테스트 결과]");
        System.out.println("=".repeat(70));
        System.out.println("📊 예상 재고: 0개");
        System.out.println("📊 실제 재고: " + finalQuantity + "개");
        System.out.println("⏱️  실행 시간: " + executionTime + "ms");
        System.out.println();

        if (finalQuantity == 0) {
            System.out.println("✅ synchronized로 동시성 문제가 완벽하게 해결되었습니다!");
            System.out.println();
            System.out.println("📝 [동작 원리]");
            System.out.println("  - 모든 스레드가 순차적으로 실행됨");
            System.out.println("  - 한 스레드가 메서드 실행 중일 때 다른 스레드는 대기");
            System.out.println("  - 락 획득 → 재고 감소 → DB 저장 → 락 해제 순서로 진행");
            System.out.println();
            System.out.println("⚠️  [주의사항]");
            System.out.println("  - 단일 JVM(서버)에서만 동작합니다");
            System.out.println("  - Kubernetes Pod가 여러 개면 동작하지 않습니다");
            System.out.println("  - Auto Scaling 환경에서는 사용할 수 없습니다");
            System.out.println("  - 분산 환경에서는 Redis 분산 락을 사용해야 합니다");
        } else {
            System.out.println("❌ 동시성 문제 발생!");
            System.out.println("  - 예상: 0개, 실제: " + finalQuantity + "개");
            System.out.println("  - synchronized가 제대로 동작하지 않았습니다");
        }
        System.out.println("=".repeat(70));

        // 검증: 재고는 정확히 0이어야 함
        assertThat(finalQuantity).isEqualTo(0L);
    }

    @Test
    @DisplayName("synchronized 성능 테스트 - 순차 처리로 인한 성능 저하 확인")
    void synchronizedPerformanceTest() throws InterruptedException {
        /*
         * ===================================================================
         * Synchronized 성능 테스트
         * ===================================================================
         *
         * [목적]
         * synchronized의 순차 처리로 인한 성능 저하를 확인합니다.
         *
         * [예상]
         * - 모든 스레드가 순차적으로 실행되므로 실행 시간이 오래 걸림
         * - 100개 스레드 × (DB 조회 + 업데이트 시간) = 긴 실행 시간
         *
         * [개선 방안]
         * - 재고 ID별로 독립적인 락 설정 (synchronized 블록 + Map<Long, Object>)
         * - 데이터베이스 락 사용 (Pessimistic Lock)
         * - Redis 분산 락 사용 (재고 ID별 독립적인 락)
         */

        // given
        Long stockId = 1L;
        int threadCount = 50;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("⏱️  [Synchronized 성능 테스트]");
        System.out.println("=".repeat(70));
        System.out.println("스레드 수: " + threadCount);
        System.out.println("=".repeat(70) + "\n");

        long startTime = System.currentTimeMillis();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    synchronizedStockService.decrease(stockId, 1L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // then
        Stock stock = stockRepository.findById(stockId).orElseThrow();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("🎯 [성능 테스트 결과]");
        System.out.println("=".repeat(70));
        System.out.println("⏱️  총 실행 시간: " + executionTime + "ms");
        System.out.println("📊 스레드당 평균 시간: " + (executionTime / threadCount) + "ms");
        System.out.println("📊 최종 재고: " + stock.getQuantity() + "개");
        System.out.println();
        System.out.println("📝 [성능 분석]");
        System.out.println("  - synchronized는 메서드 전체를 락으로 보호");
        System.out.println("  - 모든 요청이 순차적으로 처리됨");
        System.out.println("  - 재고 ID가 달라도 대기해야 함 (성능 저하)");
        System.out.println();
        System.out.println("💡 [개선 방안]");
        System.out.println("  - 재고 ID별로 독립적인 락 설정");
        System.out.println("  - Redis 분산 락 사용 (RedisStockService 참고)");
        System.out.println("=".repeat(70));

        assertThat(stock.getQuantity()).isEqualTo(50L);
    }
}
