package com.example.rabbitmq.step2_transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Step 2: 주문 메시지 Consumer - Transaction 처리
 *
 * 메시지를 수신하여 주문 상태를 업데이트하는 Consumer입니다.
 *
 * 트랜잭션 처리:
 * 1. 메시지 수신
 * 2. DB에서 주문 조회
 * 3. 주문 상태 업데이트
 * 4. 처리 성공 시 ACK, 실패 시 NACK (재시도 또는 DLQ)
 *
 * @Transactional 사용:
 * - Consumer 메서드에도 @Transactional을 사용할 수 있음
 * - DB 작업이 실패하면 예외가 발생하고 메시지는 Requeue됨
 * - application.yml의 retry 설정에 따라 재시도
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderRepository orderRepository;

    /**
     * 주문 메시지 수신 및 처리
     *
     * 트랜잭션 내에서 주문 상태를 PROCESSING으로 변경합니다.
     *
     * @param orderMessage 주문 메시지
     */
    @Transactional
    @RabbitListener(queues = "${app.rabbitmq.transaction.queue-name}")
    public void processOrder(OrderMessage orderMessage) {
        log.info("=== 주문 메시지 수신 ===");
        log.info("주문 번호: {}", orderMessage.getOrderNumber());

        try {
            // 주문 조회
            Order order = orderRepository.findByOrderNumber(orderMessage.getOrderNumber())
                    .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + orderMessage.getOrderNumber()));

            log.info("주문 조회 완료 - 현재 상태: {}", order.getStatus());

            // 주문 처리 로직 (예: 재고 확인, 결제 처리 등)
            processOrderLogic(order);

            // 주문 상태 업데이트
            order.setStatus(Order.OrderStatus.PROCESSING);
            orderRepository.save(order);

            log.info("주문 처리 완료 - 변경된 상태: {}", order.getStatus());
            log.info("=== 주문 메시지 처리 완료 ===");

        } catch (Exception e) {
            log.error("주문 처리 실패 - 주문번호: {}", orderMessage.getOrderNumber(), e);
            // 예외 발생 시 트랜잭션 롤백 및 메시지 Requeue
            throw new RuntimeException("주문 처리 실패", e);
        }
    }

    /**
     * 실제 주문 처리 비즈니스 로직
     *
     * 실제 환경에서는 다음과 같은 작업들이 수행될 수 있습니다:
     * - 재고 확인 및 차감
     * - 결제 처리
     * - 배송 준비
     * - 외부 시스템 연동
     *
     * @param order 주문
     */
    private void processOrderLogic(Order order) {
        log.info("주문 처리 로직 실행 중...");

        // 간단한 처리 시간 시뮬레이션
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 실제 비즈니스 로직 구현
        log.info("상품: {}, 수량: {}, 가격: {}",
                order.getProductName(), order.getQuantity(), order.getPrice());
    }
}
