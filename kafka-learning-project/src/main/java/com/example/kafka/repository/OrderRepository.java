package com.example.kafka.repository;

import com.example.kafka.entity.OrderEntity;
import com.example.kafka.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 주문 Repository
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * 주문 ID로 조회
     */
    Optional<OrderEntity> findByOrderId(String orderId);

    /**
     * 주문 상태로 조회
     */
    List<OrderEntity> findByOrderStatus(OrderStatus orderStatus);

    /**
     * 주문 성공 여부로 조회
     */
    List<OrderEntity> findByOrderSuccessYn(Boolean orderSuccessYn);

    /**
     * 고객 ID로 조회
     */
    List<OrderEntity> findByCustomerId(String customerId);
}
