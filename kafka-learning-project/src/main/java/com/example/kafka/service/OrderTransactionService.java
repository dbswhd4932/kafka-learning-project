package com.example.kafka.service;

import com.example.kafka.domain.Order;
import com.example.kafka.entity.OrderEntity;
import com.example.kafka.enums.OrderStatus;
import com.example.kafka.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 트랜잭션 서비스
 * - 각 트랜잭션을 독립적으로 관리
 * - REQUIRES_NEW를 통해 별도의 트랜잭션으로 실행
 * - OrderService에서 외부 호출하여 프록시를 통과하도록 설계
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTransactionService {

    private final OrderRepository orderRepository;

    /**
     * [트랜잭션 1] PENDING 상태로 주문 생성
     * - 독립적인 트랜잭션
     * - 결제 성공/실패와 무관하게 주문 이력은 남김
     */
    @Transactional
    public OrderEntity createPendingOrder(Order order) {
        log.info("💾 [TX-1 START] 주문 생성 트랜잭션 시작");

        OrderEntity entity = OrderEntity.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .productId(order.getProductId())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .totalAmount(order.getTotalAmount())
                .orderStatus(OrderStatus.PENDING)
                .orderSuccessYn(Boolean.FALSE)
                .orderDatetime(order.getOrderDateTime())
                .build();

        OrderEntity savedEntity = orderRepository.save(entity);

        log.info("💾 [TX-1 COMMIT] 주문 생성 완료 - ID: {}, 상태: PENDING", savedEntity.getOrderId());
        return savedEntity;
    }

    /**
     * [트랜잭션 2] 주문을 성공 상태로 변경
     * - 독립적인 트랜잭션
     * - 결제 성공 시 호출
     */
    @Transactional
    public OrderEntity markOrderAsSuccess(String orderId) {
        log.info("💾 [TX-2 START] 주문 성공 처리 트랜잭션 시작: {}", orderId);

        OrderEntity entity = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        entity.markAsSuccess();
        OrderEntity updatedEntity = orderRepository.save(entity);

        log.info("💾 [TX-2 COMMIT] 주문 성공 처리 완료 - ID: {}, 상태: SUCCESS, 성공여부: Y",
                updatedEntity.getOrderId());
        return updatedEntity;
    }

    /**
     * [트랜잭션 3] 주문을 실패 상태로 변경
     * - 독립적인 트랜잭션
     * - 결제 실패 시 호출
     */
    @Transactional
    public OrderEntity markOrderAsFailed(String orderId, String reason) {
        log.info("💾 [TX-3 START] 주문 실패 처리 트랜잭션 시작: {}", orderId);

        OrderEntity entity = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

        entity.markAsFailed(reason);
        OrderEntity updatedEntity = orderRepository.save(entity);

        log.info("💾 [TX-3 COMMIT] 주문 실패 처리 완료 - ID: {}, 상태: FAILED, 성공여부: N, 사유: {}",
                updatedEntity.getOrderId(), reason);
        return updatedEntity;
    }
}
