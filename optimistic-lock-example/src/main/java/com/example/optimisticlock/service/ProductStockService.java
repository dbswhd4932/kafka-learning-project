package com.example.optimisticlock.service;

import com.example.optimisticlock.dto.ProductResponse;
import com.example.optimisticlock.entity.Product;
import com.example.optimisticlock.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 재고 관리 내부 서비스
 * ProductService의 재시도 로직에서 사용하는 트랜잭션 단위 작업을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductStockService {

    private final ProductRepository productRepository;

    /**
     * 재고 감소 (트랜잭션 단위)
     * 각 호출마다 새로운 트랜잭션을 시작합니다.
     */
    @Transactional
    public ProductResponse decreaseStock(Long productId, int quantity, int attemptNumber) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        log.debug("[시도: {}] 엔티티 조회 완료 - 상품 ID: {}, 현재 재고: {}, 버전: {}",
                attemptNumber + 1, productId, product.getStock(), product.getVersion());

        product.decreaseStock(quantity);
        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }
}
