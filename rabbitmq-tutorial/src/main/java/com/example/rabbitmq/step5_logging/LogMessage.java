package com.example.rabbitmq.step5_logging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 로그 메시지 DTO
 *
 * Topic Exchange를 사용한 로그 수집 시스템에서 사용하는 메시지 구조
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogMessage {

    /**
     * 로그 ID
     */
    private String logId;

    /**
     * 로그 레벨
     */
    private LogLevel level;

    /**
     * 서비스명
     */
    private String serviceName;

    /**
     * 로그 메시지
     */
    private String message;

    /**
     * 추가 정보
     */
    private String additionalInfo;

    /**
     * 발생 시간
     */
    private LocalDateTime timestamp;

    /**
     * 간편 생성자
     */
    public LogMessage(LogLevel level, String serviceName, String message) {
        this.logId = java.util.UUID.randomUUID().toString();
        this.level = level;
        this.serviceName = serviceName;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 로그 레벨
     */
    public enum LogLevel {
        ERROR,   // 에러
        WARN,    // 경고
        INFO,    // 정보
        DEBUG    // 디버그
    }

    /**
     * Routing Key 생성
     * 패턴: {serviceName}.{level}
     */
    public String getRoutingKey() {
        return serviceName.toLowerCase() + "." + level.name().toLowerCase();
    }
}
