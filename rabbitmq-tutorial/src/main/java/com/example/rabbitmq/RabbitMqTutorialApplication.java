package com.example.rabbitmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RabbitMQ 비동기 아키텍처 튜토리얼 애플리케이션
 *
 * 이 프로젝트는 RabbitMQ를 활용한 비동기 아키텍처를 단계별로 학습하기 위한 예제 프로젝트입니다.
 *
 * 학습 내용:
 * - Step 1: 기본 메시지 전송/수신
 * - Step 2: DB 연동과 Transaction 처리
 * - Step 3: Exchange와 Routing Model (Direct, Topic, Fanout)
 * - Step 4: Pub/Sub을 이용한 실시간 알람 시스템
 * - Step 5: Routing Model을 활용한 Log 수집
 * - Step 6: Dead Letter Queue와 Retry를 이용한 재처리
 */
@SpringBootApplication
public class RabbitMqTutorialApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitMqTutorialApplication.class, args);
    }

}
