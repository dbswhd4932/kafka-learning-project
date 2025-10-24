package com.example.kafka.controller;

import com.example.kafka.domain.Order;
import com.example.kafka.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Order API Controller
 * - 이벤트 기반 주문 처리 API (트랜잭션 분리 패턴)
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성 (트랜잭션 분리 패턴)
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        log.info("Received order creation request: {}", order);
        Order createdOrder = orderService.createOrder(order);
        return ResponseEntity.ok(createdOrder);
    }


    /**
     * 대량 주문 생성 (트랜잭션 분리 패턴)
     * POST /api/orders/bulk?count=10
     */
    @PostMapping("/bulk")
    public ResponseEntity<String> createBulkOrders(@RequestParam(defaultValue = "10") int count) {
        log.info("Creating {} bulk orders", count);

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < count; i++) {
            Order order = Order.builder()
                    .customerId("CUST-" + (1000 + i))
                    .productName("Product " + (i % 5 + 1))
                    .quantity(i % 10 + 1)
                    .price(BigDecimal.valueOf((i % 5 + 1) * 10000))
                    .build();

            try {
                Order result = orderService.createOrder(order);
                if ("SUCCESS".equals(result.getStatus())) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.error("Order creation failed: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(String.format(
                "Total: %d, Success: %d, Failed: %d",
                count, successCount, failCount
        ));
    }

    /**
     * 헬스체크
     * GET /api/orders/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order API is running");
    }
}
