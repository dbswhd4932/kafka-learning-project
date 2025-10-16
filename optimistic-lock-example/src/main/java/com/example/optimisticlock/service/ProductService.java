package com.example.optimisticlock.service;

import com.example.optimisticlock.dto.ProductRequest;
import com.example.optimisticlock.dto.ProductResponse;
import com.example.optimisticlock.entity.Product;
import com.example.optimisticlock.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductStockService productStockService;  // 트랜잭션 단위 작업을 위한 별도 서비스
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 상품 생성
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .stock(request.getStock())
                .price(request.getPrice())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("상품 생성 완료 - ID: {}, 이름: {}, 재고: {}",
                savedProduct.getId(), savedProduct.getName(), savedProduct.getStock());

        return ProductResponse.from(savedProduct);
    }

    /**
     * 상품 조회
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));
        return ProductResponse.from(product);
    }

    /**
     * 전체 상품 조회
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 재고 감소 (낙관적 락 사용)
     * @Version 필드가 있으므로 자동으로 낙관적 락이 적용됩니다.
     * 동시에 같은 상품의 재고를 감소시키려고 할 때,
     * 먼저 완료된 트랜잭션은 성공하고, 나중에 커밋하는 트랜잭션은 OptimisticLockException이 발생합니다.
     */
    @Transactional
    public ProductResponse decreaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        log.info("재고 감소 시작 - 상품 ID: {}, 현재 재고: {}, 감소량: {}, 버전: {}",
                productId, product.getStock(), quantity, product.getVersion());

        product.decreaseStock(quantity);
        Product savedProduct = productRepository.save(product);

        log.info("재고 감소 완료 - 상품 ID: {}, 남은 재고: {}, 새 버전: {}",
                productId, savedProduct.getStock(), savedProduct.getVersion());

        return ProductResponse.from(savedProduct);
    }

    /**
     * 재고 감소 (재시도 로직 포함)
     * OptimisticLockException 발생 시 자동으로 재시도합니다.
     *
     * 중요: 이 메서드는 @Transactional이 없어야 합니다!
     * 각 재시도마다 새로운 트랜잭션을 시작해야 하므로,
     * 실제 작업은 별도의 서비스(ProductStockService)에서 수행합니다.
     */
    public ProductResponse decreaseStockWithRetry(Long productId, int quantity) {
        int retryCount = 0;

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                log.info("[시도: {}] 재고 감소 시작 - 상품 ID: {}", retryCount + 1, productId);

                // 각 시도마다 새로운 트랜잭션에서 실행 (별도 서비스 사용)
                ProductResponse response = productStockService.decreaseStock(productId, quantity, retryCount);

                log.info("[시도: {}] 재고 감소 성공 - 상품 ID: {}, 남은 재고: {}, 버전: {}",
                        retryCount + 1, productId, response.getStock(), response.getVersion());

                return response;

            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                retryCount++;
                log.warn("[시도 실패: {}] 낙관적 락 충돌 발생 - 상품 ID: {}", retryCount, productId);

                if (retryCount >= MAX_RETRY_COUNT) {
                    log.error("최대 재시도 횟수({}) 초과 - 상품 ID: {}", MAX_RETRY_COUNT, productId);
                    throw new RuntimeException("재고 감소 실패: 동시성 문제로 인한 최대 재시도 횟수 초과", e);
                }

                // 재시도 전 대기 (지수 백오프)
                long waitTime = 100L * retryCount;  // 100ms, 200ms, 300ms
                log.info("{}ms 대기 후 {}번째 재시도를 진행합니다...", waitTime, retryCount + 1);

                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                }
            }
        }

        throw new RuntimeException("재고 감소 실패");
    }

    /**
     * 재고 증가
     */
    @Transactional
    public ProductResponse increaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        log.info("재고 증가 시작 - 상품 ID: {}, 현재 재고: {}, 증가량: {}, 버전: {}",
                productId, product.getStock(), quantity, product.getVersion());

        product.increaseStock(quantity);
        Product savedProduct = productRepository.save(product);

        log.info("재고 증가 완료 - 상품 ID: {}, 새 재고: {}, 새 버전: {}",
                productId, savedProduct.getStock(), savedProduct.getVersion());

        return ProductResponse.from(savedProduct);
    }

    /**
     * 가격 변경
     */
    @Transactional
    public ProductResponse updatePrice(Long productId, int newPrice) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        log.info("가격 변경 시작 - 상품 ID: {}, 현재 가격: {}, 새 가격: {}, 버전: {}",
                productId, product.getPrice(), newPrice, product.getVersion());

        product.updatePrice(newPrice);
        Product savedProduct = productRepository.save(product);

        log.info("가격 변경 완료 - 상품 ID: {}, 새 가격: {}, 새 버전: {}",
                productId, savedProduct.getPrice(), savedProduct.getVersion());

        return ProductResponse.from(savedProduct);
    }
}
