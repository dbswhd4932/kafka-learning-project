package com.example.redis.service;

import com.example.redis.domain.Product;
import com.example.redis.dto.ProductResponse;
import com.example.redis.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 서비스 (Redis 캐싱 미적용)
 * - 성능 비교를 위한 캐싱 미적용 버전
 * - 매번 DB에서 직접 조회
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceWithoutCache {

    private final ProductRepository productRepository;

    /**
     * 모든 상품 조회 (캐싱 없음)
     * - 매번 DB에서 조회
     */
    public List<ProductResponse> getAllProducts() {
        log.info("DB에서 모든 상품 조회 (캐싱 미적용)");
        simulateSlowQuery();

        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * ID로 상품 조회 (캐싱 없음)
     */
    public ProductResponse getProductById(Long id) {
        log.info("DB에서 상품 조회 - ID: {} (캐싱 미적용)", id);
        simulateSlowQuery();

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다. ID: " + id));
        return ProductResponse.from(product);
    }

    /**
     * 카테고리로 상품 조회 (캐싱 없음)
     */
    public List<ProductResponse> getProductsByCategory(String category) {
        log.info("DB에서 카테고리별 상품 조회 - Category: {} (캐싱 미적용)", category);
        simulateSlowQuery();

        List<Product> products = productRepository.findByCategory(category);
        return products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 상품 검색 (캐싱 없음)
     */
    public List<ProductResponse> searchProducts(String keyword) {
        log.info("DB에서 상품 검색 - Keyword: {} (캐싱 미적용)", keyword);
        simulateSlowQuery();

        List<Product> products = productRepository.searchByName(keyword);
        return products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 느린 쿼리 시뮬레이션
     */
    private void simulateSlowQuery() {
        try {
            Thread.sleep(1000);  // 1초 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
