package com.example.rabbitmq.step2_transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 메시지 DTO
 *
 * RabbitMQ를 통해 전송될 주문 메시지의 데이터 구조를 정의합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage {

    /**
     * 주문 번호
     */
    private String orderNumber;

    /**
     * 고객 ID
     */
    private String customerId;

    /**
     * 상품명
     */
    private String productName;

    /**
     * 수량
     */
    private Integer quantity;

    /**
     * 가격
     */
    private BigDecimal price;

    /**
     * 메시지 전송 시간
     */
    private LocalDateTime sentAt;

    /**
     * Order 엔티티로부터 생성
     */
    public static OrderMessage from(Order order) {
        return new OrderMessage(
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getProductName(),
                order.getQuantity(),
                order.getPrice(),
                LocalDateTime.now()
        );
    }

    /**
     * Order 엔티티로 변환
     */
    public Order toEntity() {
        Order order = new Order();
        order.setOrderNumber(this.orderNumber);
        order.setCustomerId(this.customerId);
        order.setProductName(this.productName);
        order.setQuantity(this.quantity);
        order.setPrice(this.price);
        order.setStatus(Order.OrderStatus.PENDING);
        return order;
    }
}
