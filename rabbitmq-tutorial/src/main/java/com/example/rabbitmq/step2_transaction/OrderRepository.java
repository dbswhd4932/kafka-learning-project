package com.example.rabbitmq.step2_transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 주문 리포지토리
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 주문 번호로 조회
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * 고객 ID로 조회
     */
    List<Order> findByCustomerId(String customerId);

    /**
     * 주문 상태로 조회
     */
    List<Order> findByStatus(Order.OrderStatus status);
}
