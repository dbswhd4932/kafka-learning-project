package com.example.kafka.event;

import com.example.kafka.domain.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 판매 주문 Spring Event
 * - 주문 생성 시 발행되는 이벤트
 * - @TransactionalEventListener로 처리됨
 */
@Getter
public class SalesOrderEvent extends ApplicationEvent {

    private final Order order;

    public SalesOrderEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }

    /**
     * 정적 팩토리 메서드
     */
    public static SalesOrderEvent of(Object source, Order order) {
        return new SalesOrderEvent(source, order);
    }
}
