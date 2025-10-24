package com.example.kafka.listener;

import com.example.kafka.common.KafkaProducerCluster;
import com.example.kafka.entity.ApplicationEventFailureEntity;
import com.example.kafka.enums.ApplicationEventType;
import com.example.kafka.enums.MessageCategory;
import com.example.kafka.event.SalesOrderEvent;
import com.example.kafka.message.SalesOrderMessage;
import com.example.kafka.repository.ApplicationEventFailureRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 판매 주문 이벤트 리스너
 * - 주문 이벤트를 받아서 Kafka로 발행
 * - 성공/실패를 구분하여 처리
 * - 실패 시 DB에 저장하여 추적
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOrderEventListener {

    private final KafkaProducerCluster kafkaProducerCluster;
    private final ApplicationEventFailureRepository failureRepository;
    private final ObjectMapper objectMapper;

    /**
     * 판매 주문 이벤트 처리
     * - @TransactionalEventListener: 트랜잭션 커밋 후 실행 (OrderService.publishSuccessEvent의 @Transactional 커밋 대기)
     * - @Async: 비동기 처리
     * - REQUIRES_NEW: 새로운 트랜잭션으로 실행 (실패 저장을 위해)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void handleSalesOrderEvent(SalesOrderEvent event) {
        log.info("========================================");
        log.info("Sales Order Event Received: {}", event.getOrder().getOrderId());
        log.info("========================================");

        try {
            // 1. Event를 Kafka Message로 변환
            SalesOrderMessage message = SalesOrderMessage.toMessage(event);

            // 2. Kafka로 메시지 발행 (sales-orders 토픽)
            kafkaProducerCluster.sendMessage(
                    message.getOrderId(),
                    message,
                    MessageCategory.SALES_ORDER
            );

            log.info("✅ Successfully published sales order to Kafka: {}", message.getOrderId());

            // 3. 성공 메시지도 별도 토픽으로 발행 (모니터링용)
            publishSuccessEvent(message);

        } catch (Exception e) {
            log.error("❌ Failed to publish sales order to Kafka", e);

            // 4. 실패 시 DB에 저장
            saveFailureEvent(event, e);

            // 5. 실패 메시지를 별도 토픽으로 발행
            publishFailureEvent(event, e);
        }
    }

    /**
     * 성공 이벤트 발행
     * - order-success 토픽으로 발행
     * - 성공한 주문들을 추적하기 위한 용도
     */
    private void publishSuccessEvent(SalesOrderMessage message) {
        try {
            kafkaProducerCluster.sendMessage(
                    message.getOrderId(),
                    message,
                    MessageCategory.ORDER_SUCCESS
            );
            log.info("✅ Success event published: {}", message.getOrderId());
        } catch (Exception e) {
            log.warn("Failed to publish success event (non-critical): {}", e.getMessage());
        }
    }

    /**
     * 실패 이벤트를 DB에 저장
     * - 재처리를 위해 원본 데이터 보관
     */
    private void saveFailureEvent(SalesOrderEvent event, Exception exception) {
        try {
            String payload = convertEventToJson(event);
            ApplicationEventFailureEntity failureEntity = ApplicationEventFailureEntity.toEntity(
                    payload,
                    exception.getMessage(),
                    ApplicationEventType.SALES_ORDER
            );

            failureRepository.save(failureEntity);
            log.info("💾 Failure event saved to database: {}", event.getOrder().getOrderId());

        } catch (Exception e) {
            log.error("Failed to save failure event to database", e);
        }
    }

    /**
     * 실패 이벤트 발행
     * - order-failure 토픽으로 발행
     * - 실패한 주문들을 추적하기 위한 용도
     */
    private void publishFailureEvent(SalesOrderEvent event, Exception exception) {
        try {
            SalesOrderMessage message = SalesOrderMessage.toMessage(event);

            // 실패 정보를 포함한 메시지 생성
            FailureMessage failureMessage = FailureMessage.builder()
                    .orderId(message.getOrderId())
                    .originalMessage(message)
                    .errorMessage(exception.getMessage())
                    .failedAt(java.time.LocalDateTime.now())
                    .build();

            kafkaProducerCluster.sendMessage(
                    failureMessage.getOrderId(),
                    failureMessage,
                    MessageCategory.ORDER_FAILURE
            );

            log.info("⚠️ Failure event published: {}", message.getOrderId());

        } catch (Exception e) {
            log.error("Failed to publish failure event to Kafka", e);
        }
    }

    /**
     * Event를 JSON으로 변환
     */
    private String convertEventToJson(SalesOrderEvent event) {
        try {
            return objectMapper.writeValueAsString(event.getOrder());
        } catch (Exception e) {
            log.error("Failed to convert event to JSON", e);
            return event.getOrder().toString();
        }
    }

    /**
     * 실패 메시지 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class FailureMessage {
        private String orderId;
        private SalesOrderMessage originalMessage;
        private String errorMessage;
        private java.time.LocalDateTime failedAt;
    }
}
