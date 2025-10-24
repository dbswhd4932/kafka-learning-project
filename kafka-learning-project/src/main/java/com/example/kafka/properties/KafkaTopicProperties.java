package com.example.kafka.properties;

import com.example.kafka.enums.MessageCategory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka Topic 설정 Properties
 * - application.yml의 kafka.topics 설정을 바인딩
 * - 메시지 카테고리별 토픽명 중앙 관리
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaTopicProperties {

    /**
     * 토픽 목록
     */
    private List<KafkaTopic> topics;

    /**
     * 메시지 카테고리로 토픽명 조회
     *
     * @param category 메시지 카테고리
     * @return 실제 토픽명
     * @throws RuntimeException 토픽을 찾을 수 없는 경우
     */
    public String getName(MessageCategory category) {
        return topics.stream()
                .filter(topic -> topic.getCategory().equals(category))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        String.format("Topic not found for category: %s", category)))
                .getName();
    }

    /**
     * 메시지 카테고리로 KafkaTopic 객체 조회
     *
     * @param category 메시지 카테고리
     * @return KafkaTopic 객체
     */
    public KafkaTopic getTopic(MessageCategory category) {
        return topics.stream()
                .filter(topic -> topic.getCategory().equals(category))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        String.format("Topic not found for category: %s", category)));
    }

    /**
     * 토픽 존재 여부 확인
     *
     * @param category 메시지 카테고리
     * @return 존재 여부
     */
    public boolean exists(MessageCategory category) {
        return topics.stream()
                .anyMatch(topic -> topic.getCategory().equals(category));
    }
}
