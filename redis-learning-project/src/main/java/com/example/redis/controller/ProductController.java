package com.example.redis.controller;

import com.example.redis.dto.ProductResponse;
import com.example.redis.service.ProductService;
import com.example.redis.service.ProductServiceWithoutCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 상품 컨트롤러
 * - /api/products/with-cache: Redis 캐싱 적용
 * - /api/products/without-cache: 캐싱 미적용 (성능 비교용)
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final ProductServiceWithoutCache productServiceWithoutCache;

    /**
     * 모든 상품 조회 (Redis 캐싱 적용)
     * GET /api/products/with-cache
     */
    @GetMapping("/with-cache")
    public ResponseEntity<Map<String, Object>> getAllProductsWithCache() {
        long startTime = System.currentTimeMillis();

        List<ProductResponse> products = productService.getAllProducts();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("data", products);
        response.put("count", products.size());
        response.put("duration_ms", duration);
        response.put("cached", true);

        log.info("상품 조회 완료 (캐싱 적용) - 소요시간: {}ms", duration);

        return ResponseEntity.ok(response);
    }

    /**
     * 모든 상품 조회 (캐싱 미적용)
     * GET /api/products/without-cache
     */
    @GetMapping("/without-cache")
    public ResponseEntity<Map<String, Object>> getAllProductsWithoutCache() {
        long startTime = System.currentTimeMillis();

        List<ProductResponse> products = productServiceWithoutCache.getAllProducts();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("data", products);
        response.put("count", products.size());
        response.put("duration_ms", duration);
        response.put("cached", false);

        log.info("상품 조회 완료 (캐싱 미적용) - 소요시간: {}ms", duration);

        return ResponseEntity.ok(response);
    }

    /**
     * ID로 상품 조회 (Redis 캐싱 적용)
     * GET /api/products/with-cache/{id}
     */
    @GetMapping("/with-cache/{id}")
    public ResponseEntity<Map<String, Object>> getProductByIdWithCache(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();

        ProductResponse product = productService.getProductById(id);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("data", product);
        response.put("duration_ms", duration);
        response.put("cached", true);

        return ResponseEntity.ok(response);
    }

    /**
     * ID로 상품 조회 (캐싱 미적용)
     * GET /api/products/without-cache/{id}
     */
    @GetMapping("/without-cache/{id}")
    public ResponseEntity<Map<String, Object>> getProductByIdWithoutCache(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();

        ProductResponse product = productServiceWithoutCache.getProductById(id);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("data", product);
        response.put("duration_ms", duration);
        response.put("cached", false);

        return ResponseEntity.ok(response);
    }

    /**
     * 카테고리별 상품 조회 (Redis 캐싱 적용)
     * GET /api/products/with-cache/category/{category}
     */
    @GetMapping("/with-cache/category/{category}")
    public ResponseEntity<Map<String, Object>> getProductsByCategoryWithCache(@PathVariable String category) {
        long startTime = System.currentTimeMillis();

        List<ProductResponse> products = productService.getProductsByCategory(category);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("data", products);
        response.put("count", products.size());
        response.put("duration_ms", duration);
        response.put("cached", true);

        return ResponseEntity.ok(response);
    }

    /**
     * 카테고리별 상품 조회 (캐싱 미적용)
     * GET /api/products/without-cache/category/{category}
     */
    @GetMapping("/without-cache/category/{category}")
    public ResponseEntity<Map<String, Object>> getProductsByCategoryWithoutCache(@PathVariable String category) {
        long startTime = System.currentTimeMillis();

        List<ProductResponse> products = productServiceWithoutCache.getProductsByCategory(category);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("data", products);
        response.put("count", products.size());
        response.put("duration_ms", duration);
        response.put("cached", false);

        return ResponseEntity.ok(response);
    }

    /**
     * 캐시 삭제
     * DELETE /api/products/cache
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, String>> clearCache() {
        productService.clearCache();

        Map<String, String> response = new HashMap<>();
        response.put("message", "캐시가 성공적으로 삭제되었습니다.");

        return ResponseEntity.ok(response);
    }

}
