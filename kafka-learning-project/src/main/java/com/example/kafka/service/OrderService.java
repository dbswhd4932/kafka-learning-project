package com.example.kafka.service;

import com.example.kafka.domain.Order;
import com.example.kafka.entity.OrderEntity;
import com.example.kafka.enums.OrderStatus;
import com.example.kafka.event.SalesOrderEvent;
import com.example.kafka.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 서비스
 * - 트랜잭션 분리 패턴 적용
 * - 각 비즈니스 단위별로 트랜잭션 분리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderTransactionService transactionService;
    private final OrderEventPublishService eventPublishService;
    private final OrderRepository orderRepository;

    /**
     * 주문 생성 (메인 흐름)
     * - 트랜잭션 없음: 각 단계별로 독립적인 트랜잭션 실행
     *
     * 흐름:
     * 1. 주문 정보 검증 및 초기화
     * 2. PENDING 상태로 주문 생성 (트랜잭션 1)
     * 3. 결제 처리 시뮬레이션
     * 4-1. 성공 시: 주문 성공 처리 (트랜잭션 2) + 이벤트 발행
     * 4-2. 실패 시: 주문 실패 처리 (트랜잭션 3)
     */
    public Order createOrder(Order order) {
        log.info("========================================");
        log.info("📦 주문 생성 시작: {}", order.getProductName());
        log.info("========================================");

        // 1. 주문 정보 초기화
        initializeOrderInfo(order);

        // 2. PENDING 상태로 주문 저장 (트랜잭션 1)
        OrderEntity orderEntity = transactionService.createPendingOrder(order);
        log.info("✅ [트랜잭션 1] PENDING 상태로 주문 저장 완료: {}", orderEntity.getOrderId());

        // 3. 결제 처리 시뮬레이션
        boolean paymentSuccess = simulatePayment(orderEntity);

        if (paymentSuccess) {
            // 4-1. 결제 성공: 주문 성공 처리 (트랜잭션 2)
            orderEntity = transactionService.markOrderAsSuccess(orderEntity.getOrderId());
            log.info("✅ [트랜잭션 2] 주문 성공 처리 완료: {}", orderEntity.getOrderId());

            // 5. 성공한 주문만 Kafka 이벤트 발행 (트랜잭션 3)
            eventPublishService.publishSuccessEvent(orderEntity);
            log.info("📤 Kafka 이벤트 발행 완료: {}", orderEntity.getOrderId());

        } else {
            // 4-2. 결제 실패: 주문 실패 처리 (트랜잭션 3)
            orderEntity = transactionService.markOrderAsFailed(orderEntity.getOrderId(), "결제 승인 실패");
            log.error("❌ [트랜잭션 3] 주문 실패 처리 완료: {}", orderEntity.getOrderId());
        }

        log.info("========================================");
        log.info("📦 주문 생성 종료: {} (상태: {})", orderEntity.getOrderId(), orderEntity.getOrderStatus());
        log.info("========================================");

        return convertToOrder(orderEntity);
    }

    /**
     * 결제 처리 시뮬레이션
     * - 실제로는 PG사 API 호출
     * - 학습용: 30% 확률로 실패
     */
    private boolean simulatePayment(OrderEntity order) {
        log.info("💳 결제 처리 중... (주문 ID: {}, 금액: {}원)",
                order.getOrderId(), order.getTotalAmount());

        try {
            // 결제 API 호출 시뮬레이션 (지연)
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 30% 확률로 실패
        boolean success = Math.random() > 0.3;

        if (success) {
            log.info("💳 ✅ 결제 성공: {} (승인번호: {})",
                    order.getOrderId(),
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        } else {
            log.error("💳 ❌ 결제 실패: {} (사유: 카드 승인 거부)", order.getOrderId());
        }

        return success;
    }

    /**
     * 주문 정보 초기화
     */
    private void initializeOrderInfo(Order order) {
        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            order.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        order.setOrderDateTime(LocalDateTime.now());

        if (order.getTotalAmount() == null && order.getPrice() != null && order.getQuantity() != null) {
            order.setTotalAmount(order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity())));
        }
    }

    /**
     * OrderEntity → Order 변환
     */
    private Order convertToOrder(OrderEntity entity) {
        return Order.builder()
                .orderId(entity.getOrderId())
                .customerId(entity.getCustomerId())
                .productName(entity.getProductName())
                .quantity(entity.getQuantity())
                .price(entity.getPrice())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getOrderStatus().name())
                .orderDateTime(entity.getOrderDatetime())
                .build();
    }
}
