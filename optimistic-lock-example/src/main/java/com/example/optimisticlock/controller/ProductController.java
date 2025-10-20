package com.example.optimisticlock.controller;

import com.example.optimisticlock.dto.ProductRequest;
import com.example.optimisticlock.dto.ProductResponse;
import com.example.optimisticlock.dto.StockUpdateRequest;
import com.example.optimisticlock.service.ProductRetryService;
import com.example.optimisticlock.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final ProductRetryService productRetryService;

    /**
     * 상품 생성
     * POST /api/products
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductRequest request) {
        log.info("상품 생성 요청 - 이름: {}, 재고: {}, 가격: {}",
                request.getName(), request.getStock(), request.getPrice());
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 상품 조회
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        log.info("상품 조회 요청 - ID: {}", id);
        ProductResponse response = productService.getProduct(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 상품 조회
     * GET /api/products
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.info("전체 상품 조회 요청");
        List<ProductResponse> responses = productService.getAllProducts();
        return ResponseEntity.ok(responses);
    }

    /**
     * 재고 감소 (낙관적 락)
     * POST /api/products/{id}/decrease-stock
     */
    @PostMapping("/{id}/decrease-stock")
    public ResponseEntity<ProductResponse> decreaseStock(
            @PathVariable Long id,
            @RequestBody StockUpdateRequest request) {
        log.info("재고 감소 요청 - 상품 ID: {}, 수량: {}", id, request.getQuantity());
        try {
            ProductResponse response = productService.decreaseStock(id, request.getQuantity());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("재고 감소 실패 - 상품 ID: {}, 에러: {}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 재고 감소 (재시도 로직 포함)
     * POST /api/products/{id}/decrease-stock-retry
     */
    @PostMapping("/{id}/decrease-stock-retry")
    public ResponseEntity<ProductResponse> decreaseStockWithRetry(
            @PathVariable Long id,
            @RequestBody StockUpdateRequest request) {
        log.info("재고 감소 요청 (재시도) - 상품 ID: {}, 수량: {}", id, request.getQuantity());
        try {
            ProductResponse response = productService.decreaseStockWithRetry(id, request.getQuantity());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("재고 감소 실패 (재시도) - 상품 ID: {}, 에러: {}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 재고 증가
     * POST /api/products/{id}/increase-stock
     */
    @PostMapping("/{id}/increase-stock")
    public ResponseEntity<ProductResponse> increaseStock(
            @PathVariable Long id,
            @RequestBody StockUpdateRequest request) {
        log.info("재고 증가 요청 - 상품 ID: {}, 수량: {}", id, request.getQuantity());
        ProductResponse response = productService.increaseStock(id, request.getQuantity());
        return ResponseEntity.ok(response);
    }

    /**
     * 재고 감소 (@Retryable 사용)
     * POST /api/products/{id}/decrease-stock-retryable
     *
     * @Retryable 어노테이션을 사용하여 선언적으로 재시도를 처리합니다.
     * - 낙관적 락 충돌 시 자동으로 재시도 (최대 3회)
     * - 지수 백오프 적용 (100ms -> 200ms -> 400ms)
     */
    @PostMapping("/{id}/decrease-stock-retryable")
    public ResponseEntity<ProductResponse> decreaseStockWithRetryable(
            @PathVariable Long id,
            @RequestBody StockUpdateRequest request) {
        log.info("재고 감소 요청 (@Retryable) - 상품 ID: {}, 수량: {}", id, request.getQuantity());
        try {
            ProductResponse response = productRetryService.decreaseStockWithRetry(id, request.getQuantity());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("재고 감소 실패 (@Retryable) - 상품 ID: {}, 에러: {}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 재고 증가 (@Retryable 사용, 고정 간격)
     * POST /api/products/{id}/increase-stock-retryable
     *
     * @Retryable 어노테이션을 사용하여 고정 간격으로 재시도합니다.
     * - 낙관적 락 충돌 시 자동으로 재시도 (최대 5회)
     * - 고정 200ms 간격으로 재시도
     */
    @PostMapping("/{id}/increase-stock-retryable")
    public ResponseEntity<ProductResponse> increaseStockWithRetryable(
            @PathVariable Long id,
            @RequestBody StockUpdateRequest request) {
        log.info("재고 증가 요청 (@Retryable) - 상품 ID: {}, 수량: {}", id, request.getQuantity());
        try {
            ProductResponse response = productRetryService.increaseStockWithRetry(id, request.getQuantity());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("재고 증가 실패 (@Retryable) - 상품 ID: {}, 에러: {}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 가격 변경
     * PATCH /api/products/{id}/price
     */
    @PatchMapping("/{id}/price")
    public ResponseEntity<ProductResponse> updatePrice(
            @PathVariable Long id,
            @RequestParam int price) {
        log.info("가격 변경 요청 - 상품 ID: {}, 새 가격: {}", id, price);
        ProductResponse response = productService.updatePrice(id, price);
        return ResponseEntity.ok(response);
    }

    /**
     * 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("잘못된 요청: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("서버 에러 발생: {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse("서버 에러가 발생했습니다: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 에러 응답 DTO
     */
    public record ErrorResponse(String message) {}
}
