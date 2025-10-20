package com.example.optimisticlock.service;

import com.example.optimisticlock.dto.ProductResponse;
import com.example.optimisticlock.entity.Product;
import com.example.optimisticlock.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Retryable을 사용한 낙관적 락 재시도 서비스
 *
 * Spring Retry를 활용하여 선언적 방식으로 재시도 로직을 구현합니다.
 * AOP 기반으로 동작하므로 별도 서비스로 분리해야 합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductRetryService {

    private final ProductRepository productRepository;

    /**
     * @Retryable을 사용한 재고 감소
     *
     * @Retryable 어노테이션 파라미터:
     * - retryFor: 재시도할 예외 타입 지정 (낙관적 락 예외)
     * - maxAttempts: 최대 시도 횟수 (3번 시도)
     * - backoff: 재시도 간격 설정
     *   - delay: 기본 대기 시간 (100ms)
     *   - multiplier: 지수 백오프 배수 (2배씩 증가: 100ms -> 200ms -> 400ms)
     *   - maxDelay: 최대 대기 시간 (500ms)
     *
     * 중요: @Transactional과 함께 사용 시, 각 재시도마다 새로운 트랜잭션이 시작됩니다.
     */
    @Retryable(
        retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(
            delay = 100,        // 초기 대기 시간: 100ms
            multiplier = 2,     // 지수 백오프: 2배씩 증가
            maxDelay = 500      // 최대 대기 시간: 500ms
        )
    )
    @Transactional
    public ProductResponse decreaseStockWithRetry(Long productId, int quantity) {
        log.info("[Retryable] 재고 감소 시도 - 상품 ID: {}, 감소량: {}", productId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        log.info("[Retryable] 현재 재고: {}, 버전: {}", product.getStock(), product.getVersion());

        product.decreaseStock(quantity);
        Product savedProduct = productRepository.save(product);

        log.info("[Retryable] 재고 감소 성공 - 남은 재고: {}, 새 버전: {}",
                savedProduct.getStock(), savedProduct.getVersion());

        return ProductResponse.from(savedProduct);
    }

    /**
     * @Recover: 모든 재시도 실패 시 호출되는 복구 메서드
     *
     * - @Retryable 메서드의 모든 시도가 실패하면 자동으로 호출됩니다
     * - 첫 번째 파라미터는 발생한 예외여야 합니다
     * - 나머지 파라미터는 원본 메서드와 동일해야 합니다
     * - 반환 타입도 원본 메서드와 동일해야 합니다
     */
    @Recover
    public ProductResponse recoverDecreaseStock(
            OptimisticLockException e,
            Long productId,
            int quantity) {
        log.error("[Recover] 낙관적 락 재시도 실패 (OptimisticLockException) - 상품 ID: {}, 감소량: {}",
                productId, quantity);
        throw new RuntimeException(
                String.format("재고 감소 실패: 동시성 문제로 인한 최대 재시도 횟수 초과 (상품 ID: %d)", productId), e);
    }

    @Recover
    public ProductResponse recoverDecreaseStock(
            ObjectOptimisticLockingFailureException e,
            Long productId,
            int quantity) {
        log.error("[Recover] 낙관적 락 재시도 실패 (ObjectOptimisticLockingFailureException) - 상품 ID: {}, 감소량: {}",
                productId, quantity);
        throw new RuntimeException(
                String.format("재고 감소 실패: 동시성 문제로 인한 최대 재시도 횟수 초과 (상품 ID: %d)", productId), e);
    }

    /**
     * 고정 간격 재시도 예제
     *
     * @Backoff에 multiplier를 지정하지 않으면 고정된 간격으로 재시도합니다.
     */
    @Retryable(
        retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 200)  // 고정 200ms 간격으로 재시도
    )
    @Transactional
    public ProductResponse increaseStockWithRetry(Long productId, int quantity) {
        log.info("[Retryable-Fixed] 재고 증가 시도 - 상품 ID: {}, 증가량: {}", productId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        log.info("[Retryable-Fixed] 현재 재고: {}, 버전: {}", product.getStock(), product.getVersion());

        product.increaseStock(quantity);
        Product savedProduct = productRepository.save(product);

        log.info("[Retryable-Fixed] 재고 증가 성공 - 새 재고: {}, 새 버전: {}",
                savedProduct.getStock(), savedProduct.getVersion());

        return ProductResponse.from(savedProduct);
    }

    @Recover
    public ProductResponse recoverIncreaseStock(
            Exception e,  // 모든 예외를 처리하는 경우
            Long productId,
            int quantity) {
        log.error("[Recover] 재고 증가 재시도 실패 - 상품 ID: {}, 증가량: {}", productId, quantity);
        throw new RuntimeException(
                String.format("재고 증가 실패: 최대 재시도 횟수 초과 (상품 ID: %d)", productId), e);
    }
}
