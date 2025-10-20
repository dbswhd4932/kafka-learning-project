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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductRetryServiceTest {

    @Autowired
    private ProductRetryService productRetryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("@Retryable을 사용한 재고 감소 - 동시 요청 테스트")
    void decreaseStockWithRetryableTest() throws InterruptedException {
        /**
         * [@Retryable 테스트]
         * - 3개 스레드가 동시에 재고 감소 시도
         * - 낙관적 락 충돌 시 @Retryable이 자동 재시도
         * - 지수 백오프: 100ms -> 200ms -> 400ms
         */

        // given
        ProductRequest request = ProductRequest.builder()
                .name("@Retryable 테스트 상품")
                .stock(100)
                .price(10000)
                .build();
        ProductResponse created = productService.createProduct(request);

        int threadCount = 3;
        int decreaseAmount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 3개 스레드가 동시에 각각 5개씩 재고 감소
        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i + 1;
            executorService.submit(() -> {
                try {
                    productRetryService.decreaseStockWithRetry(created.getId(), decreaseAmount);
                    successCount.incrementAndGet();
                    System.out.println("✓ [스레드-" + threadNumber + "] 성공");
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.println("✗ [스레드-" + threadNumber + "] 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        Product finalProduct = productRepository.findById(created.getId()).orElseThrow();

        System.out.println("\n결과: 성공 " + successCount.get() + "개, 실패 " + failCount.get() + "개");
        System.out.println("최종 재고: " + finalProduct.getStock() + "개, 버전: " + finalProduct.getVersion());

        // 검증
        assertThat(successCount.get()).isEqualTo(threadCount);
        int expectedStock = 100 - (threadCount * decreaseAmount);
        assertThat(finalProduct.getStock()).isEqualTo(expectedStock);
        assertThat(finalProduct.getVersion()).isEqualTo((long) threadCount);
    }
}
