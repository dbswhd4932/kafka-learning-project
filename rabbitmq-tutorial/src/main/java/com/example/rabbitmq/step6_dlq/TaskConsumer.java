package com.example.rabbitmq.step6_dlq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Step 6: Main Queue Consumer
 *
 * 작업 메시지를 처리하며, 실패 시 자동으로 DLQ로 이동합니다.
 */
@Slf4j
@Component
public class TaskConsumer {

    /**
     * Main Queue에서 메시지 수신 및 처리
     *
     * 처리 실패 시:
     * 1. 예외가 발생하여 NACK 전송
     * 2. application.yml의 retry 설정에 따라 재시도
     * 3. 최대 재시도 횟수 초과 시 자동으로 DLQ로 이동
     */
    @RabbitListener(queues = "${app.rabbitmq.dlq.main-queue}")
    public void processTask(TaskMessage taskMessage) {
        log.info("========================================");
        log.info("[MAIN] 작업 메시지 수신");
        log.info("Task ID: {}", taskMessage.getTaskId());
        log.info("Content: {}", taskMessage.getTaskContent());
        log.info("Retry Count: {}", taskMessage.getRetryCount());
        log.info("========================================");

        try {
            // 작업 처리
            processTaskLogic(taskMessage);

            log.info("[MAIN] 작업 처리 완료");

        } catch (Exception e) {
            log.error("[MAIN] 작업 처리 실패: {}", e.getMessage());
            // 예외를 던져서 재시도하도록 함
            throw new RuntimeException("작업 처리 실패", e);
        }
    }

    /**
     * 작업 처리 로직
     */
    private void processTaskLogic(TaskMessage taskMessage) {
        log.info("[MAIN] 작업 처리 중...");

        // 실패 시뮬레이션 (테스트용)
        if (Boolean.TRUE.equals(taskMessage.getShouldFail())) {
            log.error("[MAIN] 작업 처리 실패 (시뮬레이션)");
            throw new RuntimeException("작업 처리 실패 (의도적 실패)");
        }

        // 실제 비즈니스 로직
        log.info("[MAIN] 작업 내용: {}", taskMessage.getTaskContent());

        // 처리 시간 시뮬레이션
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[MAIN] 작업 처리 성공");
    }
}
