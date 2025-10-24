package com.example.kafka.enums;

/**
 * 사용자 유형
 * - 등록자/수정자 유형 구분
 */
public enum UserType {
    SYSTEM("시스템"),
    ADMIN("관리자"),
    USER("일반 사용자"),
    API("API");

    private final String description;

    UserType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
