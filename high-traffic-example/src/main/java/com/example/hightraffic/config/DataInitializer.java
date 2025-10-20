package com.example.hightraffic.config;

import com.example.hightraffic.domain.Comment;
import com.example.hightraffic.domain.Post;
import com.example.hightraffic.repository.CommentRepository;
import com.example.hightraffic.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 애플리케이션 시작 시 테스트 데이터를 초기화하는 클래스
 *
 * @Profile("local"): local 프로파일에서만 실행
 * ApplicationRunner: 애플리케이션 시작 후 자동으로 실행
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final Random random = new Random();

    private static final String[] TITLES = {
            "Spring Boot 대용량 트래픽 처리 방법",
            "JPA 성능 최적화 전략",
            "Redis 캐싱 적용 가이드",
            "Kafka를 활용한 이벤트 기반 아키텍처",
            "데이터베이스 인덱스 최적화",
            "동시성 문제 해결하기",
            "분산 시스템 설계 패턴",
            "마이크로서비스 아키텍처 구축",
            "MySQL 복제 및 샤딩",
            "API 성능 튜닝 가이드"
    };

    private static final String[] CONTENT_TEMPLATES = {
            "이 글에서는 %s에 대해 자세히 알아보겠습니다. 실무에서 겪은 경험을 바탕으로 실질적인 해결 방법을 제시합니다.",
            "%s는 대규모 시스템에서 매우 중요한 주제입니다. 이번 포스팅에서는 실전 예제와 함께 설명하겠습니다.",
            "많은 개발자들이 %s에 대해 궁금해합니다. 이 글에서는 기초부터 고급 기법까지 단계별로 설명합니다.",
            "%s를 적용하면서 겪었던 시행착오와 최종적으로 찾은 최선의 방법을 공유합니다.",
            "실무에서 %s를 적용한 사례를 바탕으로 구체적인 구현 방법과 주의사항을 정리했습니다."
    };

    private static final String[] AUTHORS = {
            "김개발", "이백엔드", "박프론트", "최데브옵스", "정아키텍트",
            "강시니어", "윤주니어", "조풀스택", "장테크리드", "임CTO"
    };

    @Override
    public void run(ApplicationArguments args) {
        long count = postRepository.count();
        if (count > 0) {
            log.info("이미 데이터가 존재합니다. 초기화를 건너뜁니다. (현재 게시글 수: {})", count);
            return;
        }

        log.info("테스트 데이터 초기화를 시작합니다...");
        long startTime = System.currentTimeMillis();

        // 1. 게시글 생성
        List<Post> savedPosts = createPosts();

        // 2. 댓글 생성
        int totalComments = createComments(savedPosts);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("테스트 데이터 초기화 완료!");
        log.info("- 생성된 게시글 수: {}", savedPosts.size());
        log.info("- 생성된 댓글 수: {}", totalComments);
        log.info("- 소요 시간: {}ms", duration);
    }

    /**
     * 게시글 생성
     */
    private List<Post> createPosts() {
        log.info("게시글 생성 시작...");
        List<Post> posts = new ArrayList<>();
        int totalPosts = 100;

        for (int i = 1; i <= totalPosts; i++) {
            String title = generateTitle(i);
            String content = generateContent(title);
            String author = AUTHORS[random.nextInt(AUTHORS.length)];

            Post post = Post.builder()
                    .title(title)
                    .content(content)
                    .author(author)
                    .build();

            // 랜덤하게 조회수와 좋아요 수 설정
            for (int j = 0; j < random.nextInt(100); j++) {
                post.increaseViewCount();
            }
            for (int j = 0; j < random.nextInt(50); j++) {
                post.increaseLikeCount();
            }

            posts.add(post);

            // 배치 크기마다 저장
            if (posts.size() >= 50) {
                postRepository.saveAll(posts);
                posts.clear();
            }
        }

        // 남은 데이터 저장
        if (!posts.isEmpty()) {
            postRepository.saveAll(posts);
        }

        // 모든 게시글 다시 조회 (ID가 필요하므로)
        List<Post> savedPosts = postRepository.findAll();
        log.info("게시글 생성 완료: {}개", savedPosts.size());
        return savedPosts;
    }

    /**
     * 댓글 생성 (2 depth 및 무한 depth 예제 데이터)
     */
    private int createComments(List<Post> posts) {
        log.info("댓글 생성 시작...");
        List<Comment> comments = new ArrayList<>();
        int totalComments = 0;

        // 처음 10개 게시글에는 2 depth 방식 댓글 추가
        for (int i = 0; i < Math.min(10, posts.size()); i++) {
            Post post = posts.get(i);
            totalComments += createTwoDepthComments(post, comments);
        }

        // 다음 10개 게시글에는 무한 depth 방식 댓글 추가
        for (int i = 10; i < Math.min(20, posts.size()); i++) {
            Post post = posts.get(i);
            totalComments += createInfiniteDepthComments(post, comments);
        }

        // 남은 게시글에는 랜덤하게 댓글 추가
        for (int i = 20; i < posts.size(); i++) {
            Post post = posts.get(i);
            int commentCount = random.nextInt(5); // 0~4개의 댓글
            for (int j = 0; j < commentCount; j++) {
                Comment comment = Comment.createRoot(
                        post.getId(),
                        generateCommentContent(),
                        AUTHORS[random.nextInt(AUTHORS.length)]
                );
                comments.add(comment);
                totalComments++;
            }
        }

        // 배치로 저장
        if (!comments.isEmpty()) {
            commentRepository.saveAll(comments);
        }

        log.info("댓글 생성 완료: {}개", totalComments);
        return totalComments;
    }

    /**
     * 2 Depth 방식 댓글 생성 (댓글 + 대댓글)
     */
    private int createTwoDepthComments(Post post, List<Comment> comments) {
        int count = 0;

        // 3~5개의 루트 댓글 생성
        int rootCommentCount = 3 + random.nextInt(3);
        List<Comment> rootComments = new ArrayList<>();

        for (int i = 0; i < rootCommentCount; i++) {
            Comment rootComment = Comment.createRoot(
                    post.getId(),
                    generateCommentContent(),
                    AUTHORS[random.nextInt(AUTHORS.length)]
            );
            comments.add(rootComment);
            rootComments.add(rootComment);
            count++;
        }

        // 루트 댓글들을 먼저 저장 (ID 생성을 위해)
        commentRepository.saveAll(rootComments);

        // 각 루트 댓글에 1~3개의 대댓글 추가
        for (Comment rootComment : rootComments) {
            int replyCount = 1 + random.nextInt(3);
            for (int i = 0; i < replyCount; i++) {
                Comment reply = Comment.createChild(
                        rootComment,
                        post.getId(),
                        generateReplyContent(),
                        AUTHORS[random.nextInt(AUTHORS.length)]
                );
                comments.add(reply);
                count++;
            }
        }

        return count;
    }

    /**
     * 무한 Depth 방식 댓글 생성 (계층형 구조)
     */
    private int createInfiniteDepthComments(Post post, List<Comment> comments) {
        int count = 0;

        // 2~3개의 루트 댓글 생성
        int rootCommentCount = 2 + random.nextInt(2);
        List<Comment> rootComments = new ArrayList<>();

        for (int i = 0; i < rootCommentCount; i++) {
            Comment rootComment = Comment.createRoot(
                    post.getId(),
                    generateCommentContent(),
                    AUTHORS[random.nextInt(AUTHORS.length)]
            );
            comments.add(rootComment);
            rootComments.add(rootComment);
            count++;
        }

        // 루트 댓글들을 먼저 저장
        commentRepository.saveAll(rootComments);

        // 각 루트 댓글에 계층형 댓글 추가 (최대 4 depth)
        for (Comment rootComment : rootComments) {
            count += createNestedComments(post.getId(), rootComment, 1, 4, comments);
        }

        return count;
    }

    /**
     * 재귀적으로 중첩 댓글 생성
     */
    private int createNestedComments(Long postId, Comment parent, int currentDepth, int maxDepth, List<Comment> comments) {
        if (currentDepth >= maxDepth) {
            return 0;
        }

        int count = 0;
        int childCount = random.nextInt(3); // 0~2개의 자식 댓글

        for (int i = 0; i < childCount; i++) {
            Comment child = Comment.createChild(
                    parent,
                    postId,
                    generateReplyContent(),
                    AUTHORS[random.nextInt(AUTHORS.length)]
            );
            comments.add(child);
            count++;

            // 자식 댓글 저장 (ID 생성)
            commentRepository.save(child);

            // 재귀적으로 손자 댓글 생성
            count += createNestedComments(postId, child, currentDepth + 1, maxDepth, comments);
        }

        return count;
    }

    private String generateCommentContent() {
        String[] templates = {
                "좋은 글 감사합니다!",
                "많은 도움이 되었습니다.",
                "실무에 바로 적용해봐야겠네요.",
                "궁금했던 내용이었는데 잘 정리되어 있네요.",
                "추가로 궁금한 점이 있는데, 더 자세히 설명해주실 수 있나요?"
        };
        return templates[random.nextInt(templates.length)];
    }

    private String generateReplyContent() {
        String[] templates = {
                "좋은 의견 감사합니다!",
                "저도 같은 생각입니다.",
                "그 부분은 이렇게 해결할 수 있습니다.",
                "추가 설명드리자면...",
                "동의합니다!"
        };
        return templates[random.nextInt(templates.length)];
    }

    private String generateTitle(int index) {
        if (index <= TITLES.length) {
            return String.format("[%d] %s", index, TITLES[index - 1]);
        }
        String randomTitle = TITLES[random.nextInt(TITLES.length)];
        return String.format("[%d] %s (추가)", index, randomTitle);
    }

    private String generateContent(String title) {
        String template = CONTENT_TEMPLATES[random.nextInt(CONTENT_TEMPLATES.length)];
        String mainTopic = title.replaceAll("\\[\\d+\\]\\s*", "").replaceAll("\\s*\\(추가\\)\\s*", "");

        StringBuilder content = new StringBuilder();
        content.append(String.format(template, mainTopic)).append("\n\n");

        content.append("## 주요 내용\n\n");
        content.append("1. 기본 개념 이해\n");
        content.append("2. 실전 적용 방법\n");
        content.append("3. 성능 최적화 전략\n");
        content.append("4. 트러블슈팅 가이드\n\n");

        content.append("## 결론\n\n");
        content.append(mainTopic).append("를 적용하면 시스템의 성능과 안정성을 크게 향상시킬 수 있습니다. ");
        content.append("실무에 적용할 때는 각 환경의 특성을 고려하여 최적화하는 것이 중요합니다.");

        return content.toString();
    }
}
