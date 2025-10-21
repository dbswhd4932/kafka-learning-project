package com.example.redis.service;

import com.example.redis.domain.Post;
import com.example.redis.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 조회수 관리 서비스
 * - Redis를 활용한 원자적(Atomic) 조회수 증가
 * - 동시성 문제 해결
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostRepository postRepository;

    // Redis Key 형식: view:count:post:{postId}
    private static final String VIEW_COUNT_KEY_PREFIX = "view:count:post:";

    /**
     * 게시글 조회수 증가 (Redis 사용)
     *
     * 프로세스:
     * 1. Redis에서 INCR 명령어로 조회수 원자적 증가
     * 2. INCR은 싱글 스레드로 동작하므로 동시성 문제 없음
     * 3. 10개 스레드가 동시에 접근해도 정확히 10 증가
     *
     * @param postId 게시글 ID
     * @return 증가 후 조회수
     */
    public Long increaseViewCount(Long postId) {
        String key = VIEW_COUNT_KEY_PREFIX + postId;

        // Redis INCR: 원자적 연산으로 값을 1 증가시킴
        // - 싱글 스레드로 동작하므로 Race Condition 발생 안함
        // - 여러 스레드가 동시에 호출해도 정확한 카운팅 보장
        Long newCount = redisTemplate.opsForValue().increment(key, 1);

        log.debug("게시글 {} 조회수 증가: {}", postId, newCount);

        return newCount;
    }

    /**
     * Redis에서 현재 조회수 조회
     *
     * @param postId 게시글 ID
     * @return 현재 조회수 (Redis에 없으면 0)
     */
    public Long getViewCount(Long postId) {
        String key = VIEW_COUNT_KEY_PREFIX + postId;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return 0L;
        }

        // Redis에서 가져온 값을 Long으로 변환
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        return (Long) value;
    }

    /**
     * Redis 조회수를 DB와 동기화
     * - 실무에서는 스케줄러로 주기적으로 실행
     * - 또는 일정 횟수마다 동기화
     *
     * @param postId 게시글 ID
     */
    @Transactional
    public void syncViewCountToDatabase(Long postId) {
        String key = VIEW_COUNT_KEY_PREFIX + postId;
        Long redisViewCount = getViewCount(postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // DB 조회수를 Redis 값으로 업데이트
        post.setViewCount(redisViewCount);
        postRepository.save(post);

        log.info("게시글 {} 조회수 DB 동기화 완료: {}", postId, redisViewCount);
    }

    /**
     * Redis 조회수 초기화 (테스트용)
     *
     * @param postId 게시글 ID
     */
    public void resetViewCount(Long postId) {
        String key = VIEW_COUNT_KEY_PREFIX + postId;
        redisTemplate.delete(key);
        log.info("게시글 {} Redis 조회수 초기화", postId);
    }

    /**
     * Redis 조회수 설정 (테스트용)
     *
     * @param postId 게시글 ID
     * @param count 설정할 조회수
     */
    public void setViewCount(Long postId, Long count) {
        String key = VIEW_COUNT_KEY_PREFIX + postId;
        redisTemplate.opsForValue().set(key, count);
        log.info("게시글 {} Redis 조회수 설정: {}", postId, count);
    }
}
