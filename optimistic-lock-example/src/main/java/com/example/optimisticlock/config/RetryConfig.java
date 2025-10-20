package com.example.optimisticlock.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 설정 클래스
 *
 * @EnableRetry: @Retryable 어노테이션을 사용하기 위한 설정
 * - 메서드에 @Retryable을 붙이면 예외 발생 시 자동으로 재시도
 * - AOP 기반으로 동작하므로 spring-aspects 의존성이 필요
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // @EnableRetry 어노테이션만으로 기본 설정이 활성화됩니다.
    // 추가 커스터마이징이 필요한 경우 여기에 Bean을 정의할 수 있습니다.
}
