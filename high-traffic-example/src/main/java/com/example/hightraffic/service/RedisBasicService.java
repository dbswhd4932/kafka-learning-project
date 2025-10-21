package com.example.hightraffic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis 기초 학습용 서비스
 *
 * Redis의 5가지 주요 데이터 타입을 학습합니다:
 * 1. String - 가장 기본적인 key-value
 * 2. List - 순서가 있는 문자열 리스트
 * 3. Set - 중복 없는 문자열 집합
 * 4. Hash - 객체 저장에 적합 (필드-값 쌍)
 * 5. Sorted Set - 점수로 정렬된 집합
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisBasicService {

    private final RedisTemplate<String, String> redisTemplate;

    // ========================================
    // 1. String - 가장 기본적인 데이터 타입
    // ========================================

    /**
     * String 저장
     *
     * 사용 예시:
     * - 세션 정보 저장
     * - 캐싱
     * - 카운터
     *
     * Redis 명령어: SET key value
     */
    public void setString(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
        log.info("✅ String 저장 - Key: {}, Value: {}", key, value);
    }

    /**
     * String 조회
     *
     * Redis 명령어: GET key
     */
    public String getString(String key) {
        String value = redisTemplate.opsForValue().get(key);
        log.info("📖 String 조회 - Key: {}, Value: {}", key, value);
        return value;
    }

    /**
     * String TTL 설정
     *
     * TTL(Time To Live): 키가 자동으로 삭제되는 시간
     *
     * 사용 예시:
     * - 인증 코드 (5분 후 만료)
     * - 임시 토큰 (1시간 후 만료)
     *
     * Redis 명령어: SETEX key seconds value
     */
    public void setStringWithTTL(String key, String value, int seconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(seconds));
        log.info("⏰ String TTL 저장 - Key: {}, Value: {}, TTL: {}초", key, value, seconds);
    }

    /**
     * 카운터 증가 (원자적 연산)
     *
     * INCR은 원자적(atomic) 연산입니다.
     * 여러 스레드가 동시에 호출해도 안전하게 1씩 증가합니다.
     *
     * 사용 예시:
     * - 조회수
     * - 좋아요 수
     * - API 호출 횟수
     *
     * Redis 명령어: INCR key
     */
    public Long incrementCounter(String key) {
        Long newValue = redisTemplate.opsForValue().increment(key);
        log.info("➕ 카운터 증가 - Key: {}, New Value: {}", key, newValue);
        return newValue;
    }

    // ========================================
    // 2. List - 순서가 있는 리스트
    // ========================================

    /**
     * List 왼쪽에 추가 (최신 데이터가 앞에)
     *
     * 사용 예시:
     * - 최근 조회 기록
     * - 알림 목록
     * - 채팅 메시지
     *
     * Redis 명령어: LPUSH key value
     */
    public void pushToList(String key, String value) {
        redisTemplate.opsForList().leftPush(key, value);
        log.info("📝 List 추가 - Key: {}, Value: {}", key, value);
    }

    /**
     * List 조회 (범위)
     *
     * start: 0 (첫 번째)
     * end: -1 (마지막까지)
     *
     * Redis 명령어: LRANGE key start end
     */
    public List<String> getList(String key) {
        List<String> list = redisTemplate.opsForList().range(key, 0, -1);
        log.info("📋 List 조회 - Key: {}, Size: {}, Data: {}", key, list != null ? list.size() : 0, list);
        return list;
    }

    /**
     * List 크기 조회
     *
     * Redis 명령어: LLEN key
     */
    public Long getListSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        log.info("📏 List 크기 - Key: {}, Size: {}", key, size);
        return size;
    }

    // ========================================
    // 3. Set - 중복 없는 집합
    // ========================================

    /**
     * Set에 추가
     *
     * 중복된 값은 자동으로 무시됩니다.
     *
     * 사용 예시:
     * - 좋아요 누른 사용자 목록
     * - 태그 목록
     * - 유니크한 방문자 카운팅
     *
     * Redis 명령어: SADD key member
     */
    public void addToSet(String key, String value) {
        redisTemplate.opsForSet().add(key, value);
        log.info("🔹 Set 추가 - Key: {}, Value: {}", key, value);
    }

    /**
     * Set 조회 (모든 멤버)
     *
     * Redis 명령어: SMEMBERS key
     */
    public Set<String> getSet(String key) {
        Set<String> members = redisTemplate.opsForSet().members(key);
        log.info("🔸 Set 조회 - Key: {}, Size: {}, Data: {}", key, members != null ? members.size() : 0, members);
        return members;
    }

    /**
     * Set에 값이 포함되어 있는지 확인
     *
     * Redis 명령어: SISMEMBER key member
     */
    public Boolean isSetMember(String key, String value) {
        Boolean isMember = redisTemplate.opsForSet().isMember(key, value);
        log.info("❓ Set 멤버 확인 - Key: {}, Value: {}, Result: {}", key, value, isMember);
        return isMember;
    }

    // ========================================
    // 4. Hash - 객체 저장
    // ========================================

    /**
     * Hash 필드 저장
     *
     * Hash는 객체를 저장하기에 적합합니다.
     * Key 안에 여러 개의 field-value 쌍을 저장할 수 있습니다.
     *
     * 사용 예시:
     * - 사용자 정보 (user:1 → name: "홍길동", age: "30")
     * - 상품 정보 (product:100 → name: "노트북", price: "1000000")
     *
     * Redis 명령어: HSET key field value
     */
    public void setHash(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
        log.info("🗂️ Hash 저장 - Key: {}, Field: {}, Value: {}", key, field, value);
    }

    /**
     * Hash 필드 조회
     *
     * Redis 명령어: HGET key field
     */
    public String getHashField(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        log.info("📂 Hash 필드 조회 - Key: {}, Field: {}, Value: {}", key, field, value);
        return value != null ? value.toString() : null;
    }

    /**
     * Hash 전체 조회
     *
     * Redis 명령어: HGETALL key
     */
    public Map<Object, Object> getHash(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        log.info("📁 Hash 전체 조회 - Key: {}, Size: {}, Data: {}", key, entries.size(), entries);
        return entries;
    }

    // ========================================
    // 5. Sorted Set - 점수로 정렬된 집합
    // ========================================

    /**
     * Sorted Set에 추가 (점수 포함)
     *
     * 점수(score)에 따라 자동으로 정렬됩니다.
     *
     * 사용 예시:
     * - 랭킹 시스템 (게임 점수, 조회수 순위)
     * - 우선순위 큐
     * - 시간순 정렬 (timestamp를 score로 사용)
     *
     * Redis 명령어: ZADD key score member
     */
    public void addToSortedSet(String key, String value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
        log.info("🏆 Sorted Set 추가 - Key: {}, Value: {}, Score: {}", key, value, score);
    }

    /**
     * Sorted Set 조회 (점수 높은 순)
     *
     * Redis 명령어: ZREVRANGE key start end WITHSCORES
     */
    public Set<String> getSortedSetDesc(String key, int count) {
        // 점수가 높은 순서대로 조회 (내림차순)
        Set<String> members = redisTemplate.opsForZSet().reverseRange(key, 0, count - 1);
        log.info("🥇 Sorted Set 조회 (내림차순) - Key: {}, Count: {}, Data: {}", key, count, members);
        return members;
    }

    /**
     * Sorted Set 조회 (점수 낮은 순)
     *
     * Redis 명령어: ZRANGE key start end
     */
    public Set<String> getSortedSetAsc(String key, int count) {
        // 점수가 낮은 순서대로 조회 (오름차순)
        Set<String> members = redisTemplate.opsForZSet().range(key, 0, count - 1);
        log.info("🥉 Sorted Set 조회 (오름차순) - Key: {}, Count: {}, Data: {}", key, count, members);
        return members;
    }

    /**
     * 특정 값의 순위 조회
     *
     * Redis 명령어: ZREVRANK key member (점수 높은 순 기준)
     */
    public Long getRank(String key, String value) {
        Long rank = redisTemplate.opsForZSet().reverseRank(key, value);
        log.info("🎖️ Sorted Set 순위 - Key: {}, Value: {}, Rank: {}", key, value, rank != null ? rank + 1 : null);
        return rank != null ? rank + 1 : null;  // 0-based → 1-based
    }

    // ========================================
    // 공통 - 키 관리
    // ========================================

    /**
     * 키 삭제
     *
     * Redis 명령어: DEL key
     */
    public void deleteKey(String key) {
        redisTemplate.delete(key);
        log.info("🗑️ 키 삭제 - Key: {}", key);
    }

    /**
     * 키 존재 여부 확인
     *
     * Redis 명령어: EXISTS key
     */
    public Boolean hasKey(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        log.info("🔍 키 존재 확인 - Key: {}, Exists: {}", key, exists);
        return exists;
    }

    /**
     * TTL 조회 (남은 시간)
     *
     * Redis 명령어: TTL key
     */
    public Long getTTL(String key) {
        Long ttl = redisTemplate.getExpire(key);
        log.info("⏱️ TTL 조회 - Key: {}, TTL: {}초", key, ttl);
        return ttl;
    }
}
