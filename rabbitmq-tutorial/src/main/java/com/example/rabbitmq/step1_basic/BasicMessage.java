package com.example.rabbitmq.step1_basic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 기본 메시지 DTO
 *
 * RabbitMQ를 통해 전송될 메시지의 데이터 구조를 정의합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasicMessage {

    /**
     * 메시지 ID
     */
    private String messageId;

    /**
     * 메시지 내용
     */
    private String content;

    /**
     * 발신자
     */
    private String sender;

    /**
     * 메시지 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 간편 생성자
     */
    public BasicMessage(String content, String sender) {
        this.messageId = java.util.UUID.randomUUID().toString();
        this.content = content;
        this.sender = sender;
        this.createdAt = LocalDateTime.now();
    }
}
