package com.example.optimisticlock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Integer price;

    /**
     * JPA의 낙관적 락(Optimistic Locking)을 위한 버전 필드
     * - 엔티티가 수정될 때마다 자동으로 버전이 증가합니다
     * - UPDATE 쿼리 실행 시 WHERE 절에 version 조건이 자동으로 추가됩니다
     * - 버전이 일치하지 않으면 OptimisticLockException이 발생합니다
     */
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 재고 감소 메서드
     * @param quantity 감소시킬 수량
     * @throws IllegalArgumentException 재고가 부족한 경우
     */
    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalArgumentException(
                String.format("재고가 부족합니다. 현재 재고: %d, 요청 수량: %d", this.stock, quantity)
            );
        }
        this.stock -= quantity;
    }

    /**
     * 재고 증가 메서드
     * @param quantity 증가시킬 수량
     */
    public void increaseStock(int quantity) {
        this.stock += quantity;
    }

    /**
     * 가격 변경 메서드
     * @param newPrice 새로운 가격
     */
    public void updatePrice(int newPrice) {
        if (newPrice < 0) {
            throw new IllegalArgumentException("가격은 0보다 작을 수 없습니다.");
        }
        this.price = newPrice;
    }
}
