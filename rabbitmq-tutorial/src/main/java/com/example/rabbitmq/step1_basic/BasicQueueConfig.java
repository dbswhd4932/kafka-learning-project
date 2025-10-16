package com.example.rabbitmq.step1_basic;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Step 1: 기본 메시지 큐 설정
 *
 * RabbitMQ의 핵심 구성요소인 Queue, Exchange, Binding을 설정합니다.
 *
 * AMQP 메시징 모델:
 * Producer -> Exchange -> Binding -> Queue -> Consumer
 *
 * - Queue: 메시지가 저장되는 버퍼
 * - Exchange: 메시지를 적절한 Queue로 라우팅하는 라우터
 * - Binding: Exchange와 Queue를 연결하고 라우팅 규칙을 정의
 * - Routing Key: Exchange가 메시지를 라우팅할 때 사용하는 키
 */
@Configuration
public class BasicQueueConfig {

    @Value("${app.rabbitmq.basic.queue-name}")
    private String queueName;

    @Value("${app.rabbitmq.basic.exchange-name}")
    private String exchangeName;

    @Value("${app.rabbitmq.basic.routing-key}")
    private String routingKey;

    /**
     * Queue 빈 생성
     *
     * durable = true: 서버 재시작 시에도 Queue가 유지됩니다.
     * durable = false: 서버 재시작 시 Queue가 삭제됩니다.
     *
     * @return Queue
     */
    @Bean
    public Queue basicQueue() {
        return new Queue(queueName, true);
    }

    /**
     * Direct Exchange 빈 생성
     *
     * Direct Exchange는 Routing Key가 정확히 일치하는 Queue로 메시지를 전달합니다.
     * 가장 간단하고 많이 사용되는 Exchange 타입입니다.
     *
     * @return DirectExchange
     */
    @Bean
    public DirectExchange basicExchange() {
        return new DirectExchange(exchangeName);
    }

    /**
     * Binding 빈 생성
     *
     * Exchange와 Queue를 Routing Key로 연결합니다.
     * Producer가 이 Routing Key로 메시지를 전송하면 해당 Queue로 전달됩니다.
     *
     * @return Binding
     */
    @Bean
    public Binding basicBinding(Queue basicQueue, DirectExchange basicExchange) {
        return BindingBuilder.bind(basicQueue)
                .to(basicExchange)
                .with(routingKey);
    }
}
