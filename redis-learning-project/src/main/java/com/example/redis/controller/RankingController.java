package com.example.redis.controller;

import com.example.redis.dto.*;
import com.example.redis.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 랭킹 컨트롤러
 * - Redis Sorted Set을 활용한 실시간 랭킹 API
 */
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
@Slf4j
public class RankingController {

    private final RankingService rankingService;

    /**
     * 점수 추가/업데이트
     * POST /api/ranking/score
     *
     * Request: ?userId=user1&score=1000
     */
    @PostMapping("/score")
    public ResponseEntity<ScoreUpdateResponse> addScore(
            @RequestParam String userId,
            @RequestParam double score) {

        rankingService.addScore(userId, score);

        ScoreUpdateResponse response = ScoreUpdateResponse.builder()
                .message("점수가 등록되었습니다.")
                .userId(userId)
                .score(score)
                .build();

        log.info("점수 등록 - User: {}, Score: {}", userId, score);

        return ResponseEntity.ok(response);
    }

    /**
     * 점수 증가
     * POST /api/ranking/score/increment
     *
     * Request: ?userId=user1&delta=100
     */
    @PostMapping("/score/increment")
    public ResponseEntity<ScoreUpdateResponse> incrementScore(
            @RequestParam String userId,
            @RequestParam double delta) {

        Double newScore = rankingService.incrementScore(userId, delta);

        ScoreUpdateResponse response = ScoreUpdateResponse.builder()
                .message("점수가 증가되었습니다.")
                .userId(userId)
                .delta(delta)
                .newScore(newScore)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 상위 N명 랭킹 조회
     * GET /api/ranking/top?limit=10
     */
    @GetMapping("/top")
    public ResponseEntity<TopRankingResponse> getTopRankings(
            @RequestParam(defaultValue = "10") int limit) {

        List<RankingResponse> rankings = rankingService.getTopRankings(limit);

        TopRankingResponse response = TopRankingResponse.builder()
                .data(rankings)
                .count(rankings.size())
                .build();

        log.info("상위 {} 랭킹 조회", limit);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 사용자 랭킹 조회
     * GET /api/ranking/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserRankingResponse> getUserRanking(@PathVariable String userId) {

        RankingResponse ranking = rankingService.getUserRanking(userId);

        UserRankingResponse response;

        if (ranking != null) {
            response = UserRankingResponse.builder()
                    .data(ranking)
                    .found(true)
                    .build();
        } else {
            response = UserRankingResponse.builder()
                    .message("랭킹 데이터가 없습니다.")
                    .found(false)
                    .build();
        }

        log.info("사용자 랭킹 조회 - User: {}", userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 전체 랭킹 인원 수 조회
     * GET /api/ranking/count
     */
    @GetMapping("/count")
    public ResponseEntity<RankingCountResponse> getRankingCount() {

        Long count = rankingService.getRankingCount();

        RankingCountResponse response = RankingCountResponse.builder()
                .totalUsers(count)
                .build();

        log.info("전체 랭킹 인원: {}", count);

        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 삭제
     * DELETE /api/ranking/user/{userId}
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<MessageResponse> removeUser(@PathVariable String userId) {

        rankingService.removeUser(userId);

        MessageResponse response = MessageResponse.builder()
                .message("사용자가 랭킹에서 삭제되었습니다.")
                .userId(userId)
                .build();

        log.info("사용자 삭제 - User: {}", userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 전체 랭킹 초기화
     * DELETE /api/ranking/all
     */
    @DeleteMapping("/all")
    public ResponseEntity<MessageResponse> clearRanking() {

        rankingService.clearRanking();

        MessageResponse response = MessageResponse.builder()
                .message("전체 랭킹이 초기화되었습니다.")
                .build();

        log.info("전체 랭킹 초기화");

        return ResponseEntity.ok(response);
    }

}
