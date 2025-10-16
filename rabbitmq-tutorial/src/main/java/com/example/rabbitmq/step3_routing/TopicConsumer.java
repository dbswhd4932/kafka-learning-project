package com.example.rabbitmq.step3_routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Step 3-2: Topic Exchange Consumer
 *
 * Topic Exchange로부터 패턴 매칭된 메시지를 수신합니다.
 */
@Slf4j
@Component
public class TopicConsumer {

    /**
     * Order 관련 메시지 수신 (order.*)
     *
     * 매칭되는 Routing Key:
     * - order.created
     * - order.updated
     * - order.deleted
     * 등등...
     */
    @RabbitListener(queues = TopicExchangeConfig.TOPIC_QUEUE_ORDER)
    public void receiveOrderMessages(RoutingMessage message) {
        log.info("========================================");
        log.info("[TOPIC - ORDER] 메시지 수신");
        log.info("Message ID: {}", message.getMessageId());
        log.info("Routing Key: {}", message.getRoutingKey());
        log.info("Content: {}", message.getContent());
        log.info("Pattern: order.*");
        log.info("========================================");

        // Order 관련 작업 처리
        processOrderMessage(message);
    }

    /**
     * Payment 관련 메시지 수신 (*.payment.*)
     *
     * 매칭되는 Routing Key:
     * - order.payment.completed
     * - user.payment.failed
     * - subscription.payment.pending
     * 등등...
     */
    @RabbitListener(queues = TopicExchangeConfig.TOPIC_QUEUE_PAYMENT)
    public void receivePaymentMessages(RoutingMessage message) {
        log.info("========================================");
        log.info("[TOPIC - PAYMENT] 메시지 수신");
        log.info("Message ID: {}", message.getMessageId());
        log.info("Routing Key: {}", message.getRoutingKey());
        log.info("Content: {}", message.getContent());
        log.info("Pattern: *.payment.*");
        log.info("========================================");

        // Payment 관련 작업 처리
        processPaymentMessage(message);
    }

    /**
     * 모든 메시지 수신 (#)
     *
     * 매칭되는 Routing Key:
     * - 모든 Routing Key
     *
     * 이 Queue는 로깅이나 모니터링 용도로 사용할 수 있습니다.
     */
    @RabbitListener(queues = TopicExchangeConfig.TOPIC_QUEUE_ALL)
    public void receiveAllMessages(RoutingMessage message) {
        log.info("========================================");
        log.info("[TOPIC - ALL] 메시지 수신");
        log.info("Message ID: {}", message.getMessageId());
        log.info("Routing Key: {}", message.getRoutingKey());
        log.info("Content: {}", message.getContent());
        log.info("Pattern: #");
        log.info("========================================");

        // 로깅 또는 모니터링
        logAllMessages(message);
    }

    private void processOrderMessage(RoutingMessage message) {
        log.info("[ORDER] 주문 관련 메시지 처리 중...");
        // 주문 관련 비즈니스 로직
    }

    private void processPaymentMessage(RoutingMessage message) {
        log.info("[PAYMENT] 결제 관련 메시지 처리 중...");
        // 결제 관련 비즈니스 로직
    }

    private void logAllMessages(RoutingMessage message) {
        log.info("[ALL] 모든 메시지 로깅 중...");
        // 로깅 또는 모니터링 로직
    }
}
