package com.example.kafka.config;

import com.example.kafka.properties.KafkaProducerProperties;
import com.example.kafka.properties.KafkaSSLProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Kafka Producer 설정
 * - ConfigurationProperties를 사용한 타입 안전한 설정
 * - 범용 KafkaTemplate (Object 타입 지원)
 * - SSL/SASL 보안 설정 지원
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {

    private final KafkaProducerProperties producerProperties;
    private final KafkaSSLProperties sslProperties;

    /**
     * ObjectMapper 설정
     * - LocalDateTime을 ISO-8601 형식으로 직렬화
     */
    @Bean
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // JavaTimeModule 등록 (LocalDateTime 등 Java 8 Time API 지원)
        mapper.registerModule(new JavaTimeModule());
        // LocalDateTime을 배열이 아닌 ISO-8601 문자열로 직렬화
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Producer Factory 설정
     * - Key: String
     * - Value: Object (모든 타입 지원)
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        Properties props = buildProducerProperties();

        props.forEach((key, value) -> configProps.put(key.toString(), value));

        // JsonSerializer에 ObjectMapper 설정
        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(kafkaObjectMapper());
        jsonSerializer.setAddTypeInfo(false);  // Type 헤더 비활성화

        return new DefaultKafkaProducerFactory<>(
                configProps,
                new StringSerializer(),
                jsonSerializer
        );
    }

    /**
     * KafkaTemplate
     * - 범용 Producer 템플릿 (모든 타입의 메시지 전송 가능)
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Producer Properties 빌드
     * - Map 대신 Properties 객체 사용
     */
    private Properties buildProducerProperties() {
        Properties props = new Properties();

        // 기본 설정
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerProperties.getBootstrapServers());
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());

        // Producer 성능 및 신뢰성 설정
        props.setProperty(ProducerConfig.ACKS_CONFIG, producerProperties.getAcks());
        props.setProperty(ProducerConfig.RETRIES_CONFIG, String.valueOf(producerProperties.getRetries()));
        props.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, String.valueOf(producerProperties.getBatchSize()));
        props.setProperty(ProducerConfig.LINGER_MS_CONFIG, String.valueOf(producerProperties.getLingerMs()));
        props.setProperty(ProducerConfig.BUFFER_MEMORY_CONFIG, String.valueOf(producerProperties.getBufferMemory()));
        props.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, producerProperties.getCompressionType());
        props.setProperty(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(producerProperties.getRequestTimeoutMs()));
        props.setProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG, String.valueOf(producerProperties.getMaxBlockMs()));

        // SSL/SASL 보안 설정
        if (sslProperties.isEnabled()) {
            log.info("Kafka SSL/SASL security is enabled");
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
