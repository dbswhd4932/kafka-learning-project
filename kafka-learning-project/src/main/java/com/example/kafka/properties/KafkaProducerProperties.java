package com.example.kafka.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kafka Producer 설정 Properties
 * - application.yml의 kafka.producer 설정을 바인딩
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kafka.producer")
public class KafkaProducerProperties {

    /**
     * Kafka 브로커 주소
     * 예: localhost:9092
     */
    private String bootstrapServers;

    /**
     * ACK 설정
     * - 0: 확인 안 함
     * - 1: Leader 확인
     * - all: 모든 Replica 확인
     */
    private String acks = "all";

    /**
     * 재시도 횟수
     */
    private Integer retries = 3;

    /**
     * 배치 크기 (bytes)
     */
    private Integer batchSize = 16384;

    /**
     * 배치 대기 시간 (ms)
     */
    private Integer lingerMs = 1;

    /**
     * 버퍼 메모리 (bytes)
     */
    private Long bufferMemory = 33554432L;

    /**
     * 압축 타입
     * - none, gzip, snappy, lz4, zstd
     */
    private String compressionType = "none";

    /**
     * 요청 타임아웃 (ms)
     */
    private Integer requestTimeoutMs = 30000;

    /**
     * 최대 블록 시간 (ms)
     */
    private Long maxBlockMs = 60000L;
}
