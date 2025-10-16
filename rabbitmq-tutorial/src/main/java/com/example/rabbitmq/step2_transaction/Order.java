package com.example.rabbitmq.step2_transaction;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 엔티티
 *
 * 데이터베이스에 저장될 주문 정보를 나타냅니다.
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 주문 번호
     */
    @Column(nullable = false, unique = true)
    private String orderNumber;

    /**
     * 고객 ID
     */
    @Column(nullable = false)
    private String customerId;

    /**
     * 상품명
     */
    @Column(nullable = false)
    private String productName;

    /**
     * 수량
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * 가격
     */
    @Column(nullable = false)
    private BigDecimal price;

    /**
     * 주문 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    /**
     * 생성 시간
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 생성 시 자동으로 시간 설정
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 수정 시 자동으로 시간 설정
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문 상태
     */
    public enum OrderStatus {
        PENDING,    // 대기
        PROCESSING, // 처리 중
        COMPLETED,  // 완료
        FAILED      // 실패
    }
}
