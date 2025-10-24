package com.example.kafka.config;

import com.example.kafka.properties.KafkaProducerProperties;
import com.example.kafka.properties.KafkaTopic;
import com.example.kafka.properties.KafkaTopicProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Kafka Topic 설정
 * - application.yml의 토픽 설정을 기반으로 자동 생성
 * - 동적으로 모든 토픽 생성
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {

    private final KafkaProducerProperties producerProperties;
    private final KafkaTopicProperties topicProperties;

    /**
     * Kafka Admin 설정
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Properties props = new Properties();
        props.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, producerProperties.getBootstrapServers());

        return new KafkaAdmin(props.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue()
                )));
    }

    /**
     * application.yml의 토픽 설정을 읽어서 자동으로 토픽 생성
     * - 토픽명, 파티션 수, 복제 팩터를 YAML에서 관리
     */
    @Bean
    public KafkaAdmin.NewTopics createTopics() {
        List<NewTopic> topics = topicProperties.getTopics().stream()
                .map(this::createNewTopic)
                .collect(Collectors.toList());

        log.info("Creating {} topics from configuration", topics.size());
        topics.forEach(topic ->
                log.info("Topic: {} | Partitions: {} | Replication Factor: {}",
                        topic.name(),
                        topic.numPartitions(),
                        topic.replicationFactor())
        );

        return new KafkaAdmin.NewTopics(topics.toArray(new NewTopic[0]));
    }

    /**
     * KafkaTopic 설정을 NewTopic으로 변환
     */
    private NewTopic createNewTopic(KafkaTopic kafkaTopic) {
        return new NewTopic(
                kafkaTopic.getName(),
                kafkaTopic.getPartitions(),
                kafkaTopic.getReplicationFactor()
        );
    }
}
