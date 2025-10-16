package com.example.rabbitmq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 기본 설정 클래스
 *
 * 이 클래스는 RabbitMQ와 Spring AMQP를 사용하기 위한 기본 설정을 제공합니다.
 *
 * 주요 설정:
 * - MessageConverter: JSON 형식으로 메시지를 직렬화/역직렬화
 * - RabbitTemplate: 메시지 전송을 위한 템플릿 클래스
 * - ObjectMapper: Java 객체와 JSON 간의 변환을 담당
 */
@Configuration
public class RabbitMqConfig {

    /**
     * Jackson ObjectMapper 설정
     * Java 8 날짜/시간 API를 지원하도록 설정합니다.
     *
     * @return 설정된 ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Java 8 날짜/시간 모듈 등록
        objectMapper.registerModule(new JavaTimeModule());
        // 날짜를 timestamp가 아닌 ISO-8601 형식으로 직렬화
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    /**
     * MessageConverter 빈 설정
     * RabbitMQ 메시지를 JSON 형식으로 변환하기 위한 컨버터입니다.
     *
     * Jackson2JsonMessageConverter를 사용하여:
     * - Java 객체를 JSON으로 직렬화하여 메시지로 전송
     * - 수신한 JSON 메시지를 Java 객체로 역직렬화
     *
     * @return Jackson2JsonMessageConverter
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter(objectMapper());
    }

    /**
     * RabbitTemplate 빈 설정
     * 메시지 전송을 위한 핵심 클래스입니다.
     *
     * 설정 내용:
     * - MessageConverter를 사용하여 메시지를 JSON으로 변환
     * - Publisher Confirms: 메시지가 브로커에 도착했는지 확인
     * - Publisher Returns: 라우팅 실패 시 메시지를 반환받음
     *
     * @param connectionFactory RabbitMQ 연결 팩토리
     * @return 설정된 RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());

        // Publisher Confirms Callback 설정
        // 메시지가 Exchange에 도착했는지 확인
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("메시지가 Exchange에 성공적으로 전달되었습니다.");
            } else {
                System.err.println("메시지 전달 실패: " + cause);
            }
        });

        // Publisher Returns Callback 설정
        // 메시지가 Queue로 라우팅되지 못한 경우 호출
        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("메시지 라우팅 실패!");
            System.err.println("메시지: " + returned.getMessage());
            System.err.println("Reply Code: " + returned.getReplyCode());
            System.err.println("Reply Text: " + returned.getReplyText());
            System.err.println("Exchange: " + returned.getExchange());
            System.err.println("Routing Key: " + returned.getRoutingKey());
        });

        return rabbitTemplate;
    }
}
