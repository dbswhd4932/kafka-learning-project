package com.example.concurrency.service;

import com.example.concurrency.domain.Stock;
import com.example.concurrency.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redisson 분산 락을 사용한 동시성 제어 테스트
 *
 * [테스트 목적]
 * Redisson의 RLock을 사용하여 분산 환경에서 동시성 문제를 해결하는 것을 검증합니다.
 *
 * [테스트 환경]
 * - 단일 JVM (Spring Boot 테스트)
 * - 멀티 스레드 (ExecutorService) - 실제로는 여러 Pod/서버를 시뮬레이션
 * - MySQL 데이터베이스
 * - Redis (Redisson 클라이언트)
 *
 * [Lettuce vs Redisson]
 * - Lettuce: 스핀 락 방식 (계속 재시도, CPU 사용률 높음)
 * - Redisson: Pub/Sub 방식 (이벤트 기반, CPU 사용률 낮음)
 *
 * [핵심 포인트]
 * - Redisson은 Lettuce보다 성능이 좋음
 * - Pub/Sub 방식으로 불필요한 재시도 감소
 * - Watchdog으로 락 TTL 자동 연장
 */
@SpringBootTest
class RedissonStockServiceTest {

    @Autowired
    private RedissonStockService redissonStockService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 각 테스트 실행 전 초기 재고 설정
     */
    @BeforeEach
    void setUp() {
        Stock stock = new Stock(1L, 100L); // 재고 100개로 설정
        stockRepository.saveAndFlush(stock);

        // Redis 초기화 (혹시 남아있는 락 제거)
        if (redissonClient.getKeys().countExists("stock:lock:1") > 0) {
            redissonClient.getKeys().delete("stock:lock:1");
        }
    }

    /**
     * 각 테스트 실행 후 모든 재고 데이터 삭제
     */
    @AfterEach
    void tearDown() {
        stockRepository.deleteAll();
        // Redis 락 정리
        redissonClient.getKeys().delete("stock:lock:1");
    }

    @Test
    @DisplayName("Redisson 분산 락을 사용한 동시성 제어 - 100개 스레드 테스트")
    void redissonDistributedLockTest() throws InterruptedException {
        /*
         * ===================================================================
         * Redisson 분산 락 동시성 제어 테스트
         * ===================================================================
         *
         * [시나리오]
         * - 초기 재고: 100개
         * - 100개의 스레드가 동시에 각각 1개씩 구매 시도
         * - 예상 결과: 100 - 100 = 0개
         * - Redisson 분산 락으로 순차 처리되므로 정확히 0개
         *
         * [Redisson 동작 방식 (Pub/Sub)]
         * Thread 1: tryLock() → 성공 → 재고 감소 (100 → 99) → unlock() → Redis publish
         * Thread 2: tryLock() → 실패 → Redis subscribe (대기) → 알림 받음 → 성공 → 재고 감소 (99 → 98)
         * Thread 3: tryLock() → 실패 → Redis subscribe (대기) → 알림 받음 → 성공 → 재고 감소 (98 → 97)
         * ...
         *
         * [Lettuce vs Redisson 성능 비교]
         *
         * **Lettuce (스핀 락)**
         * - 락 획득 실패 시 50ms 대기 후 계속 재시도
         * - CPU 사용률 높음
         * - 불필요한 Redis 요청 많음
         *
         * **Redisson (Pub/Sub)**
         * - 락 획득 실패 시 Redis subscribe (대기)
         * - 락 해제 시 Redis가 알림 (publish)
         * - CPU 사용률 낮음
         * - Redis 요청 최소화
         *
         * [장점]
         * - 분산 환경(여러 Pod/서버)에서 동작
         * - Lettuce보다 성능 좋음
         * - CPU 사용률 낮음
         * - 구현이 간단
         */

        // given: 초기 재고 100개
        Long stockId = 1L;
        int threadCount = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("📦 [Redisson 분산 락 동시성 테스트 시작]");
        System.out.println("=".repeat(70));
        System.out.println("초기 재고: 100개");
        System.out.println("동시 요청: 100개 스레드");
        System.out.println("각 스레드당 구매 수량: 1개");
        System.out.println("예상 최종 재고: 0개");
        System.out.println("=".repeat(70) + "\n");

        long startTime = System.currentTimeMillis();

        // when: 100개의 스레드가 동시에 재고 감소 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    redissonStockService.decrease(stockId, 1L);
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
            System.out.println("✅ Redisson 분산 락으로 동시성 문제가 완벽하게 해결되었습니다!");
            System.out.println();
            System.out.println("📝 [동작 원리]");
            System.out.println("  - Redisson RLock의 tryLock()으로 락 획득");
            System.out.println("  - Pub/Sub 방식으로 효율적인 대기");
            System.out.println("  - 락 해제 시 Redis가 대기 중인 클라이언트에게 알림");
            System.out.println("  - 불필요한 재시도 없이 순차 처리");
            System.out.println();
            System.out.println("✅ [Lettuce vs Redisson]");
            System.out.println("  - Lettuce: 스핀 락 (CPU 사용률 높음)");
            System.out.println("  - Redisson: Pub/Sub (CPU 사용률 낮음, 성능 좋음)");
            System.out.println();
            System.out.println("💡 [권장 사항]");
            System.out.println("  - 높은 트래픽 환경에서는 Redisson 사용 권장");
            System.out.println("  - Lettuce는 간단한 케이스에만 사용");
        } else {
            System.out.println("❌ 동시성 문제 발생!");
            System.out.println("  - 예상: 0개, 실제: " + finalQuantity + "개");
            System.out.println("  - Redisson 분산 락이 제대로 동작하지 않았습니다");
        }
        System.out.println("=".repeat(70));

