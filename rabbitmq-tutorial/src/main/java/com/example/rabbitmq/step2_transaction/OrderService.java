package com.example.rabbitmq.step2_transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Step 2: 주문 서비스 - Transaction 처리
 *
 * DB와 메시지 큐를 함께 사용하는 트랜잭션 처리를 보여줍니다.
 *
 * 중요 포인트:
 *
 * 1. @Transactional 어노테이션
 *    - 메서드 실행 중 예외가 발생하면 DB 작업이 롤백됩니다
 *    - 하지만 RabbitMQ 메시지 전송은 DB 트랜잭션과 별개입니다!
 *
 * 2. 메시지 전송 시점
 *    - DB 저장 후 메시지를 전송하는 것이 일반적
 *    - DB 저장이 성공한 후에만 메시지를 전송해야 데이터 일관성 유지
 *
 * 3. 분산 트랜잭션 문제
 *    - DB 저장은 성공했지만 메시지 전송이 실패할 수 있음
 *    - 메시지 전송은 성공했지만 DB 커밋이 실패할 수 있음
 *    - 이를 해결하기 위한 패턴:
 *      a. Outbox Pattern (가장 권장)
 *      b. 2-Phase Commit (복잡하고 성능 이슈)
 *      c. Saga Pattern (MSA 환경)
 *
 * 4. 이 예제의 제한사항
 *    - 간단한 예제를 위해 기본적인 트랜잭션 처리만 구현
 *    - 실제 프로덕션에서는 Outbox Pattern 등을 사용 권장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.transaction.exchange-name}")
    private String exchangeName;

    @Value("${app.rabbitmq.transaction.routing-key}")
    private String routingKey;

    /**
     * 주문 생성 및 메시지 전송
     *
     * 트랜잭션 내에서 DB 저장과 메시지 전송을 수행합니다.
     *
     * 동작 순서:
     * 1. 주문 엔티티 생성 및 DB 저장
     * 2. DB 저장이 성공하면 메시지 생성
     * 3. RabbitMQ에 메시지 전송
     *
     * 주의사항:
     * - DB 커밋 전에 메시지가 전송될 수 있음
     * - 메시지 전송 실패 시 DB 롤백됨
     *
     * @param orderMessage 주문 메시지
     * @return 저장된 주문
     */
    @Transactional
    public Order createOrderWithMessage(OrderMessage orderMessage) {
        log.info("=== 주문 생성 및 메시지 전송 시작 ===");
        log.info("주문 번호: {}", orderMessage.getOrderNumber());

        try {
            // 1. 주문 엔티티 생성 및 저장
            Order order = orderMessage.toEntity();
            order.setStatus(Order.OrderStatus.PENDING);
            Order savedOrder = orderRepository.save(order);
            log.info("DB 저장 완료 - Order ID: {}", savedOrder.getId());

            // 2. 메시지 전송
            // DB 커밋 전에 메시지가 전송될 수 있음을 유의!
            OrderMessage message = OrderMessage.from(savedOrder);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
            log.info("메시지 전송 완료");

            log.info("=== 주문 생성 및 메시지 전송 완료 ===");
            return savedOrder;

        } catch (Exception e) {
            log.error("주문 생성 또는 메시지 전송 실패", e);
            // 예외 발생 시 트랜잭션 롤백
            throw new RuntimeException("주문 처리 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 주문 상태 업데이트
     *
     * @param orderNumber 주문 번호
     * @param status 변경할 상태
     * @return 업데이트된 주문
     */
    @Transactional
    public Order updateOrderStatus(String orderNumber, Order.OrderStatus status) {
        log.info("주문 상태 업데이트 - 주문번호: {}, 상태: {}", orderNumber, status);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderNumber));

        order.setStatus(status);
        return orderRepository.save(order);
    }

    /**
     * 주문 조회
     *
     * @param orderNumber 주문 번호
     * @return 주문
     */
    @Transactional(readOnly = true)
    public Order getOrder(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderNumber));
    }
}
