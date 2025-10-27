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
            int productIndex = i % 5 + 1;
            Order order = Order.builder()
                    .customerId("CUST-" + (1000 + i))
                    .productId("PROD-00" + productIndex)
                    .productName("Product " + productIndex)
                    .quantity(i % 10 + 1)
                    .price(BigDecimal.valueOf(productIndex * 10000))
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
     * 같은 고객의 여러 주문 생성 (Key 파티셔닝 테스트)
     * POST /api/orders/same-customer?customerId=CUST-TEST&count=5
     */
    @PostMapping("/same-customer")
    public ResponseEntity<String> createSameCustomerOrders(
            @RequestParam(defaultValue = "CUST-TEST") String customerId,
            @RequestParam(defaultValue = "5") int count) {
        log.info("Creating {} orders for same customer: {}", count, customerId);

        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < count; i++) {
            Order order = Order.builder()
                    .customerId(customerId)  // 같은 고객 ID
                    .productId("PROD-00" + (i % 5 + 1))
                    .productName("Product " + (i % 5 + 1))
                    .quantity(i + 1)
                    .price(BigDecimal.valueOf(10000 * (i + 1)))
                    .build();

            try {
                Order result = orderService.createOrder(order);
                if ("SUCCESS".equals(result.getStatus())) {
                    successCount++;
                    log.info("Order {} created: {}", i + 1, result.getOrderId());
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.error("Order creation failed: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(String.format(
                "Customer: %s, Total: %d, Success: %d, Failed: %d",
                customerId, count, successCount, failCount
        ));
    }

    /**
     * Partition 분산 테스트 (Key별 파티션 확인)
     * GET /api/orders/test-partition
     */
    @GetMapping("/test-partition")
    public ResponseEntity<String> testPartition() {
        StringBuilder result = new StringBuilder();
        result.append("=== Partition Distribution Test ===\n\n");

        // 같은 Key로 3번 전송 (같은 파티션으로 가야 함)
        String testKey = "TEST-KEY-123";

        for (int i = 0; i < 3; i++) {
            Order order = Order.builder()
                    .orderId(testKey)  // 같은 Key 사용!
                    .customerId("CUST-PARTITION-TEST")
                    .productId("PROD-TEST")
                    .productName("Partition Test Product")
                    .quantity(1)
                    .price(BigDecimal.valueOf(10000))
                    .build();

            try {
                orderService.createOrder(order);
                result.append(String.format("Message %d sent with key: %s\n", i + 1, testKey));
            } catch (Exception e) {
                result.append(String.format("Message %d failed: %s\n", i + 1, e.getMessage()));
            }
        }

        result.append("\n✅ Check logs to verify all messages went to the SAME partition!\n");
        result.append("Look for: 'Successfully sent message with key - topic: sales-orders, key: TEST-KEY-123'\n");

        return ResponseEntity.ok(result.toString());
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