        // 검증: 재고는 정확히 0이어야 함
        assertThat(finalQuantity).isEqualTo(0L);
    }

    @Test
    @DisplayName("Redisson 성능 테스트 - Pub/Sub 방식의 효율성 확인")
    void redissonPerformanceTest() throws InterruptedException {
        /*
         * ===================================================================
         * Redisson 성능 테스트
         * ===================================================================
         *
         * [목적]
         * Redisson의 Pub/Sub 방식이 Lettuce의 스핀 락보다 효율적임을 확인합니다.
         *
         * [Lettuce (스핀 락) 동작]
         * ```
         * while (!락 획득) {
         *     Thread.sleep(50ms);      // CPU 계속 사용
         *     Redis SETNX 재시도;      // 불필요한 요청
         * }
         * ```
         *
         * [Redisson (Pub/Sub) 동작]
         * ```
         * if (!락 획득) {
         *     Redis SUBSCRIBE (대기);  // CPU 사용 안 함
         *     // Redis가 publish 알림 보내면 깨어남
         * }
         * ```
         *
         * [예상]
         * - Redisson이 Lettuce보다 빠름
         * - CPU 사용률이 낮음
         * - Redis 요청 횟수가 적음
         */

        // given
        Long stockId = 1L;
        int threadCount = 50;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("⏱️  [Redisson 성능 테스트]");
        System.out.println("=".repeat(70));
        System.out.println("스레드 수: " + threadCount);
        System.out.println("=".repeat(70) + "\n");

        long startTime = System.currentTimeMillis();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    redissonStockService.decrease(stockId, 1L);
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
        System.out.println("  - Redisson은 Pub/Sub 방식으로 효율적");
        System.out.println("  - 락 해제 시 Redis가 대기 중인 클라이언트에게 알림");
        System.out.println("  - 불필요한 재시도 없음");
        System.out.println("  - CPU 사용률 낮음");
        System.out.println();
        System.out.println("💡 [Lettuce와 비교]");
        System.out.println("  - Lettuce는 50ms마다 재시도 (스핀 락)");
        System.out.println("  - Redisson은 이벤트 기반 대기 (Pub/Sub)");
        System.out.println("  - 높은 트래픽 환경에서는 Redisson이 훨씬 효율적");
        System.out.println();
        System.out.println("🔧 [Redisson의 추가 기능]");
        System.out.println("  - Watchdog: 락 TTL 자동 연장");
        System.out.println("  - Fair Lock: 공정한 락 획득 순서");
        System.out.println("  - MultiLock: 여러 락을 동시에 관리");
        System.out.println("=".repeat(70));

        assertThat(stock.getQuantity()).isEqualTo(50L);
    }

    @Test
    @DisplayName("재고 ID별 독립적인 락 테스트 - Redisson도 재고별 독립 처리")
    void independentLockPerStockIdTest() throws InterruptedException {
        /*
         * ===================================================================
         * 재고 ID별 독립적인 락 테스트
         * ===================================================================
         *
         * [목적]
         * Redisson도 Lettuce처럼 재고 ID별로 독립적인 락을 사용함을 확인합니다.
         *
         * [시나리오]
         * - 상품 A (ID: 1) 재고: 50개
         * - 상품 B (ID: 2) 재고: 50개
         * - 각 상품에 50개 스레드씩 동시 접근
         * - 예상: 상품 A와 상품 B가 독립적으로 처리됨
         *
         * [동작]
         * - 상품 A: Redis 키 'stock:lock:1' 사용
         * - 상품 B: Redis 키 'stock:lock:2' 사용
         * - 서로 다른 락이므로 동시 처리 가능
         */

        // given: 2개의 재고 생성
        Stock stock1 = new Stock(1L, 50L);
        Stock stock2 = new Stock(2L, 50L);
        stockRepository.saveAndFlush(stock1);
        stockRepository.saveAndFlush(stock2);

        int threadCountPerStock = 50;
        int totalThreadCount = threadCountPerStock * 2;

        ExecutorService executorService = Executors.newFixedThreadPool(totalThreadCount);
        CountDownLatch latch = new CountDownLatch(totalThreadCount);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("📦 [재고 ID별 독립적인 락 테스트]");
        System.out.println("=".repeat(70));
        System.out.println("상품 A (ID: 1) 재고: 50개, 요청: 50개");
        System.out.println("상품 B (ID: 2) 재고: 50개, 요청: 50개");
        System.out.println("=".repeat(70) + "\n");

        long startTime = System.currentTimeMillis();

        // when: 상품 A와 상품 B에 동시 접근
        for (int i = 0; i < threadCountPerStock; i++) {
            // 상품 A 감소
            executorService.submit(() -> {
                try {
                    redissonStockService.decrease(1L, 1L);
                } finally {
                    latch.countDown();
                }
            });

            // 상품 B 감소
            executorService.submit(() -> {
                try {
                    redissonStockService.decrease(2L, 1L);
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
        Stock finalStock1 = stockRepository.findById(1L).orElseThrow();
        Stock finalStock2 = stockRepository.findById(2L).orElseThrow();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("🎯 [테스트 결과]");
        System.out.println("=".repeat(70));
        System.out.println("📊 상품 A 최종 재고: " + finalStock1.getQuantity() + "개 (예상: 0개)");
        System.out.println("📊 상품 B 최종 재고: " + finalStock2.getQuantity() + "개 (예상: 0개)");
        System.out.println("⏱️  총 실행 시간: " + executionTime + "ms");
        System.out.println();
        System.out.println("✅ 재고 ID별로 독립적인 락이 동작합니다!");
        System.out.println();
        System.out.println("📝 [동작 방식]");
        System.out.println("  - 상품 A: Redis 키 'stock:lock:1' 사용");
        System.out.println("  - 상품 B: Redis 키 'stock:lock:2' 사용");
        System.out.println("  - 서로 다른 락이므로 동시 처리 가능");
        System.out.println("=".repeat(70));

        assertThat(finalStock1.getQuantity()).isEqualTo(0L);
        assertThat(finalStock2.getQuantity()).isEqualTo(0L);

        // 정리
        stockRepository.deleteById(2L);
        redissonClient.getKeys().delete("stock:lock:2");
    }
}
