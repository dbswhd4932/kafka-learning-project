package com.example.kafka.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kafka SSL/Security 설정 Properties
 * - 프로덕션 환경에서 보안 연결을 위한 설정
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kafka.ssl")
public class KafkaSSLProperties {

    /**
     * SSL 활성화 여부
     */
    private boolean enabled = false;

    /**
     * 보안 프로토콜
     * - PLAINTEXT: 보안 없음
     * - SSL: SSL/TLS 암호화
     * - SASL_PLAINTEXT: SASL 인증
     * - SASL_SSL: SASL 인증 + SSL 암호화
     */
    private String securityProtocol = "PLAINTEXT";

    /**
     * SASL 메커니즘
     * - PLAIN
     * - SCRAM-SHA-256
     * - SCRAM-SHA-512
     * - GSSAPI (Kerberos)
     */
    private String saslMechanism;

    /**
     * SASL JAAS 설정
     * 예: org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="admin-secret";
     */
    private String saslJaasConfig;
}
