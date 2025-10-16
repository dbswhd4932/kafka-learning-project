package com.example.rabbitmq.step1_basic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Step 1: 기본 메시지 Producer (생산자)
 *
 * RabbitMQ에 메시지를 전송하는 역할을 담당합니다.
 *
 * RabbitTemplate을 사용하여 메시지를 전송합니다:
 * - convertAndSend(exchange, routingKey, message)
 *
 * 동작 순서:
 * 1. Java 객체를 JSON으로 변환 (MessageConverter)
 * 2. Exchange에 Routing Key와 함께 메시지 전송
 * 3. Exchange가 Routing Key를 확인하여 적절한 Queue로 라우팅
 * 4. Queue에 메시지 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BasicProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.basic.exchange-name}")
    private String exchangeName;

    @Value("${app.rabbitmq.basic.routing-key}")
    private String routingKey;

    /**
     * 메시지 전송
     *
     * @param message 전송할 메시지
     */
    public void sendMessage(BasicMessage message) {
        log.info("=== 메시지 전송 시작 ===");
        log.info("Exchange: {}", exchangeName);
        log.info("Routing Key: {}", routingKey);
        log.info("Message: {}", message);

        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);

        log.info("=== 메시지 전송 완료 ===");
    }

    /**
     * 간단한 텍스트 메시지 전송 (편의 메서드)
     *
     * @param content 메시지 내용
     * @param sender 발신자
     */
    public void sendSimpleMessage(String content, String sender) {
        BasicMessage message = new BasicMessage(content, sender);
        sendMessage(message);
    }
}
