package com.example.redis.service;

import com.example.redis.dto.RankingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 랭킹 서비스
 * - Redis Sorted Set을 활용한 실시간 랭킹 시스템
 * - 게임 점수, 인기 게시글 등에 활용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis Key: ranking:game (게임 랭킹)
    private static final String RANKING_KEY = "ranking:game";

    /**
     * 점수 추가/업데이트
     * - Sorted Set에 사용자 점수 저장
     * - 같은 사용자면 점수 업데이트
     *
     * @param userId 사용자 ID
     * @param score 점수
     */
    public void addScore(String userId, double score) {
        redisTemplate.opsForZSet().add(RANKING_KEY, userId, score);
        log.info("랭킹 점수 추가 - User: {}, Score: {}", userId, score);
    }

    /**
     * 점수 증가
     * - 기존 점수에 더하기
     *
     * @param userId 사용자 ID
     * @param delta 증가시킬 점수
     */
    public Double incrementScore(String userId, double delta) {
        Double newScore = redisTemplate.opsForZSet().incrementScore(RANKING_KEY, userId, delta);
        log.info("랭킹 점수 증가 - User: {}, Delta: {}, New Score: {}", userId, delta, newScore);
        return newScore;
    }

    /**
     * 상위 N명 조회 (1위부터)
     * - 점수가 높은 순서대로 반환
     *
     * @param topN 조회할 상위 N명
     * @return 랭킹 리스트
     */
    public List<RankingResponse> getTopRankings(int topN) {
        // reverseRangeWithScores: 점수 높은 순으로 정렬 (내림차순)
        Set<ZSetOperations.TypedTuple<Object>> rankings =
            redisTemplate.opsForZSet().reverseRangeWithScores(RANKING_KEY, 0, topN - 1);

        List<RankingResponse> result = new ArrayList<>();
        long rank = 1;

        if (rankings != null) {
            for (ZSetOperations.TypedTuple<Object> ranking : rankings) {
                result.add(RankingResponse.builder()
                    .rank(rank++)
                    .userId(ranking.getValue().toString())
                    .score(ranking.getScore())
                    .build());
            }
        }

        log.info("상위 {} 랭킹 조회 완료", topN);
        return result;
    }

    /**
     * 특정 사용자의 순위 조회
     * - 1위면 0 반환 (Redis는 0부터 시작)
     *
     * @param userId 사용자 ID
     * @return 순위 (1위부터 시작, 없으면 null)
     */
    public Long getUserRank(String userId) {
        Long rank = redisTemplate.opsForZSet().reverseRank(RANKING_KEY, userId);

        if (rank != null) {
            // Redis는 0부터 시작하므로 +1
            long actualRank = rank + 1;
            log.info("사용자 {} 순위: {}", userId, actualRank);
            return actualRank;
        }

        log.info("사용자 {} 랭킹 데이터 없음", userId);
        return null;
    }

    /**
     * 특정 사용자의 점수 조회
     *
     * @param userId 사용자 ID
     * @return 점수 (없으면 null)
     */
    public Double getUserScore(String userId) {
        Double score = redisTemplate.opsForZSet().score(RANKING_KEY, userId);
        log.info("사용자 {} 점수: {}", userId, score);
        return score;
    }

    /**
     * 특정 사용자의 랭킹 정보 조회 (순위 + 점수)
     *
     * @param userId 사용자 ID
     * @return 랭킹 정보
     */
    public RankingResponse getUserRanking(String userId) {
        Long rank = getUserRank(userId);
        Double score = getUserScore(userId);

        if (rank != null && score != null) {
            return RankingResponse.builder()
                .rank(rank)
                .userId(userId)
                .score(score)
                .build();
        }

        return null;
    }

    /**
     * 전체 랭킹 인원 수
     *
     * @return 랭킹에 등록된 사용자 수
     */
    public Long getRankingCount() {
        Long count = redisTemplate.opsForZSet().size(RANKING_KEY);
        log.info("전체 랭킹 인원: {}", count);
        return count != null ? count : 0L;
    }

    /**
     * 사용자 삭제
     *
     * @param userId 삭제할 사용자 ID
     */
    public void removeUser(String userId) {
        redisTemplate.opsForZSet().remove(RANKING_KEY, userId);
        log.info("랭킹에서 사용자 삭제: {}", userId);
    }

    /**
     * 전체 랭킹 초기화
     */
    public void clearRanking() {
        redisTemplate.delete(RANKING_KEY);
        log.info("전체 랭킹 초기화");
    }

}
