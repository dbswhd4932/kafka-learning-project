package com.example.kafka.common;

import com.example.kafka.enums.MessageCategory;
import com.example.kafka.properties.KafkaTopicProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer 범용 클러스터
 * - 모든 도메인에서 재사용 가능한 범용 Producer
 * - MessageBuilder 패턴을 사용한 메시지 전송
 * - Enum 기반 토픽 관리로 타입 안전성 확보
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducerCluster {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    /**
     * 메시지 전송 (비동기)
     * - MessageCategory Enum으로 토픽 지정
     *
     * @param data     전송할 데이터
     * @param category 메시지 카테고리
     */
    public void sendMessage(Object data, MessageCategory category) {
        String topicName = topicProperties.getName(category);
        sendMessage(data, topicName);
    }

    /**
     * 메시지 전송 (비동기) - 토픽명 직접 지정
     *
     * @param data  전송할 데이터
     * @param topic 토픽명
     */
    public void sendMessage(Object data, String topic) {
        try {
            Message<Object> message = MessageBuilder
                    .withPayload(data)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .build();

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully sent message - topic: {}, partition: {}, offset: {}, data: {}",
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            result.getProducerRecord().value().toString());
                } else {
                    log.error("Failed to send message - topic: {}, error: {}", topic, ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Exception occurred while sending message - topic: {}", topic, e);
            throw e;
        }
    }

    /**
     * 메시지 전송 (Key 포함)
     *
     * @param key      메시지 Key
     * @param data     전송할 데이터
     * @param category 메시지 카테고리
     */
    public void sendMessage(String key, Object data, MessageCategory category) {
        String topicName = topicProperties.getName(category);
        sendMessage(key, data, topicName);
    }

    /**
     * 메시지 전송 (Key 포함) - 토픽명 직접 지정
     *
     * @param key   메시지 Key
     * @param data  전송할 데이터
     * @param topic 토픽명
     */
    public void sendMessage(String key, Object data, String topic) {
        try {
            Message<Object> message = MessageBuilder
                    .withPayload(data)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(KafkaHeaders.KEY, key)
                    .build();

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully sent message with key - topic: {}, key: {}, partition: {}, offset: {}",
                            topic,
                            key,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send message with key - topic: {}, key: {}, error: {}",
                            topic, key, ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Exception occurred while sending message with key - topic: {}, key: {}", topic, key, e);
            throw e;
        }
    }

    /**
     * 메시지 전송 (동기)
     * - 전송 완료까지 대기
     *
     * @param data     전송할 데이터
     * @param category 메시지 카테고리
     */
    public void sendMessageSync(Object data, MessageCategory category) {
        String topicName = topicProperties.getName(category);
        sendMessageSync(data, topicName);
    }

    /**
     * 메시지 전송 (동기) - 토픽명 직접 지정
     *
     * @param data  전송할 데이터
     * @param topic 토픽명
     */
    public void sendMessageSync(Object data, String topic) {
        try {
            Message<Object> message = MessageBuilder
                    .withPayload(data)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .build();

            SendResult<String, Object> result = kafkaTemplate.send(message).get();

            log.info("Successfully sent message (sync) - topic: {}, partition: {}, offset: {}",
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (Exception e) {
            log.error("Failed to send message (sync) - topic: {}", topic, e);
            throw new RuntimeException("Kafka 메시지 전송 실패 (동기)", e);
        }
    }

    /**
     * 특정 파티션으로 메시지 전송
     *
     * @param data      전송할 데이터
     * @param category  메시지 카테고리
     * @param partition 파티션 번호
     */
    public void sendMessageToPartition(Object data, MessageCategory category, int partition) {
        String topicName = topicProperties.getName(category);
        sendMessageToPartition(data, topicName, partition);
    }

    /**
     * 특정 파티션으로 메시지 전송 - 토픽명 직접 지정
     *
     * @param data      전송할 데이터
     * @param topic     토픽명
     * @param partition 파티션 번호
     */
    public void sendMessageToPartition(Object data, String topic, int partition) {
        try {
            Message<Object> message = MessageBuilder
                    .withPayload(data)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(KafkaHeaders.PARTITION, partition)
                    .build();

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully sent message to partition - topic: {}, partition: {}, offset: {}",
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send message to partition - topic: {}, partition: {}, error: {}",
                            topic, partition, ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Exception occurred while sending message to partition - topic: {}, partition: {}",
                    topic, partition, e);
            throw e;
        }
    }
}
