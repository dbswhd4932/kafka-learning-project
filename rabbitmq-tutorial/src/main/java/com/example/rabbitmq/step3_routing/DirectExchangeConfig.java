package com.example.rabbitmq.step3_routing;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Step 3-1: Direct Exchange 설정
 *
 * Direct Exchange는 Routing Key가 정확히 일치하는 Queue로 메시지를 라우팅합니다.
 *
 * 특징:
 * - Routing Key가 정확히 일치해야만 메시지가 전달됨
 * - 1:1 매칭 방식
 * - 가장 간단하고 성능이 좋음
 * - 기본 Exchange 타입
 *
 * 사용 사례:
 * - 특정 작업을 특정 워커에게 할당
 * - 우선순위별 작업 큐 (high, medium, low)
 * - 사용자별 전용 큐
 *
 * 예시:
 * - Routing Key "error"로 전송 → "error" 큐로만 전달
 * - Routing Key "info"로 전송 → "info" 큐로만 전달
 */
@Configuration
public class DirectExchangeConfig {

    public static final String DIRECT_EXCHANGE = "direct.exchange";
    public static final String DIRECT_QUEUE_HIGH = "direct.high.queue";
    public static final String DIRECT_QUEUE_MEDIUM = "direct.medium.queue";
    public static final String DIRECT_QUEUE_LOW = "direct.low.queue";

    /**
     * Direct Exchange 생성
     */
    @Bean
    public DirectExchange routingDirectExchange() {
        return new DirectExchange(DIRECT_EXCHANGE);
    }

    /**
     * High Priority Queue 생성
     */
    @Bean
    public Queue directHighQueue() {
        return new Queue(DIRECT_QUEUE_HIGH, true);
    }

    /**
     * Medium Priority Queue 생성
     */
    @Bean
    public Queue directMediumQueue() {
        return new Queue(DIRECT_QUEUE_MEDIUM, true);
    }

    /**
     * Low Priority Queue 생성
     */
    @Bean
    public Queue directLowQueue() {
        return new Queue(DIRECT_QUEUE_LOW, true);
    }

    /**
     * High Priority Binding
     * Routing Key "high"로 전송된 메시지는 High Queue로 전달
     */
    @Bean
    public Binding directHighBinding(Queue directHighQueue, DirectExchange routingDirectExchange) {
        return BindingBuilder.bind(directHighQueue)
                .to(routingDirectExchange)
                .with("high");
    }

    /**
     * Medium Priority Binding
     * Routing Key "medium"으로 전송된 메시지는 Medium Queue로 전달
     */
    @Bean
    public Binding directMediumBinding(Queue directMediumQueue, DirectExchange routingDirectExchange) {
        return BindingBuilder.bind(directMediumQueue)
                .to(routingDirectExchange)
                .with("medium");
    }

    /**
     * Low Priority Binding
     * Routing Key "low"로 전송된 메시지는 Low Queue로 전달
     */
    @Bean
    public Binding directLowBinding(Queue directLowQueue, DirectExchange routingDirectExchange) {
        return BindingBuilder.bind(directLowQueue)
                .to(routingDirectExchange)
                .with("low");
    }
}
