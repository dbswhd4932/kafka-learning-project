package com.example.hightraffic.config;

import com.example.hightraffic.domain.Post;
import com.example.hightraffic.repository.PostRepository;
import com.example.hightraffic.service.ViewCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 조회수 초기화
 *
 * 애플리케이션 시작 시:
 * 1. DB에서 모든 게시글의 조회수 조회
 * 2. Redis에 조회수 초기화
 *
 * 목적:
 * - 서버 재시작 후에도 조회수 정합성 유지
 * - Redis에 데이터가 없을 때 DB에서 복원
 *
 * Profile:
 * - local: 개발 환경에서만 실행
 * - 프로덕션에서는 DataInitializer와 함께 조건부 실행 가능
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class ViewCountInitializer implements ApplicationRunner {

    private final PostRepository postRepository;
    private final ViewCountService viewCountService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== 조회수 초기화 시작 ===");

        try {
            // 모든 게시글 조회
            List<Post> posts = postRepository.findAll();

            int initializedCount = 0;
            int skippedCount = 0;

            for (Post post : posts) {
                try {
                    // Redis에 조회수 초기화
                    // (Redis에 이미 값이 있으면 스킵)
                    viewCountService.initializeViewCount(post.getId(), post.getViewCount());
                    initializedCount++;
                } catch (Exception e) {
                    log.error("조회수 초기화 실패: postId={}", post.getId(), e);
                    skippedCount++;
                }
            }

            log.info("=== 조회수 초기화 완료: 성공={}, 건너뜀={}, 전체={} ===",
                    initializedCount, skippedCount, posts.size());

        } catch (Exception e) {
            log.error("조회수 초기화 중 에러 발생", e);
        }
    }
}
