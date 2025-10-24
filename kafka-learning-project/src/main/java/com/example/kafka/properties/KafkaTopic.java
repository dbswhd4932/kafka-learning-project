package com.example.kafka.properties;

import com.example.kafka.enums.MessageCategory;
import lombok.Getter;
import lombok.Setter;

/**
 * Kafka 토픽 설정 모델
 * - 카테고리와 실제 토픽명 매핑
 */
@Getter
@Setter
public class KafkaTopic {

    /**
     * 메시지 카테고리
     */
    private MessageCategory category;

    /**
     * 실제 Kafka 토픽명
     */
    private String name;

    /**
     * 파티션 수 (선택사항)
     */
    private Integer partitions = 3;

    /**
     * 복제 팩터 (선택사항)
     */
    private Short replicationFactor = 1;
}
