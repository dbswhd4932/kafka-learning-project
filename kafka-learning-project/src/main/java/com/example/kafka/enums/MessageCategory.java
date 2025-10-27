package com.example.kafka.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 메시지 카테고리 Enum
 * - Kafka 토픽 카테고리 정의
 * - 타입 안전성을 제공하여 오타 방지
 */
@Getter
@RequiredArgsConstructor
public enum MessageCategory {

    // 주문 관련
    SALES_ORDER("판매 주문"),

    // 성공/실패 추적
    ORDER_SUCCESS("주문 성공"),
    ORDER_FAILURE("주문 실패"),

    // Kafka Streams 출력
    HIGH_VALUE_ORDERS("고액 주문");

    private final String description;
}
