package com.example.redis.service;

import com.example.redis.domain.Product;
import com.example.redis.dto.ProductResponse;
import com.example.redis.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 서비스
 * - Redis 캐싱을 적용한 버전
 * - @Cacheable: 조회 결과를 캐시에 저장
 * - @CacheEvict: 캐시 삭제
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 모든 상품 조회 (Redis 캐싱 적용)
     * - 첫 조회 시 DB에서 조회하고 결과를 Redis에 저장
     * - 이후 조회 시 Redis에서 조회
     */
    // products 라는 캐시영역에서 all 이라는 키를 가진 데이터가 있는지 확인 (value :: key)
    @Cacheable(value = "products", key = "'all'")
    public List<ProductResponse> getAllProducts() {
        log.info("DB에서 모든 상품 조회 (캐시 미스)");
        // 실제 DB 조회 시뮬레이션을 위한 지연
        simulateSlowQuery();

        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * ID로 상품 조회 (Redis 캐싱 적용)
     */
    @Cacheable(value = "product", key = "#id")
    public ProductResponse getProductById(Long id) {
        log.info("DB에서 상품 조회 - ID: {} (캐시 미스)", id);
        simulateSlowQuery();

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다. ID: " + id));
        return ProductResponse.from(product);
    }

    /**
     * 카테고리로 상품 조회 (Redis 캐싱 적용)
     */
    @Cacheable(value = "products", key = "'category:' + #category")
    public List<ProductResponse> getProductsByCategory(String category) {
        log.info("DB에서 카테고리별 상품 조회 - Category: {} (캐시 미스)", category);
        simulateSlowQuery();

        List<Product> products = productRepository.findByCategory(category);
        return products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 상품 검색 (Redis 캐싱 적용)
     */
    @Cacheable(value = "products", key = "'search:' + #keyword")
    public List<ProductResponse> searchProducts(String keyword) {
        log.info("DB에서 상품 검색 - Keyword: {} (캐시 미스)", keyword);
        simulateSlowQuery();

        List<Product> products = productRepository.searchByName(keyword);
        return products.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 캐시 삭제 (상품 정보가 변경되었을 때)
     */
    @CacheEvict(value = {"product", "products"}, allEntries = true)
    public void clearCache() {
        log.info("상품 캐시 전체 삭제");
    }

    /**
     * 느린 쿼리 시뮬레이션
     * - 실제 DB 조회가 느린 상황을 재현하기 위한 지연
     */
    private void simulateSlowQuery() {
        try {
            Thread.sleep(1000);  // 1초 지연
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
