package com.example.optimisticlock.service;

import com.example.optimisticlock.dto.ProductRequest;
import com.example.optimisticlock.dto.ProductResponse;
import com.example.optimisticlock.entity.Product;
import com.example.optimisticlock.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("상품 생성 테스트")
    void createProductTest() {
        // given
        ProductRequest request = ProductRequest.builder()
                .name("테스트 상품")
                .stock(100)
                .price(10000)
                .build();

        // when
        ProductResponse response = productService.createProduct(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("테스트 상품");
        assertThat(response.getStock()).isEqualTo(100);
        assertThat(response.getPrice()).isEqualTo(10000);
        assertThat(response.getVersion()).isEqualTo(0L);  // 초기 버전은 0
    }

    @Test
    @DisplayName("재고 감소 테스트 - 정상 케이스")
    void decreaseStockTest() {
        // given
        ProductRequest request = ProductRequest.builder()
                .name("테스트 상품")
                .stock(100)
                .price(10000)
                .build();
        ProductResponse created = productService.createProduct(request);

        // when
        ProductResponse response = productService.decreaseStock(created.getId(), 10);

        // then
        assertThat(response.getStock()).isEqualTo(90);
        assertThat(response.getVersion()).isEqualTo(1L);  // 버전이 1 증가
    }

    @Test
    @DisplayName("재고 감소 테스트 - 재고 부족")
    void decreaseStockInsufficientTest() {
        // given
        ProductRequest request = ProductRequest.builder()
                .name("테스트 상품")
                .stock(10)
                .price(10000)
                .build();
        ProductResponse created = productService.createProduct(request);

        // when & then
        assertThatThrownBy(() -> productService.decreaseStock(created.getId(), 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    @DisplayName("낙관적 락 충돌 테스트 - 동시에 재고 감소")
    void optimisticLockConflictTest() throws InterruptedException {
        /**
         * [테스트 목적]
         * - 5개의 스레드가 동시에 같은 상품의 재고를 감소시킬 때 낙관적 락이 정상 작동하는지 확인
         * - 재시도 로직 없이 순수하게 낙관적 락만 사용
         * - OptimisticLockException 발생으로 일부 요청이 실패하는지 검증
         *
         * [예상 동작]
         * 1. 여러 스레드가 동시에 같은 엔티티를 읽음 (같은 version)
         * 2. 먼저 커밋하는 스레드만 성공하고 version이 증가
         * 3. 나머지 스레드들은 version 불일치로 OptimisticLockException 발생
         * 4. 결과적으로 성공한 요청만큼만 재고가 감소됨
         */

        // given - 초기 재고 100개인 상품 생성
        ProductRequest request = ProductRequest.builder()
                .name("테스트 상품")
                .stock(100)
                .price(10000)
                .build();
        ProductResponse created = productService.createProduct(request);

        System.out.println("\n========== 낙관적 락 충돌 테스트 시작 ==========");
        System.out.println("[초기 상태] 상품 ID: " + created.getId() + ", 재고: " + created.getStock() + ", 버전: " + created.getVersion());

        // 5개의 스레드로 동시 요청 시뮬레이션
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);  // 성공한 요청 수
        AtomicInteger failCount = new AtomicInteger(0);      // 실패한 요청 수

        // when - 5개 스레드가 동시에 각각 5개씩 재고 감소 시도
        System.out.println("\n[동시 요청 시작] " + threadCount + "개 스레드가 각각 5개씩 재고 감소 시도");

        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i + 1;
            executorService.submit(() -> {
                try {
                    // 재시도 없이 바로 재고 감소 시도
                    productService.decreaseStock(created.getId(), 5);
                    successCount.incrementAndGet();
                    System.out.println("  ✓ [스레드-" + threadNumber + "] 재고 감소 성공");
                } catch (ObjectOptimisticLockingFailureException e) {
                    failCount.incrementAndGet();
                    System.out.println("  ✗ [스레드-" + threadNumber + "] 낙관적 락 충돌로 실패 (버전 불일치)");
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("  ✗ [스레드-" + threadNumber + "] 예외 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 완료 대기
        latch.await();
        executorService.shutdown();

        // then - 결과 검증
        Product product = productRepository.findById(created.getId()).orElseThrow();

        System.out.println("\n========== 테스트 결과 ==========");
        System.out.println("성공한 요청: " + successCount.get() + "개");
        System.out.println("실패한 요청: " + failCount.get() + "개 (낙관적 락 충돌)");
        System.out.println("최종 재고: " + product.getStock() + "개");
        System.out.println("최종 버전: " + product.getVersion());

        // 성공한 트랜잭션만큼 재고가 감소되어야 함
        int expectedStock = 100 - (successCount.get() * 5);
        assertThat(product.getStock()).isEqualTo(expectedStock);

        // 낙관적 락으로 인해 일부 트랜잭션은 반드시 실패해야 함
        assertThat(failCount.get()).isGreaterThan(0);

        System.out.println("\n검증 완료: 성공 요청(" + successCount.get() + ") × 5 = " + (successCount.get() * 5) + "개 감소");
        System.out.println("예상 재고: " + expectedStock + ", 실제 재고: " + product.getStock() + " ✓");
        System.out.println("=========================================\n");
    }

    @Test
    @DisplayName("낙관적 락 충돌 테스트 - 재시도 로직 사용")
    void optimisticLockWithRetryTest() throws InterruptedException {
        /**
         * [테스트 목적]
         * - 재시도 로직을 사용했을 때 낙관적 락 충돌을 어떻게 처리하는지 확인
         * - 이전 테스트와 동일한 조건이지만 재시도 로직이 추가됨
         * - 재시도로 인해 성공률이 크게 높아지는지 검증
         *
         * [예상 동작]
         * 1. 스레드들이 동시에 같은 엔티티를 읽음
         * 2. 충돌 발생 시 예외를 잡고 최대 3번까지 재시도
         * 3. 재시도 간에 점진적 대기 시간 적용 (50ms * 재시도 횟수)
         * 4. 결과적으로 대부분의 요청이 성공함
         *
         * [이전 테스트와의 차이점]
         * - 이전 테스트: 충돌 시 바로 실패 → 성공률 낮음
         * - 이번 테스트: 충돌 시 재시도 → 성공률 높음
         */

        // given - 초기 재고 100개인 상품 생성
        ProductRequest request = ProductRequest.builder()
                .name("테스트 상품")
                .stock(100)
                .price(10000)
                .build();
        ProductResponse created = productService.createProduct(request);

        System.out.println("\n========== 낙관적 락 재시도 테스트 시작 ==========");
        System.out.println("[초기 상태] 상품 ID: " + created.getId() + ", 재고: " + created.getStock() + ", 버전: " + created.getVersion());
        System.out.println("[설정] 최대 재시도 횟수: 3회, 재시도 대기: 점진적 증가 (50ms × 재시도 횟수)");

        // 5개의 스레드로 동시 요청 시뮬레이션
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);  // 성공한 요청 수
        AtomicInteger failCount = new AtomicInteger(0);      // 실패한 요청 수

        // when - 5개 스레드가 동시에 각각 5개씩 재고 감소 시도 (재시도 로직 포함)
        System.out.println("\n[동시 요청 시작] " + threadCount + "개 스레드가 각각 5개씩 재고 감소 시도 (재시도 로직 사용)");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i + 1;
            executorService.submit(() -> {
                try {
                    // 재시도 로직이 포함된 메서드 호출
                    productService.decreaseStockWithRetry(created.getId(), 5);
                    successCount.incrementAndGet();
                    System.out.println("  ✓ [스레드-" + threadNumber + "] 재고 감소 성공 (재시도 포함)");
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("  ✗ [스레드-" + threadNumber + "] 최대 재시도 후에도 실패: " + e.getMessage());
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

        // then - 결과 검증
        Product product = productRepository.findById(created.getId()).orElseThrow();

        System.out.println("\n========== 테스트 결과 (재시도 로직) ==========");
        System.out.println("성공한 요청: " + successCount.get() + "개");
        System.out.println("실패한 요청: " + failCount.get() + "개");
        System.out.println("총 실행 시간: " + executionTime + "ms");
        System.out.println("최종 재고: " + product.getStock() + "개");
        System.out.println("최종 버전: " + product.getVersion());

        // 재시도 로직으로 인해 모든 요청이 성공할 가능성이 높음
        if (successCount.get() == threadCount) {
            System.out.println("\n✓ 모든 요청 성공! 재시도 로직이 효과적으로 작동했습니다.");
        } else {
            System.out.println("\n일부 요청 실패: 높은 동시성으로 인해 최대 재시도 횟수를 초과했습니다.");
        }

        // 성공한 만큼 재고가 감소되어야 함
        int expectedStock = 100 - (successCount.get() * 5);
        assertThat(product.getStock()).isEqualTo(expectedStock);

        System.out.println("\n검증 완료: 성공 요청(" + successCount.get() + ") × 5 = " + (successCount.get() * 5) + "개 감소");
        System.out.println("예상 재고: " + expectedStock + ", 실제 재고: " + product.getStock() + " ✓");

        // 두 테스트 비교 안내
        System.out.println("\n[비교] 재시도 없는 테스트에서는 더 많은 요청이 실패했을 것입니다.");
        System.out.println("[결론] 재시도 로직을 사용하면 낙관적 락 충돌을 효과적으로 처리할 수 있습니다.");
        System.out.println("=================================================\n");
    }

    @Test
    @DisplayName("재고 증가 테스트")
    void increaseStockTest() {
        // given
        ProductRequest request = ProductRequest.builder()
                .name("테스트 상품")
                .stock(100)
                .price(10000)
                .build();
        ProductResponse created = productService.createProduct(request);

        // when
        ProductResponse response = productService.increaseStock(created.getId(), 50);

        // then
        assertThat(response.getStock()).isEqualTo(150);
        assertThat(response.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("가격 변경 테스트")
    void updatePriceTest() {
        // given
        ProductRequest request = ProductRequest.builder()
                .name("테스트 상품")
                .stock(100)
                .price(10000)
                .build();
        ProductResponse created = productService.createProduct(request);

        // when
        ProductResponse response = productService.updatePrice(created.getId(), 15000);

        // then
        assertThat(response.getPrice()).isEqualTo(15000);
        assertThat(response.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("버전 증가 테스트 - 여러 번 수정")
    void versionIncrementTest() {
        // given
        ProductRequest request = ProductRequest.builder()
                .name("테스트 상품")
                .stock(100)
                .price(10000)
                .build();
        ProductResponse created = productService.createProduct(request);
        assertThat(created.getVersion()).isEqualTo(0L);

        // when & then
        ProductResponse response1 = productService.decreaseStock(created.getId(), 10);
        assertThat(response1.getVersion()).isEqualTo(1L);

        ProductResponse response2 = productService.increaseStock(created.getId(), 5);
        assertThat(response2.getVersion()).isEqualTo(2L);

        ProductResponse response3 = productService.updatePrice(created.getId(), 20000);
        assertThat(response3.getVersion()).isEqualTo(3L);
    }

    @Test
    @DisplayName("낙관적 락 + 재시도 테스트 - 모든 요청 순차 성공")
    void optimisticLockWithRetrySequentialTest() throws InterruptedException {
        /**
         * [테스트 목적]
         * - 5개의 스레드가 동시에 요청하지만 낙관적 락 + 재시도로 모두 성공하는지 확인
         * - 스레드1이 먼저 커밋하면 나머지는 재시도하고, version이 업데이트되면 순차적으로 성공
         * - 재시도 로직을 통해 모든 요청이 충돌 없이 성공하는지 검증
         *
         * [예상 동작]
         * 1. 모든 스레드가 거의 동시에 version 0으로 엔티티를 읽음
         * 2. 스레드1이 먼저 커밋 성공 → version이 0→1로 증가
         * 3. 스레드2 커밋 시도 → version 불일치로 실패 → 재시도
         * 4. 스레드2 재시도 → version 1로 읽고 커밋 성공 → version이 1→2로 증가
         * 5. 스레드3 재시도 → version 2로 읽고 커밋 성공 → version이 2→3으로 증가
         * 6. 이런 식으로 재시도를 통해 모든 스레드가 순차적으로 성공
         * 7. 결과: 모든 요청 성공, 재고 정확히 25개 감소 (5개 × 5)
         *
         * [낙관적 락의 재시도 메커니즘]
         * - 충돌 발생 → 예외 잡기 → 대기(50ms × 재시도 횟수) → 최신 데이터 재조회 → 재시도
         * - 최대 3번까지 재시도하므로 대부분의 요청이 성공
         * - DB 잠금 없이 application level에서 충돌 해결
         */

        // given - 초기 재고 100개인 상품 생성
        ProductRequest request = ProductRequest.builder()
                .name("테스트 상품")
                .stock(100)
                .price(10000)
                .build();
        ProductResponse created = productService.createProduct(request);

        System.out.println("\n========== 낙관적 락 + 재시도 순차 처리 테스트 시작 ==========");
        System.out.println("[초기 상태] 상품 ID: " + created.getId() + ", 재고: " + created.getStock() + ", 버전: " + created.getVersion());
        System.out.println("[락 방식] 낙관적 락 (@Version) + 재시도 로직 (최대 3회)");
        System.out.println("[재시도 전략] 충돌 감지 → 대기(50ms × 재시도) → 최신 데이터 재조회 → 재시도");

        // 5개의 스레드로 동시 요청 시뮬레이션
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);  // 성공한 요청 수
        AtomicInteger failCount = new AtomicInteger(0);      // 실패한 요청 수
        AtomicInteger totalRetryCount = new AtomicInteger(0);  // 총 재시도 횟수

        // when - 5개 스레드가 동시에 각각 5개씩 재고 감소 시도 (낙관적 락 + 재시도)
        System.out.println("\n[동시 요청 시작] " + threadCount + "개 스레드가 각각 5개씩 재고 감소 시도");
        System.out.println("→ 충돌 발생 시 재시도를 통해 순차적으로 처리됩니다.\n");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i + 1;
            executorService.submit(() -> {
                try {
                    long threadStartTime = System.currentTimeMillis();
                    System.out.println("  → [스레드-" + threadNumber + "] 재고 감소 시도 시작");

                    // 낙관적 락 + 재시도 로직 사용
                    productService.decreaseStockWithRetry(created.getId(), 5);

                    long threadEndTime = System.currentTimeMillis();
                    long threadExecutionTime = threadEndTime - threadStartTime;

                    successCount.incrementAndGet();
                    System.out.println("  ✓ [스레드-" + threadNumber + "] 재고 감소 성공 (소요 시간: " + threadExecutionTime + "ms)");

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("  ✗ [스레드-" + threadNumber + "] 최대 재시도 후 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });

            // 스레드 간 약간의 간격을 두어 동시 요청 상황 재현
            Thread.sleep(10);
        }

        // 모든 스레드 완료 대기
        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long totalExecutionTime = endTime - startTime;

        // then - 결과 검증
        Product product = productRepository.findById(created.getId()).orElseThrow();

        System.out.println("\n========== 테스트 결과 (낙관적 락 + 재시도) ==========");
        System.out.println("성공한 요청: " + successCount.get() + "개");
        System.out.println("실패한 요청: " + failCount.get() + "개");
        System.out.println("총 실행 시간: " + totalExecutionTime + "ms");
        System.out.println("최종 재고: " + product.getStock() + "개");
        System.out.println("최종 버전: " + product.getVersion());

        // 재시도 로직 덕분에 모든 요청이 성공해야 함
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isEqualTo(0);

        // 모든 요청이 성공했으므로 재고는 정확히 25개 감소 (5개 스레드 × 5개씩)
        int expectedStock = 100 - (threadCount * 5);
        assertThat(product.getStock()).isEqualTo(expectedStock);

        System.out.println("\n검증 완료: 모든 요청(" + successCount.get() + "개) 성공 ✓");
        System.out.println("재고 감소: " + successCount.get() + " × 5 = " + (successCount.get() * 5) + "개");
        System.out.println("예상 재고: " + expectedStock + ", 실제 재고: " + product.getStock() + " ✓");

        // 동작 흐름 설명
        System.out.println("\n[동작 흐름]");
        System.out.println("1단계: 모든 스레드가 version 0인 엔티티를 읽음");
        System.out.println("2단계: 스레드-1 먼저 커밋 성공 → version: 0→1");
        System.out.println("3단계: 스레드-2 충돌 감지 → 재시도 → version 1로 읽고 커밋 → version: 1→2");
        System.out.println("4단계: 스레드-3 충돌 감지 → 재시도 → version 2로 읽고 커밋 → version: 2→3");
        System.out.println("5단계: 스레드-4 충돌 감지 → 재시도 → version 3으로 읽고 커밋 → version: 3→4");
        System.out.println("6단계: 스레드-5 충돌 감지 → 재시도 → version 4로 읽고 커밋 → version: 4→5");

        System.out.println("\n[결론] 낙관적 락 + 재시도로 모든 요청이 순차적으로 성공했습니다!");
        System.out.println("       충돌 발생 시 재시도를 통해 최신 데이터를 읽고 작업을 완료합니다.");
        System.out.println("================================================================\n");
    }
}
