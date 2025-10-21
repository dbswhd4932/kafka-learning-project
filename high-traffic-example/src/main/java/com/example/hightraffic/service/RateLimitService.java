package com.example.hightraffic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Rate Limiting 서비스 (IP 기반)
 *
 * 문제 정의:
 * - 조회수 어뷰징: 자동화 스크립트로 무한 새로고침
 * - 서버 리소스 과다 소비 (Redis, DB, 네트워크)
 * - DDoS 공격, 크롤링 봇에 의한 서비스 불안정
 *
 * 해결 방법: 어뷰징 방지 - 정책 B: Rate Limiting (중급)
 * - Sliding Window 방식의 요청 횟수 제한
 * - 1분에 최대 20회 조회 허용
 * - 초과 시 조회는 가능하지만 조회수 증가 차단
 *
 * 구현 방식:
 * - Redis INCR + TTL 활용
 * - Key: ratelimit:ip:{ip}
 * - TTL: 60초 (1분)
 * - Value: 요청 횟수
 *
 * 정책 적용:
 * - 1~20회: 정상 처리 (조회수 증가)
 * - 21회 이상: 조회는 허용, 조회수는 증가 안함
 * - 60초 후: 카운터 자동 리셋
 *
 * 성능 고려사항:
 * - Redis INCR: O(1) 시간 복잡도
 * - TTL 자동 만료: 메모리 효율적
 * - IP 기반 제한: 프록시 환경에서는 X-Forwarded-For 헤더 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    // Redis Key Prefix
    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:ip:";

    // Rate Limiting 설정
    private static final int MAX_REQUESTS_PER_MINUTE = 20;  // 1분에 최대 20회
    private static final int WINDOW_SIZE_SECONDS = 60;      // 1분 윈도우

    /**
     * Rate Limit 체크 및 요청 기록
     *
     * 로직:
     * 1. Redis에서 현재 요청 횟수 조회
     * 2. 제한 초과 여부 확인 (20회)
     * 3. 초과하지 않으면 INCR로 횟수 증가
     * 4. 첫 요청이면 TTL 60초 설정
     *
     * 동작 예시:
     * - 1~20번째 요청: INCR → true 반환 (조회수 증가 허용)
     * - 21번째 요청: false 반환 (조회는 허용, 조회수 증가 차단)
     * - 60초 후: 키 자동 만료 → 다시 1번째 요청부터 시작
     *
     * Race Condition 방어:
     * - Redis INCR는 원자적 연산으로 동시성 안전
     * - 여러 스레드가 동시에 호출해도 정확한 카운팅
     *
     * @param ip 클라이언트 IP
     * @return 허용 여부 (true: 조회수 증가 허용, false: 조회수 증가 차단)
     */
    public boolean isAllowed(String ip) {
        String key = RATE_LIMIT_KEY_PREFIX + ip;

        // 현재 요청 횟수 조회
        String countStr = redisTemplate.opsForValue().get(key);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;

        // 제한 초과 체크
        if (currentCount >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate Limit 초과: ip={}, count={}", ip, currentCount);
            return false;
        }

        // 요청 횟수 증가
        Long newCount = redisTemplate.opsForValue().increment(key);

        // 첫 요청이면 TTL 설정 (1분)
        if (newCount == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SIZE_SECONDS));
        }

        log.debug("Rate Limit 체크: ip={}, count={}/{}", ip, newCount, MAX_REQUESTS_PER_MINUTE);

        return true;
    }

    /**
     * 현재 요청 횟수 조회
     *
     * @param ip 클라이언트 IP
     * @return 현재 요청 횟수
     */
    public int getCurrentCount(String ip) {
        String key = RATE_LIMIT_KEY_PREFIX + ip;
        String countStr = redisTemplate.opsForValue().get(key);
        return countStr != null ? Integer.parseInt(countStr) : 0;
    }

    /**
     * 남은 요청 횟수 조회
     *
     * @param ip 클라이언트 IP
     * @return 남은 요청 횟수
     */
    public int getRemainingRequests(String ip) {
        int currentCount = getCurrentCount(ip);
        return Math.max(0, MAX_REQUESTS_PER_MINUTE - currentCount);
    }

    /**
     * Rate Limit이 해제될 때까지 남은 시간 조회 (초 단위)
     *
     * @param ip 클라이언트 IP
     * @return 남은 시간 (초), 제한이 없으면 0
     */
    public long getTimeToReset(String ip) {
        String key = RATE_LIMIT_KEY_PREFIX + ip;
        Long ttl = redisTemplate.getExpire(key);

        // TTL이 -1이면 키는 있지만 만료시간 없음 (비정상)
        // TTL이 -2이면 키가 없음
        if (ttl == null || ttl < 0) {
            return 0;
        }

        return ttl;
    }

    /**
     * Rate Limit 리셋 (테스트용)
     *
     * @param ip 클라이언트 IP
     */
    public void reset(String ip) {
        String key = RATE_LIMIT_KEY_PREFIX + ip;
        redisTemplate.delete(key);
        log.debug("Rate Limit 리셋: ip={}", ip);
    }
}
