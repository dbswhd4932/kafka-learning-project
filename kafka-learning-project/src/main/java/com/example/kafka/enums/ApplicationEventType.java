package com.example.kafka.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 애플리케이션 이벤트 타입
 * - 실패 추적을 위한 이벤트 분류
 */
@Getter
@RequiredArgsConstructor
public enum ApplicationEventType {

    SALES_ORDER("판매 주문"),
    ORDER_SUCCESS("주문 성공"),
    ORDER_FAILURE("주문 실패");

    private final String description;
}
