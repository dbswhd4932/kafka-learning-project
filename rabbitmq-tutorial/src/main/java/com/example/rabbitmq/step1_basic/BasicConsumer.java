package com.example.rabbitmq.step1_basic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Step 1: 기본 메시지 Consumer (소비자)
 *
 * RabbitMQ Queue에서 메시지를 수신하고 처리하는 역할을 담당합니다.
 *
 * @RabbitListener 어노테이션:
 * - queues: 수신할 Queue의 이름 (application.yml에서 정의)
 * - 메시지가 Queue에 도착하면 자동으로 이 메서드가 호출됩니다
 * - MessageConverter가 JSON을 자동으로 Java 객체로 변환
 *
 * Acknowledge (확인 응답):
 * - Auto Acknowledge (기본): 메서드가 정상적으로 종료되면 자동으로 ACK 전송
 * - Manual Acknowledge: 개발자가 직접 ACK/NACK 처리 (Step 6에서 설명)
 *
 * 메시지 처리 실패 시:
 * - 예외 발생 시 메시지는 Requeue되거나 Dead Letter Queue로 이동
 * - application.yml의 retry 설정에 따라 재시도
 */
@Slf4j
@Component
public class BasicConsumer {

    /**
     * 기본 큐에서 메시지 수신
     *
     * 이 메서드는 Queue에 메시지가 도착할 때마다 자동으로 호출됩니다.
     *
     * @param message 수신한 메시지 (JSON -> BasicMessage 객체로 자동 변환)
     */
    @RabbitListener(queues = "${app.rabbitmq.basic.queue-name}")
    public void receiveMessage(BasicMessage message) {
        log.info("=== 메시지 수신 ===");
        log.info("Message ID: {}", message.getMessageId());
        log.info("Content: {}", message.getContent());
        log.info("Sender: {}", message.getSender());
        log.info("Created At: {}", message.getCreatedAt());
        log.info("=== 메시지 처리 완료 ===");

        // 여기에 실제 비즈니스 로직을 구현합니다.
        // 예: 데이터베이스 저장, 외부 API 호출, 파일 처리 등
        processMessage(message);
    }

    /**
     * 메시지 처리 로직
     *
     * @param message 처리할 메시지
     */
    private void processMessage(BasicMessage message) {
        // 실제 비즈니스 로직 구현
        // 예제에서는 로그만 출력
        log.info("비즈니스 로직 처리: {}", message.getContent());

        // 실제 환경에서는 다음과 같은 작업들이 수행될 수 있습니다:
        // - 데이터베이스에 메시지 내용 저장
        // - 외부 서비스로 메시지 전달
        // - 파일 생성 또는 업로드
        // - 알림 발송
        // 등등...
    }
}
