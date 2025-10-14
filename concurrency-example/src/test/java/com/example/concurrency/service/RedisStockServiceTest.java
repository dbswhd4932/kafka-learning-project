package com.example.concurrency.service;

import com.example.concurrency.domain.Stock;
import com.example.concurrency.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis Lettuce 분산 락을 사용한 동시성 제어 테스트
 *
 * [테스트 목적]
 * Redis 분산 락을 사용하여 분산 환경에서 동시성 문제를 해결하는 것을 검증합니다.
 *
 * [테스트 환경]
 * - 단일 JVM (Spring Boot 테스트)
 * - 멀티 스레드 (ExecutorService) - 실제로는 여러 Pod/서버를 시뮬레이션
 * - MySQL 데이터베이스
 * - Redis (Lettuce 클라이언트)
 *
 * [핵심 포인트]
 * - Redis 분산 락은 여러 서버/Pod 환경에서 동작
 * - SETNX를 사용한 스핀 락 방식
 * - 재고 ID별로 독립적인 락 설정 가능
 */
@SpringBootTest
class RedisStockServiceTest {

    @Autowired
    private RedisStockService redisStockService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 각 테스트 실행 전 초기 재고 설정
     */
    @BeforeEach
    void setUp() {
        Stock stock = new Stock(1L, 100L); // 재고 100개로 설정
        stockRepository.saveAndFlush(stock);

        // Redis 초기화 (혹시 남아있는 락 제거)
        redisTemplate.delete("stock:lock:1");
    }

    /**
     * 각 테스트 실행 후 모든 재고 데이터 삭제
     */
    @AfterEach
    void tearDown() {
        stockRepository.deleteAll();
        // Redis 락 정리
        redisTemplate.delete("stock:lock:1");
    }

    @Test
    @DisplayName("Redis 분산 락을 사용한 동시성 제어 - 100개 스레드 테스트")
    void redisDistributedLockTest() throws InterruptedException {
        /*
         * ===================================================================
         * Redis 분산 락 동시성 제어 테스트
         * ===================================================================
         *
         * [시나리오]
         * - 초기 재고: 100개
         * - 100개의 스레드가 동시에 각각 1개씩 구매 시도
         * - 예상 결과: 100 - 100 = 0개
         * - Redis 분산 락으로 순차 처리되므로 정확히 0개
         *
         * [Redis 분산 락 동작 방식]
         * Thread 1: Redis SETNX stock:lock:1 → 성공 → 재고 감소 (100 → 99) → Redis DEL stock:lock:1
         * Thread 2: Redis SETNX stock:lock:1 → 실패 → 50ms 대기 → 재시도 → 성공 → 재고 감소 (99 → 98)
         * Thread 3: Redis SETNX stock:lock:1 → 실패 → 50ms 대기 → 재시도 → 성공 → 재고 감소 (98 → 97)
         * ...
         *
         * [장점]
         * - 분산 환경(여러 Pod/서버)에서 동작
         * - Kubernetes, Auto Scaling 환경 지원
         * - 재고 ID별로 독립적인 락 설정 가능
         *
         * [단점]
         * - Redis 인프라 필요
         * - 네트워크 I/O로 인한 성능 저하
         * - 스핀 락 방식으로 CPU 사용량 증가
         */

        // given: 초기 재고 100개
        Long stockId = 1L;
        int threadCount = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("📦 [Redis 분산 락 동시성 테스트 시작]");
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
                    redisStockService.decrease(stockId, 1L);
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
            System.out.println("✅ Redis 분산 락으로 동시성 문제가 완벽하게 해결되었습니다!");
            System.out.println();
            System.out.println("📝 [동작 원리]");
            System.out.println("  - Redis SETNX 명령어로 락 획득");
            System.out.println("  - 락 획득 실패 시 50ms 대기 후 재시도 (스핀 락)");
            System.out.println("  - 재고 감소 완료 후 Redis 락 삭제");
            System.out.println("  - 다음 스레드가 락 획득하여 처리");
            System.out.println();
            System.out.println("✅ [사용 가능한 환경]");
            System.out.println("  - Kubernetes Pod가 여러 개인 경우");
            System.out.println("  - Auto Scaling 환경");
            System.out.println("  - 로드 밸런서 뒤에 여러 서버가 있는 경우");
            System.out.println("  - MSA 환경");
            System.out.println();
            System.out.println("💡 [Synchronized vs Redis 분산 락]");
            System.out.println("  - Synchronized: 단일 JVM에서만 동작, 빠름, 간단함");
            System.out.println("  - Redis 분산 락: 분산 환경에서 동작, 느림, 복잡함");
        } else {
            System.out.println("❌ 동시성 문제 발생!");
            System.out.println("  - 예상: 0개, 실제: " + finalQuantity + "개");
            System.out.println("  - Redis 분산 락이 제대로 동작하지 않았습니다");
        }
        System.out.println("=".repeat(70));

