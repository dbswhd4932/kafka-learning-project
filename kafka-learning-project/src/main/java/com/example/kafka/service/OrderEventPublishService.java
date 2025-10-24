package com.example.kafka.service;

import com.example.kafka.domain.Order;
import com.example.kafka.entity.OrderEntity;
import com.example.kafka.event.SalesOrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 이벤트 발행 서비스
 * - 트랜잭션 내에서 이벤트를 발행
 * - @TransactionalEventListener가 트랜잭션 커밋 후 실행되도록 보장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventPublishService {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 성공 이벤트 발행 (트랜잭션 3)
     * - @Transactional: 이벤트 발행을 위한 트랜잭션 컨텍스트 제공
     * - 이 트랜잭션이 커밋되면 @TransactionalEventListener가 실행됨
     */
    @Transactional
    public void publishSuccessEvent(OrderEntity orderEntity) {
        log.info("💾 [TX-3 START] 이벤트 발행 트랜잭션 시작: {}", orderEntity.getOrderId());

        Order order = convertToOrder(orderEntity);
        SalesOrderEvent event = SalesOrderEvent.of(this, order);

        eventPublisher.publishEvent(event);

        log.info("📤 판매 주문 이벤트 발행: {}", orderEntity.getOrderId());
        log.info("💾 [TX-3 COMMIT] 이벤트 발행 트랜잭션 커밋 (리스너는 커밋 후 실행됨)");
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
