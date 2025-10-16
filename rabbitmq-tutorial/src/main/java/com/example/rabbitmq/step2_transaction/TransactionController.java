package com.example.rabbitmq.step2_transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Step 2: Transaction 처리 컨트롤러
 *
 * DB와 메시지 큐를 함께 사용하는 트랜잭션 처리 API를 제공합니다.
 *
 * 사용 예시:
 * POST http://localhost:8080/api/v1/transaction/orders
 * Content-Type: application/json
 * {
 *   "orderNumber": "ORD-001",
 *   "customerId": "CUST-001",
 *   "productName": "Laptop",
 *   "quantity": 1,
 *   "price": 1500000
 * }
 */
@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final OrderService orderService;

    /**
     * 주문 생성 API
     *
     * DB에 주문을 저장하고 RabbitMQ에 메시지를 전송합니다.
     *
     * @param orderMessage 주문 정보
     * @return 생성된 주문 정보
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody OrderMessage orderMessage) {
        Order order = orderService.createOrderWithMessage(orderMessage);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "주문이 성공적으로 생성되었습니다.");
        response.put("orderId", order.getId());
        response.put("orderNumber", order.getOrderNumber());
        response.put("status", order.getStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * 주문 조회 API
     *
     * @param orderNumber 주문 번호
     * @return 주문 정보
     */
    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderNumber) {
        Order order = orderService.getOrder(orderNumber);
        return ResponseEntity.ok(order);
    }

    /**
     * 주문 상태 업데이트 API
     *
     * @param orderNumber 주문 번호
     * @param status 변경할 상태
     * @return 업데이트된 주문 정보
     */
    @PutMapping("/orders/{orderNumber}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable String orderNumber,
            @RequestParam Order.OrderStatus status) {
        Order order = orderService.updateOrderStatus(orderNumber, status);
        return ResponseEntity.ok(order);
    }

    /**
     * 헬스 체크 API
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Transaction Service is running!");
    }
}
