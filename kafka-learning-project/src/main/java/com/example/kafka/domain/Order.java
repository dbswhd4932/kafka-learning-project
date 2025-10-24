package com.example.kafka.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 도메인 모델
 * Kafka를 통해 전송될 이벤트 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private String orderId;
    private String customerId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime orderDateTime;
}
