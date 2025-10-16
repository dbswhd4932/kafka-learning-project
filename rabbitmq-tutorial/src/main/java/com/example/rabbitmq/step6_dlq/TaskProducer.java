package com.example.rabbitmq.step6_dlq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Step 6: 작업 메시지 Producer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProducer {

    private final RabbitTemplate rabbitTemplate;

    private static final String MAIN_EXCHANGE = "main.task.exchange";
    private static final String MAIN_ROUTING_KEY = "main.task";
    private static final String DLQ_EXCHANGE = "dlq.exchange";
    private static final String PARKING_LOT_ROUTING_KEY = "parking.lot";

    /**
     * Main Queue로 작업 메시지 전송
     */
    public void sendTask(TaskMessage taskMessage) {
        log.info("=== 작업 메시지 전송 ===");
        log.info("Task ID: {}", taskMessage.getTaskId());
        log.info("Content: {}", taskMessage.getTaskContent());
        log.info("Should Fail: {}", taskMessage.getShouldFail());

        rabbitTemplate.convertAndSend(MAIN_EXCHANGE, MAIN_ROUTING_KEY, taskMessage);

        log.info("작업 메시지 전송 완료");
    }

    /**
     * 간편 작업 전송 (성공 케이스)
     */
    public void sendSuccessTask(String content) {
        sendTask(new TaskMessage(content, false));
    }

    /**
     * 간편 작업 전송 (실패 케이스 - 테스트용)
     */
    public void sendFailTask(String content) {
        sendTask(new TaskMessage(content, true));
    }

    /**
     * Parking Lot으로 메시지 이동
     */
    public void moveToParkingLot(TaskMessage taskMessage) {
        log.info("=== Parking Lot으로 메시지 이동 ===");
        log.info("Task ID: {}", taskMessage.getTaskId());

        rabbitTemplate.convertAndSend(DLQ_EXCHANGE, PARKING_LOT_ROUTING_KEY, taskMessage);

        log.info("Parking Lot으로 이동 완료");
    }
}
