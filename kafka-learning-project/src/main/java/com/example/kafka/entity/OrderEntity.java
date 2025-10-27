package com.example.kafka.entity;

import com.example.kafka.converter.BooleanToYNConverter;
import com.example.kafka.entity.base.BaseEntity;
import com.example.kafka.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 Entity
 * - BaseEntity 상속 (감사 정보 + 논리 삭제)
 * - 주문 성공 여부 관리
 */
@Entity
@Table(name = "orders")
@Where(clause = "delete_yn = 'N'")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 주문 ID (비즈니스 키)
     */
    @Column(name = "order_id", nullable = false, unique = true, length = 50, columnDefinition = "varchar(50) COMMENT '주문 ID'")
    private String orderId;

    /**
     * 고객 ID
     */
    @Column(name = "customer_id", nullable = false, length = 100, columnDefinition = "varchar(100) COMMENT '고객 ID'")
    private String customerId;

    /**
     * 상품 ID
     */
    @Column(name = "product_id", nullable = false, length = 100, columnDefinition = "varchar(100) COMMENT '상품 ID'")
    private String productId;

    /**
     * 상품명
     */
    @Column(name = "product_name", nullable = false, length = 200, columnDefinition = "varchar(200) COMMENT '상품명'")
    private String productName;

    /**
     * 수량
     */
    @Column(name = "quantity", nullable = false, columnDefinition = "int COMMENT '수량'")
    private Integer quantity;

    /**
     * 단가
     */
    @Column(name = "price", nullable = false, precision = 15, scale = 2, columnDefinition = "decimal(15,2) COMMENT '단가'")
    private BigDecimal price;

    /**
     * 총액
     */
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2, columnDefinition = "decimal(15,2) COMMENT '총액'")
    private BigDecimal totalAmount;

    /**
     * 주문 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20, columnDefinition = "varchar(20) COMMENT '주문상태'")
    private OrderStatus orderStatus;

    /**
     * 주문 성공 여부
     * - Y: 성공
     * - N: 실패
     */
    @Builder.Default
    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "order_success_yn", columnDefinition = "char(1) DEFAULT 'N' COMMENT '주문성공여부'", nullable = false)
    private Boolean orderSuccessYn = Boolean.FALSE;

    /**
     * 주문 일시
     */
    @Column(name = "order_datetime", nullable = false, columnDefinition = "datetime COMMENT '주문일시'")
    private LocalDateTime orderDatetime;

    /**
     * 실패 사유
     */
    @Column(name = "failure_reason", columnDefinition = "text COMMENT '실패사유'")
    private String failureReason;

    // ===== 비즈니스 메서드 =====

    /**
     * 주문 상태를 성공으로 변경
     */
    public void markAsSuccess() {
        this.orderStatus = OrderStatus.SUCCESS;
        this.orderSuccessYn = Boolean.TRUE;
        this.failureReason = null;
    }

    /**
     * 주문 상태를 실패로 변경
     */
    public void markAsFailed(String reason) {
        this.orderStatus = OrderStatus.FAILED;
        this.orderSuccessYn = Boolean.FALSE;
        this.failureReason = reason;
    }

    /**
     * 결제 진행 중 상태로 변경
     */
    public void markAsPending() {
        this.orderStatus = OrderStatus.PENDING;
        this.orderSuccessYn = Boolean.FALSE;
    }

    /**
     * 주문 취소
     */
    public void cancel(String reason) {
        this.orderStatus = OrderStatus.CANCELLED;
        this.orderSuccessYn = Boolean.FALSE;
        this.failureReason = reason;
    }
}
