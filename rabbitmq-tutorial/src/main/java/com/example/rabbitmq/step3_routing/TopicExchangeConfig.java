package com.example.rabbitmq.step3_routing;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Step 3-2: Topic Exchange 설정
 *
 * Topic Exchange는 Routing Key 패턴 매칭을 통해 메시지를 라우팅합니다.
 *
 * 특징:
 * - Routing Key 패턴 매칭 지원
 * - 와일드카드 사용 가능
 *   * (asterisk): 정확히 한 단어 매칭
 *   # (hash): 0개 이상의 단어 매칭
 * - 유연한 라우팅 규칙
 *
 * 사용 사례:
 * - 로그 수집 시스템 (*.error, *.warn, *.info)
 * - 위치 기반 알림 (korea.seoul.*, korea.#)
 * - 다양한 조건의 메시지 필터링
 *
 * Routing Key 예시:
 * - "order.created" → order.* 패턴에 매칭
 * - "order.payment.completed" → order.# 패턴에 매칭
 * - "user.registered" → user.* 패턴에 매칭
 *
 * 패턴 매칭 규칙:
 * - Routing Key는 점(.)으로 구분된 단어들로 구성
 * - * : 정확히 한 단어 (예: "order.*" → "order.created" OK, "order.payment.completed" NO)
 * - # : 0개 이상의 단어 (예: "order.#" → "order.created", "order.payment.completed" 모두 OK)
 */
@Configuration
public class TopicExchangeConfig {

    public static final String TOPIC_EXCHANGE = "topic.exchange";
    public static final String TOPIC_QUEUE_ORDER = "topic.order.queue";
    public static final String TOPIC_QUEUE_PAYMENT = "topic.payment.queue";
    public static final String TOPIC_QUEUE_ALL = "topic.all.queue";

    /**
     * Topic Exchange 생성
     */
    @Bean
    public TopicExchange routingTopicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE);
    }

    /**
     * Order Queue 생성
     * order.* 패턴의 메시지를 받음
     */
    @Bean
    public Queue topicOrderQueue() {
        return new Queue(TOPIC_QUEUE_ORDER, true);
    }

    /**
     * Payment Queue 생성
     * *.payment.* 패턴의 메시지를 받음
     */
    @Bean
    public Queue topicPaymentQueue() {
        return new Queue(TOPIC_QUEUE_PAYMENT, true);
    }

    /**
     * All Queue 생성
     * # 패턴으로 모든 메시지를 받음
     */
    @Bean
    public Queue topicAllQueue() {
        return new Queue(TOPIC_QUEUE_ALL, true);
    }

    /**
     * Order Binding
     * "order.*" 패턴: order.created, order.updated 등
     */
    @Bean
    public Binding topicOrderBinding(Queue topicOrderQueue, TopicExchange routingTopicExchange) {
        return BindingBuilder.bind(topicOrderQueue)
                .to(routingTopicExchange)
                .with("order.*");
    }

    /**
     * Payment Binding
     * "*.payment.*" 패턴: order.payment.completed, user.payment.failed 등
     */
    @Bean
    public Binding topicPaymentBinding(Queue topicPaymentQueue, TopicExchange routingTopicExchange) {
        return BindingBuilder.bind(topicPaymentQueue)
                .to(routingTopicExchange)
                .with("*.payment.*");
    }

    /**
     * All Binding
     * "#" 패턴: 모든 메시지를 받음
     */
    @Bean
    public Binding topicAllBinding(Queue topicAllQueue, TopicExchange routingTopicExchange) {
        return BindingBuilder.bind(topicAllQueue)
                .to(routingTopicExchange)
                .with("#");
    }
}
