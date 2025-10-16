package com.example.rabbitmq.step6_dlq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Step 6: Parking Lot Consumer
 *
 * 자동으로 처리할 수 없는 메시지를 수신하여 로깅하고 보관합니다.
 *
 * Parking Lot의 용도:
 * 1. 자동 처리 불가능한 메시지 보관
 * 2. 수동 처리를 위한 대기
 * 3. 실패 원인 분석
 * 4. 시스템 개선을 위한 데이터 수집
 */
@Slf4j
@Component
public class ParkingLotConsumer {

    /**
     * Parking Lot 메시지 수신
     */
    @RabbitListener(queues = "${app.rabbitmq.dlq.parking-lot-queue}")
    public void processParkingLotMessage(TaskMessage taskMessage) {
        log.error("========================================");
        log.error("[PARKING LOT] 최종 실패 메시지 수신");
        log.error("Task ID: {}", taskMessage.getTaskId());
        log.error("Content: {}", taskMessage.getTaskContent());
        log.error("Retry Count: {}", taskMessage.getRetryCount());
        log.error("Created At: {}", taskMessage.getCreatedAt());
        log.error("========================================");

        // Parking Lot 메시지 처리
        handleParkingLotMessage(taskMessage);

        log.error("[PARKING LOT] 메시지 보관 완료 - 수동 처리 또는 분석 필요");
    }

    /**
     * Parking Lot 메시지 처리
     *
     * 실제 환경에서는 다음과 같은 작업을 수행:
     * 1. 데이터베이스에 실패 이력 저장
     * 2. 알림 발송 (Slack, Email 등)
     * 3. 모니터링 시스템에 전송
     * 4. 로그 파일에 상세 기록
     * 5. 통계 수집
     */
    private void handleParkingLotMessage(TaskMessage taskMessage) {
        log.error("[PARKING LOT] 실패 메시지 분석 중...");

        // 실패 원인 로깅
        log.error("[PARKING LOT] 실패 원인: 최대 재시도 횟수 초과");
        log.error("[PARKING LOT] 재시도 횟수: {}", taskMessage.getRetryCount());

        // 실제 환경에서는 다음과 같은 처리를 합니다:
        // 1. DB에 저장
        // 2. 관리자에게 알림
        // 3. 수동 처리 대기 큐에 추가
        // 4. 에러 리포트 생성

        log.error("[PARKING LOT] 수동 개입이 필요합니다.");
        log.error("[PARKING LOT] Task ID를 관리자에게 전달하세요: {}", taskMessage.getTaskId());
    }
}
