package com.example.kafka.producer;

import com.example.kafka.common.KafkaProducerCluster;
import com.example.kafka.domain.Order;
import com.example.kafka.enums.MessageCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Order Producer 서비스
 * - 이제는 이벤트 기반으로 처리하므로 이 클래스는 사용하지 않음
 * - OrderService + SalesOrderEventListener 사용
 * - 레거시 호환성을 위해 남겨둠
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaProducerCluster kafkaProducerCluster;

    /**
     * 판매 주문 이벤트 발행 (비동기)
     * - MessageCategory.SALES_ORDER 토픽으로 전송
     * @deprecated Use OrderService.createOrder() instead
     */
    @Deprecated
    public void sendOrder(Order order) {
        log.info("Publishing sales order event: {}", order);
        kafkaProducerCluster.sendMessage(order.getOrderId(), order, MessageCategory.SALES_ORDER);
    }
}