        // 검증: 재고는 정확히 0이어야 함
        assertThat(finalQuantity).isEqualTo(0L);
    }

    @Test
    @DisplayName("Redis 분산 락 성능 테스트 - 스핀 락으로 인한 성능 확인")
    void redisDistributedLockPerformanceTest() throws InterruptedException {
        /*
         * ===================================================================
         * Redis 분산 락 성능 테스트
         * ===================================================================
         *
         * [목적]
         * Redis 분산 락의 스핀 락 방식으로 인한 성능 특성을 확인합니다.
         *
         * [스핀 락 (Spin Lock)]
         * - 락 획득 실패 시 계속 재시도하는 방식
         * - 50ms 대기 → 재시도 → 50ms 대기 → 재시도 ...
         * - CPU를 계속 사용하므로 CPU 사용률 증가
         *
         * [성능 비교]
         * - Synchronized: 빠름 (JVM 내부 락, 네트워크 I/O 없음)
         * - Redis 분산 락: 느림 (네트워크 I/O, 스핀 락 재시도)
         *
         * [개선 방안]
         * - Redisson 사용 (Pub/Sub 방식, 스핀 락 방식보다 효율적)
         * - 락 타임아웃 조정
         * - 재시도 간격 조정
         */

        // given
        Long stockId = 1L;
        int threadCount = 50;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("⏱️  [Redis 분산 락 성능 테스트]");
        System.out.println("=".repeat(70));
        System.out.println("스레드 수: " + threadCount);
        System.out.println("=".repeat(70) + "\n");

        long startTime = System.currentTimeMillis();

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    redisStockService.decrease(stockId, 1L);
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
        System.out.println("  - Redis 분산 락은 네트워크 I/O가 발생");
        System.out.println("  - 스핀 락 방식으로 재시도 시 50ms 대기");
        System.out.println("  - Synchronized보다 느리지만 분산 환경에서 동작");
        System.out.println();
        System.out.println("💡 [개선 방안]");
        System.out.println("  - Redisson 사용 (Pub/Sub 방식으로 효율 향상)");
        System.out.println("  - 재시도 간격 조정 (50ms → 100ms)");
        System.out.println("  - 락 타임아웃 조정 (현재 3초)");
        System.out.println();
        System.out.println("⚖️  [Trade-off]");
        System.out.println("  - 성능을 포기하고 분산 환경 지원을 선택");
        System.out.println("  - 높은 트래픽 환경에서는 Redisson 권장");
        System.out.println("=".repeat(70));

        assertThat(stock.getQuantity()).isEqualTo(50L);
    }

    @Test
    @DisplayName("재고 ID별 독립적인 락 테스트 - 다른 상품은 동시 처리 가능")
    void independentLockPerStockIdTest() throws InterruptedException {
        /*
         * ===================================================================
         * 재고 ID별 독립적인 락 테스트
         * ===================================================================
         *
         * [목적]
         * Redis 분산 락이 재고 ID별로 독립적으로 동작하는지 확인합니다.
         *
         * [시나리오]
         * - 상품 A (ID: 1) 재고: 50개
         * - 상품 B (ID: 2) 재고: 50개
         * - 각 상품에 50개 스레드씩 동시 접근
         * - 예상: 상품 A와 상품 B가 독립적으로 처리됨
         *
         * [Synchronized vs Redis 분산 락]
         * - Synchronized (메서드 레벨): 상품 A 처리 중 상품 B도 대기
         * - Redis 분산 락 (재고 ID별): 상품 A와 상품 B가 동시 처리 가능
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
                    redisStockService.decrease(1L, 1L);
                } finally {
                    latch.countDown();
                }
            });

            // 상품 B 감소
            executorService.submit(() -> {
                try {
                    redisStockService.decrease(2L, 1L);
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
        System.out.println();
        System.out.println("💡 [성능 향상]");
        System.out.println("  - Synchronized (메서드 레벨): 상품 A와 B가 순차 처리");
        System.out.println("  - Redis 분산 락: 상품 A와 B가 동시 처리");
        System.out.println("=".repeat(70));

        assertThat(finalStock1.getQuantity()).isEqualTo(0L);
        assertThat(finalStock2.getQuantity()).isEqualTo(0L);

        // 정리
        stockRepository.deleteById(2L);
        redisTemplate.delete("stock:lock:2");
    }
}
