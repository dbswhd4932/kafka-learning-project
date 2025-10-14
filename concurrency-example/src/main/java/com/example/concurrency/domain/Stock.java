package com.example.concurrency.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고(Stock) 엔티티
 *
 * 이 엔티티는 상품의 재고 정보를 관리합니다.
 * 동시성 문제를 학습하기 위한 목적으로 설계되었습니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 상품 ID
     */
    private Long productId;

    /**
     * 재고 수량
     * 여러 스레드가 동시에 접근할 때 Race Condition이 발생할 수 있는 필드
     */
    private Long quantity;

    /**
     * 생성자
     * @param productId 상품 ID
     * @param quantity 초기 재고 수량
     */
    public Stock(Long productId, Long quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    /**
     * 재고 감소 메서드
     *
     * 주의: 이 메서드 자체는 thread-safe하지 않습니다!
     * 여러 스레드가 동시에 이 메서드를 호출하면 Race Condition이 발생합니다.
     *
     * Race Condition 발생 시나리오:
     * 1. Thread A가 quantity 값을 읽음 (예: 100)
     * 2. Thread B가 quantity 값을 읽음 (예: 100)
     * 3. Thread A가 quantity를 감소시킴 (100 - 1 = 99)
     * 4. Thread B가 quantity를 감소시킴 (100 - 1 = 99) <- Thread A의 변경을 덮어씀
     * 5. 결과: 2번 감소해야 하는데 1번만 감소됨
     *
     * @param quantity 감소시킬 재고 수량
     * @throws IllegalArgumentException 재고가 음수가 되는 경우
     */
    public void decrease(Long quantity) {
        if (this.quantity - quantity < 0) {
            throw new IllegalArgumentException("재고는 0개 미만이 될 수 없습니다.");
        }
        this.quantity -= quantity;
    }
}
