package com.example.kafka.message;

import com.example.kafka.domain.Order;
import com.example.kafka.event.SalesOrderEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kafka로 전송할 판매 주문 메시지
 * - Event를 Kafka 메시지 포맷으로 변환
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderMessage {

    private String orderId;
    private String customerId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime orderDateTime;
    private LocalDateTime publishedAt;  // 이벤트 발행 시간

    /**
     * SalesOrderEvent를 SalesOrderMessage로 변환
     */
    public static SalesOrderMessage toMessage(SalesOrderEvent event) {
        Order order = event.getOrder();

        return SalesOrderMessage.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .orderDateTime(order.getOrderDateTime())
                .publishedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Order 객체를 SalesOrderMessage로 변환
     */
    public static SalesOrderMessage from(Order order) {
        return SalesOrderMessage.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .orderDateTime(order.getOrderDateTime())
                .publishedAt(LocalDateTime.now())
                .build();
    }
}
