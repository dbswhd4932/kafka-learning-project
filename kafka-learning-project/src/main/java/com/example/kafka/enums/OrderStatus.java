package com.example.kafka.enums;

/**
 * 주문 상태
 */
public enum OrderStatus {
    PENDING("진행중"),
    SUCCESS("성공"),
    FAILED("실패"),
    CANCELLED("취소됨");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
