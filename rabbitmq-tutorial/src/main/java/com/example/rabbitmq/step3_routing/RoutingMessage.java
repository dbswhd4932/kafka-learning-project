package com.example.rabbitmq.step3_routing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Routing 메시지 DTO
 *
 * Exchange 타입별 라우팅 예제를 위한 공통 메시지 구조
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutingMessage {

    /**
     * 메시지 ID
     */
    private String messageId;

    /**
     * Exchange 타입
     */
    private String exchangeType;

    /**
     * Routing Key
     */
    private String routingKey;

    /**
     * 메시지 내용
     */
    private String content;

    /**
     * 발신 시간
     */
    private LocalDateTime sentAt;

    /**
     * 간편 생성자
     */
    public RoutingMessage(String exchangeType, String routingKey, String content) {
        this.messageId = java.util.UUID.randomUUID().toString();
        this.exchangeType = exchangeType;
        this.routingKey = routingKey;
        this.content = content;
        this.sentAt = LocalDateTime.now();
    }
}
