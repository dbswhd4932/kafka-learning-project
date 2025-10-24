package com.example.kafka.consumer;

import com.example.kafka.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Order Consumer 서비스
 * - Kafka 토픽에서 주문 이벤트를 소비하여 처리
 */
@Slf4j
@Service
public class OrderConsumer {

    /**
     * 기본 주문 Consumer
     * - 토픽: sales-orders
     * - Consumer Group: kafka-learning-group
     * - 3개의 스레드로 병렬 처리 (파티션 3개)
     */
    @KafkaListener(topics = "sales-orders", groupId = "kafka-learning-group")
    public void consumeOrder(ConsumerRecord<String, Order> record) {
        Order order = record.value();

        log.info("========================================");
        log.info("Consumed Order from Kafka");
        log.info("Topic: {}", record.topic());
        log.info("Partition: {}", record.partition());
        log.info("Offset: {}", record.offset());
        log.info("Key: {}", record.key());
        log.info("Order: {}", order);
        log.info("========================================");

        // 비즈니스 로직 처리
        processOrder(order);
    }

    /**
     * 상세 메타데이터와 함께 주문 Consumer
     * - @Payload: 메시지 본문
     * - @Header: 메시지 헤더 정보
     */
    @KafkaListener(topics = "sales-orders", groupId = "order-analytics-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeOrderWithMetadata(
            @Payload Order order,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp
    ) {
        log.info("========================================");
        log.info("Analytics Consumer - Received Order");
        log.info("Topic: {}", topic);
        log.info("Partition: {}", partition);
        log.info("Offset: {}", offset);
        log.info("Timestamp: {}", timestamp);
        log.info("Order ID: {}", order.getOrderId());
        log.info("Customer ID: {}", order.getCustomerId());
        log.info("Total Amount: {}", order.getTotalAmount());
        log.info("========================================");

        // 분석 로직 처리
        analyzeOrder(order);
    }

    /**
     * 고액 주문 필터링 Consumer
     * - 1,000,000원 이상 주문만 처리
     */
    @KafkaListener(topics = "high-value-orders", groupId = "high-value-order-group")
    public void consumeHighValueOrders(Order order) {
        if (order.getTotalAmount().longValue() >= 1000000) {
            log.warn("========================================");
            log.warn("HIGH VALUE ORDER DETECTED!");
            log.warn("Order ID: {}", order.getOrderId());
            log.warn("Customer ID: {}", order.getCustomerId());
            log.warn("Total Amount: {}", order.getTotalAmount());
            log.warn("========================================");

            // 고액 주문 알림 발송 등
            notifyHighValueOrder(order);
        }
    }

    /**
     * 주문 처리 비즈니스 로직
     */
    private void processOrder(Order order) {
        log.info("Processing order: {}", order.getOrderId());

        // 실제 비즈니스 로직 구현
        // - 재고 확인
        // - 결제 처리
        // - 배송 요청
        // - 데이터베이스 저장 등

        log.info("Order processing completed: {}", order.getOrderId());
    }

    /**
     * 주문 분석 로직
     */
    private void analyzeOrder(Order order) {
        log.info("Analyzing order for analytics: {}", order.getOrderId());

        // 실제 분석 로직 구현
        // - 매출 통계 집계
        // - 인기 상품 분석
        // - 고객 구매 패턴 분석 등
    }

    /**
     * 고액 주문 알림
     */
    private void notifyHighValueOrder(Order order) {
        log.info("Sending notification for high-value order: {}", order.getOrderId());

        // 실제 알림 로직 구현
        // - 관리자 이메일 발송
        // - Slack 알림
        // - SMS 발송 등
    }
}
