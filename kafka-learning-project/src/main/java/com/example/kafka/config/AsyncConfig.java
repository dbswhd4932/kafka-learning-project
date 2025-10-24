package com.example.kafka.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정
 * - @Async 어노테이션 활성화
 * - Event Listener의 비동기 처리를 위해 필요
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
