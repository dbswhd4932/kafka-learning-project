package com.example.redis.service;

import com.example.redis.domain.Post;
import com.example.redis.repository.PostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 조회수 동시성 테스트
 * - Redis의 INCR 명령어를 사용한 원자적 연산 검증
 * - 10개 스레드가 동시에 접근해도 정확히 10 증가하는지 확인
 */
@SpringBootTest
class ViewCountConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(ViewCountConcurrencyTest.class);

    @Autowired
    private ViewCountService viewCountService;

    @Autowired
    private PostRepository postRepository;

    private Post testPost;

    /**
     * 테스트 전 준비 작업
     * - 테스트용 게시글 생성
     * - Redis 조회수 초기화
     */
    @BeforeEach
    void setUp() {
        // Given: 테스트용 게시글 생성
        testPost = Post.builder()
                .title("동시성 테스트 게시글")
                .content("10개 스레드가 동시에 접근하는 테스트")
                .author("tester")
                .viewCount(0L)
                .build();
        testPost = postRepository.save(testPost);

        // Redis 조회수 초기화
        viewCountService.resetViewCount(testPost.getId());

        log.info("테스트 게시글 생성 완료 - ID: {}", testPost.getId());
    }

    /**
     * 테스트 후 정리 작업
     * - 테스트용 데이터 삭제
     */
    @AfterEach
    void tearDown() {
        if (testPost != null) {
            viewCountService.resetViewCount(testPost.getId());
            postRepository.deleteById(testPost.getId());
            log.info("테스트 데이터 정리 완료");
        }
    }

    /**
     * [핵심 테스트] 10개 스레드 동시 접근 시 조회수 정확성 검증
     *
     * 테스트 프로세스:
     * 1. 10개의 스레드 풀 생성
     * 2. CountDownLatch로 모든 스레드가 동시에 시작하도록 동기화
     * 3. 각 스레드가 조회수 증가 메서드 호출
     * 4. 모든 스레드 작업 완료 대기
     * 5. 최종 조회수가 정확히 10인지 검증
     *
     * 예상 결과:
     * - Redis INCR은 원자적 연산이므로 Race Condition 발생 안함
     * - 10개 스레드가 각각 1씩 증가 → 최종 조회수 = 10
     */
    @Test
    @DisplayName("10개 스레드가 동시에 조회수 증가 시 정확히 10이 증가해야 한다")
    void testConcurrentViewCountIncrease() throws InterruptedException {
        // Given: 테스트 설정
        int threadCount = 10; // 동시 실행할 스레드 개수
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // CountDownLatch: 모든 스레드를 동시에 시작시키기 위한 동기화 도구
        // - 카운트가 0이 될 때까지 대기
        // - 각 스레드가 await() 호출 후 countDown()으로 카운트 감소
        CountDownLatch startLatch = new CountDownLatch(1);  // 시작 신호용
        CountDownLatch endLatch = new CountDownLatch(threadCount);  // 완료 대기용

        // 성공 카운터 (몇 개 스레드가 성공했는지 추적)
        AtomicInteger successCount = new AtomicInteger(0);

        log.info("=== 동시성 테스트 시작 ===");
        log.info("스레드 개수: {}", threadCount);
        log.info("게시글 ID: {}", testPost.getId());

        // When: 10개 스레드 동시 실행
        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i + 1;

            executorService.submit(() -> {
                try {
                    // [1단계] 시작 신호 대기
                    // - 모든 스레드가 여기서 대기
                    // - startLatch.countDown() 호출 시 동시에 시작
                    log.debug("스레드-{}: 시작 신호 대기 중...", threadNumber);
                    startLatch.await();

                    // [2단계] 조회수 증가 (Redis INCR 호출)
                    // - 이 부분이 동시에 실행됨
                    // - Redis INCR은 싱글 스레드로 처리되므로 원자적 연산 보장
                    log.debug("스레드-{}: 조회수 증가 시작", threadNumber);
                    Long newCount = viewCountService.increaseViewCount(testPost.getId());
                    log.info("스레드-{}: 조회수 증가 완료 (현재 값: {})", threadNumber, newCount);

                    // [3단계] 성공 카운트 증가
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("스레드-{}: 에러 발생", threadNumber, e);
                } finally {
                    // [4단계] 완료 신호
                    // - 이 스레드의 작업이 끝났음을 알림
                    endLatch.countDown();
                }
            });
        }

        // [중요] 모든 스레드 동시 시작 신호 발생
        log.info(">>> 모든 스레드 동시 시작!");
        startLatch.countDown();

        // [중요] 모든 스레드 작업 완료 대기
        // - endLatch 카운트가 0이 될 때까지 대기 (10개 스레드 모두 완료)
        log.info("모든 스레드 완료 대기 중...");
        endLatch.await();
        log.info("모든 스레드 작업 완료!");

        // ExecutorService 종료
        executorService.shutdown();

        // Then: 검증
        // [검증 1] 모든 스레드가 성공적으로 완료되었는지 확인
        log.info("=== 테스트 결과 검증 ===");
        log.info("성공한 스레드 수: {}/{}", successCount.get(), threadCount);
        assertThat(successCount.get()).isEqualTo(threadCount);

        // [검증 2] Redis에 저장된 최종 조회수 확인
        Long finalViewCount = viewCountService.getViewCount(testPost.getId());
        log.info("최종 조회수: {}", finalViewCount);

        // [핵심 검증] 10개 스레드가 각각 1씩 증가 → 최종 조회수는 정확히 10
        assertThat(finalViewCount).isEqualTo((long) threadCount);

        log.info("✅ 테스트 성공: {}개 스레드가 동시 접근해도 조회수가 정확히 {}로 증가했습니다!",
                threadCount, threadCount);
    }

    /**
     * [추가 테스트] 100개 스레드 동시 접근 테스트
     * - 대규모 동시성 상황에서도 정확성 검증
     */
    @Test
    @DisplayName("100개 스레드가 동시에 조회수 증가 시 정확히 100이 증가해야 한다")
    void testConcurrentViewCountIncrease_100Threads() throws InterruptedException {
        // Given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        log.info("=== 대규모 동시성 테스트 시작 ({}개 스레드) ===", threadCount);

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    viewCountService.increaseViewCount(testPost.getId());
                } catch (Exception e) {
                    log.error("에러 발생", e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        // Then
        Long finalViewCount = viewCountService.getViewCount(testPost.getId());
        log.info("최종 조회수: {}/{}", finalViewCount, threadCount);

        assertThat(finalViewCount).isEqualTo((long) threadCount);
        log.info("✅ 대규모 테스트 성공: {}개 스레드 동시 접근 시에도 정확한 카운팅!", threadCount);
    }

    /**
     * [비교 테스트] Redis 없이 단순 증가 시 동시성 문제 발생 확인
     * - 이 테스트는 실패할 가능성이 높음 (Race Condition 발생)
     * - Redis의 원자적 연산의 중요성을 확인하기 위한 테스트
     */
    @Test
    @DisplayName("[실패 예상] 동기화 없이 조회수 증가 시 Race Condition 발생")
    void testWithoutSynchronization() throws InterruptedException {
        // Given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // 공유 변수 (동기화 없음)
        AtomicInteger unsafeCounter = new AtomicInteger(0);

        log.info("=== 동기화 없는 증가 테스트 시작 ===");

        // When: 동기화 없이 증가
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    // 동기화 없이 증가 (Race Condition 발생 가능)
                    int current = unsafeCounter.get();
                    Thread.sleep(1); // 의도적인 지연
                    unsafeCounter.set(current + 1);

                } catch (Exception e) {
                    log.error("에러 발생", e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        // Then
        int finalCount = unsafeCounter.get();
        log.warn("동기화 없는 최종 카운트: {}/{}", finalCount, threadCount);

        // 이 테스트는 실패할 가능성이 높음
        // - 동기화가 없어서 Race Condition 발생
        // - 여러 스레드가 같은 값을 읽고 덮어쓰기 때문
        log.warn("⚠️  동기화 없이는 정확한 카운팅이 보장되지 않습니다!");
        log.info("Redis INCR을 사용하면 이런 문제를 완벽하게 해결할 수 있습니다.");
    }
}
