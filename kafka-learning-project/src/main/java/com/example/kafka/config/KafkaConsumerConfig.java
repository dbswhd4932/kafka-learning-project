package com.example.kafka.config;

import com.example.kafka.domain.Order;
import com.example.kafka.properties.KafkaConsumerProperties;
import com.example.kafka.properties.KafkaSSLProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Properties;

/**
 * Kafka Consumer 설정
 * - ConfigurationProperties를 사용한 타입 안전한 설정
 * - SSL/SASL 보안 설정 지원
 */
@Slf4j
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaConsumerProperties consumerProperties;
    private final KafkaSSLProperties sslProperties;

    /**
     * Consumer Factory 설정
     * - Key: String
     * - Value: Order 객체 (JSON 역직렬화)
     */
    @Bean
    public ConsumerFactory<String, Order> consumerFactory() {
        Properties props = buildConsumerProperties();
        return new DefaultKafkaConsumerFactory<>(
                props.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                e -> e.getKey().toString(),
                                e -> e.getValue()
                        )),
                new StringDeserializer(),
                new JsonDeserializer<>(Order.class, false)
        );
    }

    /**
     * Kafka Listener Container Factory
     * - @KafkaListener 어노테이션을 사용하여 메시지를 처리하는 리스너 컨테이너
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Order> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Order> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // 동시성 설정 (파티션별로 Consumer 스레드 할당)
        factory.setConcurrency(consumerProperties.getConcurrency());

        return factory;
    }

    /**
     * Consumer Properties 빌드
     * - Map 대신 Properties 객체 사용
     */
    private Properties buildConsumerProperties() {
        Properties props = new Properties();

        // 기본 설정
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerProperties.getBootstrapServers());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, consumerProperties.getGroupId());
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());

        // JSON Deserializer 신뢰할 패키지 설정
        props.setProperty(JsonDeserializer.TRUSTED_PACKAGES, "*");

        // Consumer 동작 설정
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerProperties.getAutoOffsetReset());
        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(consumerProperties.getEnableAutoCommit()));
        props.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, String.valueOf(consumerProperties.getAutoCommitIntervalMs()));
        props.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(consumerProperties.getMaxPollRecords()));
        props.setProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, String.valueOf(consumerProperties.getMaxPollIntervalMs()));
        props.setProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, String.valueOf(consumerProperties.getSessionTimeoutMs()));
        props.setProperty(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, String.valueOf(consumerProperties.getHeartbeatIntervalMs()));

        // SSL/SASL 보안 설정
        if (sslProperties.isEnabled()) {
            log.info("Kafka SSL/SASL security is enabled for consumer");
            applySslConfig(props);
        }

        return props;
    }

    /**
     * SSL/SASL 보안 설정 적용
     */
    private void applySslConfig(Properties props) {
        props.setProperty(org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
                sslProperties.getSecurityProtocol());

        if (sslProperties.getSaslMechanism() != null) {
            props.setProperty(org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM,
                    sslProperties.getSaslMechanism());
        }
        if (sslProperties.getSaslJaasConfig() != null) {
            props.setProperty(org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG,
                    sslProperties.getSaslJaasConfig());
        }
    }
}
