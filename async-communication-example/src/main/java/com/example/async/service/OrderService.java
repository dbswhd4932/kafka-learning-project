package com.example.async.service;

import com.example.async.domain.Order;
import com.example.async.dto.OrderRequest;
import com.example.async.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final EmailService emailService;

    /**
     * 주문 생성
     */
    @Transactional
    public Order createOrder(OrderRequest request) {
        log.info("[{}] Creating order for: {}",
                Thread.currentThread().getName(), request.getCustomerEmail());

        Order order = Order.builder()
                .productName(request.getProductName())
                .amount(request.getAmount())
                .customerEmail(request.getCustomerEmail())
                .build();

        Order savedOrder = orderRepository.save(order);

        // 비동기로 주문 처리 시작
        processOrderAsync(savedOrder.getId());

        log.info("[{}] Order created: {}", Thread.currentThread().getName(), savedOrder.getId());
        return savedOrder;
    }

    /**
     * 비동기로 주문 처리
     */
    @Async("taskExecutor")
    public void processOrderAsync(Long orderId) {
        log.info("[{}] Processing order asynchronously: {}",
                Thread.currentThread().getName(), orderId);

        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // 주문 상태를 PROCESSING으로 변경
            order.setStatus(Order.OrderStatus.PROCESSING);
            orderRepository.save(order);

            // 주문 처리 시뮬레이션 (5초 소요)
            Thread.sleep(5000);

            // 주문 완료
            order.setStatus(Order.OrderStatus.COMPLETED);
            order.setProcessedAt(LocalDateTime.now());
            orderRepository.save(order);

            log.info("[{}] Order processed successfully: {}",
                    Thread.currentThread().getName(), orderId);

            // 완료 이메일 전송 (비동기)
            emailService.sendEmail(
                    order.getCustomerEmail(),
                    "Order Completed",
                    "Your order #" + order.getId() + " has been completed!"
            );

        } catch (InterruptedException e) {
            log.error("Order processing interrupted", e);
            Thread.currentThread().interrupt();
            updateOrderStatus(orderId, Order.OrderStatus.FAILED);
        } catch (Exception e) {
            log.error("Order processing failed", e);
            updateOrderStatus(orderId, Order.OrderStatus.FAILED);
        }
    }

    /**
     * 여러 주문을 병렬로 처리
     */
    @Async("taskExecutor")
    public CompletableFuture<Order> processOrderWithResult(Long orderId) {
        log.info("[{}] Processing order with result: {}",
                Thread.currentThread().getName(), orderId);

        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            order.setStatus(Order.OrderStatus.PROCESSING);
            orderRepository.save(order);

            // 처리 시뮬레이션
            Thread.sleep(3000);

            order.setStatus(Order.OrderStatus.COMPLETED);
            order.setProcessedAt(LocalDateTime.now());
            Order processedOrder = orderRepository.save(order);

            log.info("[{}] Order processing completed: {}",
                    Thread.currentThread().getName(), orderId);

            return CompletableFuture.completedFuture(processedOrder);

        } catch (InterruptedException e) {
            log.error("Order processing interrupted", e);
            Thread.currentThread().interrupt();
            updateOrderStatus(orderId, Order.OrderStatus.FAILED);
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            log.error("Order processing failed", e);
            updateOrderStatus(orderId, Order.OrderStatus.FAILED);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 주문 상태 업데이트
     */
    @Transactional
    public void updateOrderStatus(Long orderId, Order.OrderStatus status) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(status);
            if (status == Order.OrderStatus.COMPLETED) {
                order.setProcessedAt(LocalDateTime.now());
            }
            orderRepository.save(order);
        });
    }

    /**
     * 모든 주문 조회
     */
    @Transactional(readOnly = true)
    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * 주문 ID로 조회
     */
    @Transactional(readOnly = true)
    public Order findOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    /**
     * 상태별 주문 조회
     */
    @Transactional(readOnly = true)
    public List<Order> findOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
}
