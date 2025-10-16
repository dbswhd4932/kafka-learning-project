package com.example.rabbitmq.step3_routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Step 3-1: Direct Exchange Consumer
 *
 * Direct Exchange로부터 우선순위별로 메시지를 수신합니다.
 */
@Slf4j
@Component
public class DirectConsumer {

    /**
     * High Priority 메시지 수신
     */
    @RabbitListener(queues = DirectExchangeConfig.DIRECT_QUEUE_HIGH)
    public void receiveHighPriority(RoutingMessage message) {
        log.info("========================================");
        log.info("[DIRECT - HIGH] 메시지 수신");
        log.info("Message ID: {}", message.getMessageId());
        log.info("Routing Key: {}", message.getRoutingKey());
        log.info("Content: {}", message.getContent());
        log.info("========================================");

        // High Priority 작업 처리
        processHighPriorityTask(message);
    }

    /**
     * Medium Priority 메시지 수신
     */
    @RabbitListener(queues = DirectExchangeConfig.DIRECT_QUEUE_MEDIUM)
    public void receiveMediumPriority(RoutingMessage message) {
        log.info("========================================");
        log.info("[DIRECT - MEDIUM] 메시지 수신");
        log.info("Message ID: {}", message.getMessageId());
        log.info("Routing Key: {}", message.getRoutingKey());
        log.info("Content: {}", message.getContent());
        log.info("========================================");

        // Medium Priority 작업 처리
        processMediumPriorityTask(message);
    }

    /**
     * Low Priority 메시지 수신
     */
    @RabbitListener(queues = DirectExchangeConfig.DIRECT_QUEUE_LOW)
    public void receiveLowPriority(RoutingMessage message) {
        log.info("========================================");
        log.info("[DIRECT - LOW] 메시지 수신");
        log.info("Message ID: {}", message.getMessageId());
        log.info("Routing Key: {}", message.getRoutingKey());
        log.info("Content: {}", message.getContent());
        log.info("========================================");

        // Low Priority 작업 처리
        processLowPriorityTask(message);
    }

    private void processHighPriorityTask(RoutingMessage message) {
        log.info("[HIGH] 우선순위 높은 작업 처리 중...");
        // 긴급한 작업 처리
    }

    private void processMediumPriorityTask(RoutingMessage message) {
        log.info("[MEDIUM] 일반 우선순위 작업 처리 중...");
        // 일반 작업 처리
    }

    private void processLowPriorityTask(RoutingMessage message) {
        log.info("[LOW] 우선순위 낮은 작업 처리 중...");
        // 덜 긴급한 작업 처리
    }
}
