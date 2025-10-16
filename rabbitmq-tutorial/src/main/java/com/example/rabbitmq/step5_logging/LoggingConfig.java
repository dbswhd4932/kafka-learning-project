package com.example.rabbitmq.step5_logging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Step 5: Routing Model을 활용한 Log 수집 시스템
 *
 * Topic Exchange를 사용하여 로그 레벨별로 다른 Queue로 라우팅합니다.
 *
 * Routing Key 패턴:
 * - {serviceName}.error   → ERROR Queue
 * - {serviceName}.warn    → WARN Queue (+ ALL Queue)
 * - {serviceName}.info    → INFO Queue (+ ALL Queue)
 * - *.error               → ERROR Queue (모든 서비스의 에러)
 * - #                     → ALL Queue (모든 로그)
 *
 * 사용 사례:
 * - 에러 로그만 별도 모니터링
 * - 서비스별 로그 분리
 * - 로그 레벨별 저장소 분리
 * - 실시간 로그 분석
 */
@Configuration
public class LoggingConfig {

    @Value("${app.rabbitmq.logging.exchange-name}")
    private String exchangeName;

    @Value("${app.rabbitmq.logging.error-queue}")
    private String errorQueueName;

    @Value("${app.rabbitmq.logging.warn-queue}")
    private String warnQueueName;

    @Value("${app.rabbitmq.logging.info-queue}")
    private String infoQueueName;

    @Value("${app.rabbitmq.logging.all-queue}")
    private String allQueueName;

    /**
     * Logging Topic Exchange 생성
     */
    @Bean
    public TopicExchange loggingExchange() {
        return new TopicExchange(exchangeName);
    }

    /**
     * Error Log Queue 생성
     */
    @Bean
    public Queue errorLogQueue() {
        return new Queue(errorQueueName, true);
    }

    /**
     * Warn Log Queue 생성
     */
    @Bean
    public Queue warnLogQueue() {
        return new Queue(warnQueueName, true);
    }

    /**
     * Info Log Queue 생성
     */
    @Bean
    public Queue infoLogQueue() {
        return new Queue(infoQueueName, true);
    }

    /**
     * All Log Queue 생성 (모든 로그 수집)
     */
    @Bean
    public Queue allLogQueue() {
        return new Queue(allQueueName, true);
    }

    /**
     * Error Log Binding
     * 패턴: *.error (모든 서비스의 에러 로그)
     */
    @Bean
    public Binding errorLogBinding(Queue errorLogQueue, TopicExchange loggingExchange) {
        return BindingBuilder.bind(errorLogQueue)
                .to(loggingExchange)
                .with("*.error");
    }

    /**
     * Warn Log Binding
     * 패턴: *.warn (모든 서비스의 경고 로그)
     */
    @Bean
    public Binding warnLogBinding(Queue warnLogQueue, TopicExchange loggingExchange) {
        return BindingBuilder.bind(warnLogQueue)
                .to(loggingExchange)
                .with("*.warn");
    }

    /**
     * Info Log Binding
     * 패턴: *.info (모든 서비스의 정보 로그)
     */
    @Bean
    public Binding infoLogBinding(Queue infoLogQueue, TopicExchange loggingExchange) {
        return BindingBuilder.bind(infoLogQueue)
                .to(loggingExchange)
                .with("*.info");
    }

    /**
     * All Log Binding
     * 패턴: # (모든 로그)
     */
    @Bean
    public Binding allLogBinding(Queue allLogQueue, TopicExchange loggingExchange) {
        return BindingBuilder.bind(allLogQueue)
                .to(loggingExchange)
                .with("#");
    }
}
