package com.example.rabbitmq.step3_routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Step 3-3: Fanout Exchange Consumer
 *
 * Fanout Exchange로부터 브로드캐스트된 메시지를 수신합니다.
 * 모든 Consumer가 동일한 메시지를 받습니다.
 */
@Slf4j
@Component
public class FanoutConsumer {

    /**
     * Fanout Queue 1 메시지 수신
     *
     * Fanout Exchange에 전송된 모든 메시지를 받습니다.
     */
    @RabbitListener(queues = FanoutExchangeConfig.FANOUT_QUEUE_1)
    public void receiveFromQueue1(RoutingMessage message) {
        log.info("========================================");
        log.info("[FANOUT - QUEUE 1] 메시지 수신");
        log.info("Message ID: {}", message.getMessageId());
        log.info("Content: {}", message.getContent());
        log.info("========================================");

        // Queue 1 전용 작업 (예: 이메일 발송)
        processQueue1Task(message);
    }

    /**
     * Fanout Queue 2 메시지 수신
     *
     * Fanout Exchange에 전송된 모든 메시지를 받습니다.
     */
    @RabbitListener(queues = FanoutExchangeConfig.FANOUT_QUEUE_2)
    public void receiveFromQueue2(RoutingMessage message) {
        log.info("========================================");
        log.info("[FANOUT - QUEUE 2] 메시지 수신");
        log.info("Message ID: {}", message.getMessageId());
        log.info("Content: {}", message.getContent());
        log.info("========================================");

        // Queue 2 전용 작업 (예: SMS 발송)
        processQueue2Task(message);
    }

    /**
     * Fanout Queue 3 메시지 수신
     *
     * Fanout Exchange에 전송된 모든 메시지를 받습니다.
     */
    @RabbitListener(queues = FanoutExchangeConfig.FANOUT_QUEUE_3)
    public void receiveFromQueue3(RoutingMessage message) {
        log.info("========================================");
        log.info("[FANOUT - QUEUE 3] 메시지 수신");
        log.info("Message ID: {}", message.getMessageId());
        log.info("Content: {}", message.getContent());
        log.info("========================================");

        // Queue 3 전용 작업 (예: 푸시 알림 발송)
        processQueue3Task(message);
    }

    private void processQueue1Task(RoutingMessage message) {
        log.info("[QUEUE 1] 이메일 발송 시뮬레이션...");
        // 이메일 발송 로직
    }

    private void processQueue2Task(RoutingMessage message) {
        log.info("[QUEUE 2] SMS 발송 시뮬레이션...");
        // SMS 발송 로직
    }

    private void processQueue3Task(RoutingMessage message) {
        log.info("[QUEUE 3] 푸시 알림 발송 시뮬레이션...");
        // 푸시 알림 발송 로직
    }
}
