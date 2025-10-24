package com.example.kafka.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer 설정 Properties
 * - application.yml의 kafka.consumer 설정을 바인딩
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kafka.consumer")
public class KafkaConsumerProperties {

    /**
     * Kafka 브로커 주소
     */
    private String bootstrapServers;

    /**
     * Consumer Group ID
     */
    private String groupId;

    /**
     * Auto Offset Reset
     * - earliest: 처음부터
     * - latest: 최신부터
     * - none: Offset 없으면 예외
     */
    private String autoOffsetReset = "earliest";

    /**
     * 자동 커밋 활성화
     */
    private Boolean enableAutoCommit = true;

    /**
     * 자동 커밋 간격 (ms)
     */
    private Integer autoCommitIntervalMs = 1000;

    /**
     * 한 번에 가져올 최대 레코드 수
     */
    private Integer maxPollRecords = 500;

    /**
     * Poll 간격 (ms)
     */
    private Integer maxPollIntervalMs = 300000;

    /**
     * 세션 타임아웃 (ms)
     */
    private Integer sessionTimeoutMs = 10000;

    /**
     * 하트비트 간격 (ms)
     */
    private Integer heartbeatIntervalMs = 3000;

    /**
     * 동시성 (Consumer 스레드 수)
     */
    private Integer concurrency = 3;
}
