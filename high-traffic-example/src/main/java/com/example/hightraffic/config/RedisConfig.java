package com.example.hightraffic.config;

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
 * 목적:
 * - Redis를 활용한 조회수 처리 (고성능 INCR 연산)
 * - Rate Limiting 구현 (어뷰징 방지)
 * - 중복 조회 방지 (TTL 기반)
 *
 * 주요 기능:
 * 1. RedisTemplate 설정 - 조회수, Rate Limit 데이터 저장
 * 2. CacheManager 설정 - Spring Cache 추상화
 *
 * Redis 활용 사례:
 * - 조회수: post:viewcount:{postId} (INCR, GET)
 * - 중복 방지: post:viewed:{postId}:{ip} (5초 TTL)
 * - Rate Limit: ratelimit:ip:{ip} (60초 TTL)
 */
@EnableCaching
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate 설정
     *
     * 목적:
     * - Redis 데이터를 Java 객체로 직렬화/역직렬화
     * - 조회수, Rate Limit 등의 데이터를 Redis에 저장
     *
     * 직렬화 전략:
     * 1. Key: StringRedisSerializer
     *    - Redis에서 Key를 문자열로 저장 (가독성 향상)
     *    - 예: "post:viewcount:1", "ratelimit:ip:127.0.0.1"
     *
     * 2. Value: GenericJackson2JsonRedisSerializer
     *    - Java 객체를 JSON 형태로 저장
     *    - LocalDateTime 등의 Java 8 날짜/시간 타입 지원
     *    - 타입 정보를 함께 저장하여 역직렬화 시 원본 타입 복원
     *
     * 성능 고려사항:
     * - INCR 연산: Redis의 원자적 연산으로 동시성 문제 해결
     * - TTL 설정: 메모리 효율적 관리 (중복 방지 5초, Rate Limit 60초)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key Serializer: 문자열로 직렬화
        // Redis CLI에서 직접 확인 가능하도록 StringRedisSerializer 사용
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value Serializer: JSON으로 직렬화
        ObjectMapper objectMapper = new ObjectMapper();
        // Java 8 날짜/시간 타입 지원 (LocalDateTime, LocalDate 등)
        objectMapper.registerModule(new JavaTimeModule());
        // 날짜를 타임스탬프가 아닌 ISO-8601 형식으로 저장
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager 설정
     *
     * 목적:
     * - Spring Cache 추상화를 통한 캐싱 지원
     * - @Cacheable, @CacheEvict 등의 어노테이션 사용 가능
     *
     * 캐시 설정:
     * 1. TTL: 1시간
     *    - 자주 변경되지 않는 데이터에 적합
     *    - 메모리 효율성과 데이터 신선도 균형
     *
     * 2. Null 값 캐싱 비활성화
     *    - null 값은 캐시하지 않음
     *    - Cache Penetration 공격 방어
     *    - 메모리 낭비 방지
     *
     * 사용 예시:
     * - @Cacheable("posts") - 게시글 조회 결과 캐싱
     * - @CacheEvict("posts") - 게시글 수정 시 캐시 무효화
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 캐시 만료 시간: 1시간
                .entryTtl(Duration.ofHours(1))
                // null 값은 캐싱하지 않음 (Cache Penetration 방어)
                .disableCachingNullValues()
                // Key: 문자열로 직렬화
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                // Value: JSON으로 직렬화
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
