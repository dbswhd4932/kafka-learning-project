package com.example.redis.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 설정 클래스
 *
 * - RedisTemplate: Redis 직접 조작용
 * - CacheManager: @Cacheable 등 Spring Cache 어노테이션용
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * RedisTemplate 빈 생성
     * Redis 데이터를 직접 저장/조회할 때 사용
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // ObjectMapper 설정: Java 객체 <-> JSON 변환
        ObjectMapper objectMapper = new ObjectMapper();

        // Java 8 날짜/시간 API 지원 (LocalDateTime 등)
        objectMapper.registerModule(new JavaTimeModule());

        // 날짜를 "2025-10-21T10:20:30" 형식으로 저장 (타임스탬프 대신)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        /**
         * activateDefaultTyping: 타입 정보를 JSON에 포함
         *
         * 왜 필요한가?
         * - Redis에서 데이터를 꺼낼 때 원래 타입으로 복원하기 위해
         * - 타입 정보 없이는 List<ProductResponse>를 Object로만 인식
         *
         * 저장 형태:
         * - 타입 정보 포함: ["java.util.ArrayList", [{"@class":"...ProductResponse", ...}]]
         * - 타입 정보 없음: [{"id":1, ...}]  ← 복원 시 타입 모름!
         *
         * 파라미터:
         * - getPolymorphicTypeValidator(): 보안을 위한 타입 검증
         * - NON_FINAL: final이 아닌 클래스에 타입 정보 포함
         * - PROPERTY: @class 속성으로 타입 정보 저장
         */
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        // Key Serializer: Redis Key를 String으로 저장
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value Serializer: Redis Value를 JSON으로 저장
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager 빈 생성
     * @Cacheable, @CachePut, @CacheEvict 어노테이션 사용 시 적용
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // ObjectMapper 설정 (RedisTemplate과 동일)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 타입 정보 포함 (위와 동일한 이유)
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        // Redis Cache 설정
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // TTL: 10분 (캐시 데이터 유효 시간)
                .entryTtl(Duration.ofMinutes(10))
                // Key는 String으로 직렬화
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                // Value는 JSON으로 직렬화
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(objectMapper)
                        )
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

}
