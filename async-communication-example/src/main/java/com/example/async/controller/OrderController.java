package com.example.async.controller;

import com.example.async.domain.Order;
import com.example.async.dto.OrderRequest;
import com.example.async.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성 (비동기 처리)
     * POST /api/orders
     * 주문은 즉시 생성되고, 처리는 백그라운드에서 진행됨
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("Request received: Create order for {}", request.getCustomerEmail());
        long startTime = System.currentTimeMillis();

        Order order = orderService.createOrder(request);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Response returned in {}ms (order processing in background)", duration);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * 여러 주문을 병렬로 처리
     * POST /api/orders/batch
     */
    @PostMapping("/batch")
    public CompletableFuture<ResponseEntity<List<Order>>> createOrdersBatch(
            @Valid @RequestBody List<OrderRequest> requests) {
        log.info("Request received: Create {} orders in batch", requests.size());

        // 모든 주문을 먼저 생성
        List<Order> createdOrders = requests.stream()
                .map(orderService::createOrder)
                .collect(Collectors.toList());

        // 모든 주문을 병렬로 처리하고 결과를 기다림
        List<CompletableFuture<Order>> futures = createdOrders.stream()
                .map(order -> orderService.processOrderWithResult(order.getId()))
                .collect(Collectors.toList());

        // 모든 CompletableFuture가 완료될 때까지 대기
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        // 모든 처리가 완료되면 결과 반환
        return allFutures.thenApply(v ->
                ResponseEntity.ok(
                        futures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList())
                )
        );
    }

    /**
     * 주문 조회
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        log.info("Request received: Get order - {}", id);
        Order order = orderService.findOrderById(id);
        if (order != null) {
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 모든 주문 조회
     * GET /api/orders
     */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        log.info("Request received: Get all orders");
        List<Order> orders = orderService.findAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * 상태별 주문 조회
     * GET /api/orders/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable Order.OrderStatus status) {
        log.info("Request received: Get orders by status - {}", status);
        List<Order> orders = orderService.findOrdersByStatus(status);
        return ResponseEntity.ok(orders);
    }
}
