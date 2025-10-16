package com.example.rabbitmq.step2_transaction;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Step 2: Transaction 처리를 위한 큐 설정
 *
 * DB 트랜잭션과 메시지 큐를 함께 사용할 때의 설정입니다.
 *
 * 중요 개념:
 * 1. DB 트랜잭션과 메시지 전송의 원자성
 *    - DB 저장이 성공하면 메시지도 전송되어야 함
 *    - DB 저장이 실패하면 메시지도 전송되지 않아야 함
 *
 * 2. 메시지 처리와 DB 저장의 원자성
 *    - 메시지 처리가 성공하면 ACK를 보내야 함
 *    - 메시지 처리가 실패하면 NACK를 보내 재처리하도록 해야 함
 */
@Configuration
public class TransactionQueueConfig {

    @Value("${app.rabbitmq.transaction.queue-name}")
    private String queueName;

    @Value("${app.rabbitmq.transaction.exchange-name}")
    private String exchangeName;

    @Value("${app.rabbitmq.transaction.routing-key}")
    private String routingKey;

    /**
     * Transaction Queue 빈 생성
     */
    @Bean
    public Queue transactionQueue() {
        return new Queue(queueName, true);
    }

    /**
     * Transaction Exchange 빈 생성
     */
    @Bean
    public DirectExchange transactionExchange() {
        return new DirectExchange(exchangeName);
    }

    /**
     * Transaction Binding 빈 생성
     */
    @Bean
    public Binding transactionBinding(Queue transactionQueue, DirectExchange transactionExchange) {
        return BindingBuilder.bind(transactionQueue)
                .to(transactionExchange)
                .with(routingKey);
    }
}
