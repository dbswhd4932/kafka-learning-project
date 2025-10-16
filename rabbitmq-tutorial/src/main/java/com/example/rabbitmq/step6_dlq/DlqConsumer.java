package com.example.rabbitmq.step6_dlq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Step 6: DLQ Consumer
 *
 * Main Queue에서 처리 실패한 메시지를 수신하여 재처리합니다.
 *
 * DLQ Consumer의 역할:
 * 1. 실패 원인 분석
 * 2. 재처리 가능 여부 판단
 * 3. 재처리 시도 또는 Parking Lot으로 이동
 * 4. 실패 로그 기록
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DlqConsumer {

    private final TaskProducer taskProducer;

    /**
     * 최대 재시도 횟수
     */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * DLQ에서 메시지 수신 및 재처리
     */
    @RabbitListener(queues = "${app.rabbitmq.dlq.dlq-queue}")
    public void processDlqMessage(TaskMessage taskMessage) {
        log.warn("========================================");
        log.warn("[DLQ] 실패 메시지 수신");
        log.warn("Task ID: {}", taskMessage.getTaskId());
        log.warn("Content: {}", taskMessage.getTaskContent());
        log.warn("Retry Count: {}", taskMessage.getRetryCount());
        log.warn("========================================");

        // 재시도 횟수 증가
        taskMessage.incrementRetryCount();

        try {
            // 재처리 가능 여부 판단
            if (canRetry(taskMessage)) {
                log.info("[DLQ] 재처리 시도 - 재시도 횟수: {}/{}", taskMessage.getRetryCount(), MAX_RETRY_COUNT);

                // 재처리 로직
                retryTask(taskMessage);

                log.info("[DLQ] 재처리 성공");

            } else {
                log.warn("[DLQ] 최대 재시도 횟수 초과 - Parking Lot으로 이동");

                // Parking Lot으로 이동
                moveToParkingLot(taskMessage);
            }

        } catch (Exception e) {
            log.error("[DLQ] 재처리 실패: {}", e.getMessage());

            // 재시도 가능하면 다시 DLQ에 넣기, 아니면 Parking Lot으로
            if (canRetry(taskMessage)) {
                log.info("[DLQ] 재시도 가능 - DLQ에 다시 추가");
                // 실제로는 여기서 delay를 주고 재전송할 수 있음
            } else {
                log.warn("[DLQ] 재시도 불가 - Parking Lot으로 이동");
                moveToParkingLot(taskMessage);
            }
        }
    }

    /**
     * 재시도 가능 여부 확인
     */
    private boolean canRetry(TaskMessage taskMessage) {
        Integer retryCount = taskMessage.getRetryCount();
        if (retryCount == null) {
            retryCount = 0;
        }
        return retryCount < MAX_RETRY_COUNT;
    }

    /**
     * 작업 재처리
     */
    private void retryTask(TaskMessage taskMessage) {
        log.info("[DLQ] 재처리 시작...");

        // 실제 환경에서는 다음과 같은 작업을 수행:
        // 1. 실패 원인 분석
        // 2. 데이터 복구 시도
        // 3. 외부 시스템 재연결
        // 4. 보상 트랜잭션 실행
        // 5. Main Queue로 다시 전송 (선택적)

        // 간단한 시뮬레이션
        try {
            Thread.sleep(2000); // 재처리 대기 시간
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[DLQ] 재처리 로직 실행 완료");
    }

    /**
     * Parking Lot으로 메시지 이동
     *
     * 더 이상 자동으로 처리할 수 없는 메시지를 보관합니다.
     * 수동 처리나 분석을 위해 사용됩니다.
     */
    private void moveToParkingLot(TaskMessage taskMessage) {
        log.warn("[DLQ] Parking Lot으로 메시지 이동");

        taskProducer.moveToParkingLot(taskMessage);

        log.warn("[DLQ] Parking Lot 이동 완료 - 수동 처리 필요");
    }
}
