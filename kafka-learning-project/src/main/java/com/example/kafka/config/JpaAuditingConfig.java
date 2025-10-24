package com.example.kafka.config;

import com.example.kafka.security.AccessUserManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA Auditing 설정
 * - @CreatedBy, @LastModifiedBy 자동 주입
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * AuditorAware 구현
     * - 현재 사용자 ID 반환
     */
    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> Optional.of(AccessUserManager.getAccessUser().getUserId());
    }
}
