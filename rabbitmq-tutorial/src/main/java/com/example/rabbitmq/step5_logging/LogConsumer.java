package com.example.rabbitmq.step5_logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Step 5: 로그 메시지 Consumer
 *
 * 로그 레벨별로 메시지를 수신하고 처리합니다.
 */
@Slf4j
@Component
public class LogConsumer {

    /**
     * Error 로그 수신
     */
    @RabbitListener(queues = "${app.rabbitmq.logging.error-queue}")
    public void receiveErrorLog(LogMessage logMessage) {
        log.error("========================================");
        log.error("[ERROR LOG] 수신");
        log.error("Log ID: {}", logMessage.getLogId());
        log.error("Service: {}", logMessage.getServiceName());
        log.error("Message: {}", logMessage.getMessage());
        log.error("Timestamp: {}", logMessage.getTimestamp());
        log.error("========================================");

        // 에러 로그 처리 (예: 알림 발송, 특별 처리)
        handleErrorLog(logMessage);
    }

    /**
     * Warn 로그 수신
     */
    @RabbitListener(queues = "${app.rabbitmq.logging.warn-queue}")
    public void receiveWarnLog(LogMessage logMessage) {
        log.warn("========================================");
        log.warn("[WARN LOG] 수신");
        log.warn("Log ID: {}", logMessage.getLogId());
        log.warn("Service: {}", logMessage.getServiceName());
        log.warn("Message: {}", logMessage.getMessage());
        log.warn("Timestamp: {}", logMessage.getTimestamp());
        log.warn("========================================");

        // 경고 로그 처리
        handleWarnLog(logMessage);
    }

    /**
     * Info 로그 수신
     */
    @RabbitListener(queues = "${app.rabbitmq.logging.info-queue}")
    public void receiveInfoLog(LogMessage logMessage) {
        log.info("========================================");
        log.info("[INFO LOG] 수신");
        log.info("Log ID: {}", logMessage.getLogId());
        log.info("Service: {}", logMessage.getServiceName());
        log.info("Message: {}", logMessage.getMessage());
        log.info("Timestamp: {}", logMessage.getTimestamp());
        log.info("========================================");

        // 정보 로그 처리
        handleInfoLog(logMessage);
    }

    /**
     * 모든 로그 수신 (모니터링용)
     */
    @RabbitListener(queues = "${app.rabbitmq.logging.all-queue}")
    public void receiveAllLogs(LogMessage logMessage) {
        log.debug("========================================");
        log.debug("[ALL LOG] 수신");
        log.debug("Log ID: {}", logMessage.getLogId());
        log.debug("Level: {}", logMessage.getLevel());
        log.debug("Service: {}", logMessage.getServiceName());
        log.debug("Message: {}", logMessage.getMessage());
        log.debug("========================================");

        // 모든 로그 저장 (예: Elasticsearch, 파일 등)
        handleAllLogs(logMessage);
    }

    /**
     * 에러 로그 처리
     */
    private void handleErrorLog(LogMessage logMessage) {
        // 실제 환경에서는:
        // - 즉시 알림 발송 (Slack, Email 등)
        // - 에러 추적 시스템에 전송 (Sentry 등)
        // - 긴급 대응 프로세스 시작
        log.error("[ERROR HANDLER] 에러 로그 특별 처리 중...");
    }

    /**
     * 경고 로그 처리
     */
    private void handleWarnLog(LogMessage logMessage) {
        // 실제 환경에서는:
        // - 경고 알림 발송
        // - 모니터링 대시보드에 표시
        log.warn("[WARN HANDLER] 경고 로그 처리 중...");
    }

    /**
     * 정보 로그 처리
     */
    private void handleInfoLog(LogMessage logMessage) {
        // 실제 환경에서는:
        // - 통계 수집
        // - 로그 저장
        log.info("[INFO HANDLER] 정보 로그 처리 중...");
    }

    /**
     * 모든 로그 처리
     */
    private void handleAllLogs(LogMessage logMessage) {
        // 실제 환경에서는:
        // - Elasticsearch에 저장
        // - 로그 파일에 기록
        // - 실시간 분석
        log.debug("[ALL HANDLER] 모든 로그 저장 중...");
    }
}
