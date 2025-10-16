package com.example.rabbitmq.step6_dlq;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Step 6: Dead Letter Queue(DLQ)와 Retry 재처리 설정
 *
 * Dead Letter Queue란?
 * - 처리 실패한 메시지가 이동하는 특별한 큐
 * - 메시지가 다음 상황에서 DLQ로 이동합니다:
 *   1. Consumer에서 예외 발생 후 최대 재시도 횟수 초과
 *   2. 메시지가 거부(NACK)되고 재큐잉되지 않을 때
 *   3. 메시지 TTL(Time To Live) 만료
 *
 * 구조:
 * Producer → Main Exchange → Main Queue → Consumer (처리 실패)
 *                              ↓
 *                          DLQ Exchange → DLQ Queue → DLQ Consumer
 *
 * DLQ의 장점:
 * 1. 실패한 메시지 보존 및 분석
 * 2. 시스템 안정성 향상
 * 3. 재처리 기회 제공
 * 4. 디버깅 및 모니터링 용이
 *
 * 실전 활용:
 * - 결제 실패 메시지 재처리
 * - 외부 API 호출 실패 재시도
 * - 일시적 장애 대응
 */
@Configuration
public class DlqConfig {

    @Value("${app.rabbitmq.dlq.main-queue}")
    private String mainQueueName;

    @Value("${app.rabbitmq.dlq.dlq-queue}")
    private String dlqQueueName;

    @Value("${app.rabbitmq.dlq.dlq-exchange}")
    private String dlqExchangeName;

    @Value("${app.rabbitmq.dlq.parking-lot-queue}")
    private String parkingLotQueueName;

    /**
     * Main Queue 생성
     *
     * x-dead-letter-exchange: 메시지 처리 실패 시 이동할 Exchange
     * x-dead-letter-routing-key: DLQ로 라우팅할 때 사용할 Routing Key
     */
    @Bean
    public Queue mainTaskQueue() {
        Map<String, Object> args = new HashMap<>();
        // Dead Letter Exchange 설정
        args.put("x-dead-letter-exchange", dlqExchangeName);
        args.put("x-dead-letter-routing-key", "dlq.task");
        // 메시지 TTL 설정 (선택사항): 30초 후 자동으로 DLQ로 이동
        // args.put("x-message-ttl", 30000);

        return new Queue(mainQueueName, true, false, false, args);
    }

    /**
     * Main Exchange 생성
     */
    @Bean
    public DirectExchange mainTaskExchange() {
        return new DirectExchange("main.task.exchange");
    }

    /**
     * Main Binding
     */
    @Bean
    public Binding mainTaskBinding(Queue mainTaskQueue, DirectExchange mainTaskExchange) {
        return BindingBuilder.bind(mainTaskQueue)
                .to(mainTaskExchange)
                .with("main.task");
    }

    /**
     * DLQ Exchange 생성
     */
    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(dlqExchangeName);
    }

    /**
     * DLQ Queue 생성
     */
    @Bean
    public Queue dlqQueue() {
        return new Queue(dlqQueueName, true);
    }

    /**
     * DLQ Binding
     */
    @Bean
    public Binding dlqBinding(Queue dlqQueue, DirectExchange dlqExchange) {
        return BindingBuilder.bind(dlqQueue)
                .to(dlqExchange)
                .with("dlq.task");
    }

    /**
     * Parking Lot Queue 생성
     *
     * DLQ에서도 처리할 수 없는 메시지가 최종적으로 이동하는 큐
     * 수동 처리나 분석을 위해 보관됩니다.
     */
    @Bean
    public Queue parkingLotQueue() {
        return new Queue(parkingLotQueueName, true);
    }

    /**
     * Parking Lot Binding
     */
    @Bean
    public Binding parkingLotBinding(Queue parkingLotQueue, DirectExchange dlqExchange) {
        return BindingBuilder.bind(parkingLotQueue)
                .to(dlqExchange)
                .with("parking.lot");
    }
}
