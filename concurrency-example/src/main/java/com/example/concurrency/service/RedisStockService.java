package com.example.concurrency.service;

import com.example.concurrency.domain.Stock;
import com.example.concurrency.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Redis Lettuce를 사용한 분산 락 기반 재고 관리 서비스
 *
 * [동작 원리]
 * Redis의 SETNX (SET if Not eXists) 명령어를 사용하여 분산 락을 구현합니다.
 * SETNX는 키가 존재하지 않을 때만 값을 설정하고 true를 반환합니다.
 * 이를 통해 여러 서버/Pod에서 동시에 접근해도 하나의 요청만 락을 획득할 수 있습니다.
 *
 * [장점]
 * 1. 분산 환경에서 동작 - 여러 서버/Pod에서도 동시성 제어 가능
 * 2. Kubernetes, Auto Scaling 환경에서 사용 가능
 * 3. MSA 환경에서도 사용 가능
 * 4. 재고 ID별로 독립적인 락 설정 가능 (성능 향상)
 * 5. TTL 설정으로 데드락 방지
 *
 * [단점]
 * 1. Redis 인프라 필요 (추가 비용)
 * 2. Redis 장애 시 서비스 영향
 * 3. 네트워크 I/O 발생 (synchronized보다 느림)
 * 4. 스핀 락 방식으로 구현 시 CPU 사용량 증가
 * 5. 구현 복잡도 증가
 *
 * [사용 가능한 환경]
 * ✅ Kubernetes Pod가 여러 개인 경우
 * ✅ Auto Scaling 사용 시
 * ✅ 로드 밸런서 뒤에 여러 서버가 있는 경우
 * ✅ MSA 환경
 * ✅ 높은 트래픽 환경
 *
 * [Lettuce vs Redisson]
 * - Lettuce: Spring Boot 기본 Redis 클라이언트, 수동 락 구현 필요, 스핀 락 방식
 * - Redisson: 락 기능 내장, Pub/Sub 방식으로 효율적, 하지만 추가 의존성 필요
 *
 * 이 서비스는 Lettuce를 사용하여 스핀 락 방식으로 구현합니다.
 *
 * [트랜잭션 처리]
 * - @Transactional을 반드시 사용해야 합니다!
 * - 락 획득 후 DB 작업 중 예외 발생 시 롤백 필요
 * - 락은 finally 블록에서 해제, 트랜잭션은 @Transactional이 자동 롤백
 *
 * [예외 발생 시 동작]
 * ```
 * tryLock()              ← 락 획득
 *   ↓
 * @Transactional 시작
 *   ↓
 * DB 작업 중 예외 발생!
 *   ↓
 * @Transactional 롤백    ← 자동 롤백
 *   ↓
 * finally unlock()       ← 락 해제
 * ```
 *
 * [주의사항]
 * - 락 획득 실패 시 재시도 로직 필요 (스핀 락)
 * - TTL 설정 필수 (데드락 방지)
 * - finally 블록에서 반드시 락 해제
 * - Redis 싱글 스레드 특성으로 인한 병목 가능성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStockService {

    private final StockRepository stockRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String LOCK_KEY_PREFIX = "stock:lock:";
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(3); // 락 타임아웃 3초
    private static final long SPIN_LOCK_RETRY_INTERVAL_MS = 50; // 재시도 간격 50ms
    private static final long SPIN_LOCK_MAX_ATTEMPTS = 100; // 최대 100번 재시도 (5초)

    /**
     * Redis 분산 락을 사용한 재고 감소
     *
     * [동작 과정]
     * 1. Redis에 락 키 생성 시도 (SETNX)
     * 2. 락 획득 실패 시 50ms 대기 후 재시도 (스핀 락)
     * 3. 락 획득 성공 시:
     *    - 트랜잭션 시작 (@Transactional)
     *    - 재고 조회 및 감소
     *    - DB 저장
     *    - 트랜잭션 커밋
     *    - Redis 락 해제 (DELETE, finally)
     *
     * [스핀 락 방식]
     * ```
     * while (락 획득 실패) {
     *     50ms 대기
     *     락 획득 재시도
     *     최대 100번 시도 (5초 타임아웃)
     * }
     * ```
     *
     * [분산 환경에서의 동작]
     * ```
     * [Pod A] Redis SETNX stock:lock:1 → 성공 → 재고 감소
     * [Pod B] Redis SETNX stock:lock:1 → 실패 (이미 존재) → 대기
     * [Pod C] Redis SETNX stock:lock:1 → 실패 (이미 존재) → 대기
     * [Pod A] 재고 감소 완료 → Redis DEL stock:lock:1
     * [Pod B] Redis SETNX stock:lock:1 → 성공 → 재고 감소
     * ```
     *
     * [데드락 방지]
     * - Redis 락에 TTL 설정 (3초)
     * - 애플리케이션 비정상 종료 시에도 3초 후 자동 락 해제
     * - finally 블록에서 명시적 락 해제
     *
     * [성능 최적화]
     * - 재고 ID별로 독립적인 락 키 생성 (stock:lock:1, stock:lock:2, ...)
     * - 상품 A 처리 중에도 상품 B는 동시 처리 가능
     *
     * [트랜잭션과 락의 범위]
     * ```
     * tryLock()              ← 락 시작
     *   ↓
     * @Transactional 시작    ← 트랜잭션 시작
     *   ↓
     * DB 조회/수정/저장
     *   ↓
     * @Transactional 커밋    ← 트랜잭션 종료
     *   ↓
     * finally unlock()      ← 락 해제
     * ```
     * 락의 범위가 트랜잭션보다 크므로 안전합니다!
     *
     * [예외 발생 시 처리]
     * - DB 작업 중 예외 발생 → @Transactional이 자동 롤백
     * - finally 블록에서 락 해제
     * - 따라서 데이터 일관성 보장
     *
     * @param id 재고 ID
     * @param quantity 감소할 수량
     * @throws IllegalArgumentException 재고를 찾을 수 없는 경우
     * @throws IllegalStateException 재고가 부족하거나 락 획득 실패한 경우
     */
    @Transactional
    public void decrease(Long id, Long quantity) {
        String lockKey = LOCK_KEY_PREFIX + id;
        String lockValue = Thread.currentThread().getName() + ":" + System.currentTimeMillis();

        log.info("[Redis Lock] 락 획득 시도 - 재고 ID: {}, 스레드: {}",
                id, Thread.currentThread().getName());

        // 스핀 락 방식으로 락 획득 시도
        boolean lockAcquired = false;
        int attempts = 0;

        try {
            while (!lockAcquired && attempts < SPIN_LOCK_MAX_ATTEMPTS) {
                // SETNX 명령어로 락 획득 시도
                lockAcquired = Boolean.TRUE.equals(
                        redisTemplate.opsForValue()
                                .setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT)
                );

                if (!lockAcquired) {
                    attempts++;
                    log.debug("[Redis Lock] 락 획득 실패 ({}번째 시도) - 재고 ID: {}, 스레드: {}",
                            attempts, id, Thread.currentThread().getName());

                    // 50ms 대기 후 재시도
                    Thread.sleep(SPIN_LOCK_RETRY_INTERVAL_MS);
                }
            }

            if (!lockAcquired) {
                log.error("[Redis Lock] 락 획득 타임아웃 - 재고 ID: {}, 스레드: {}, 시도 횟수: {}",
                        id, Thread.currentThread().getName(), attempts);
                throw new IllegalStateException("락 획득에 실패했습니다. 잠시 후 다시 시도해주세요.");
            }

            log.info("[Redis Lock] 락 획득 성공 - 재고 ID: {}, 스레드: {}, 시도 횟수: {}",
                    id, Thread.currentThread().getName(), attempts + 1);

            // 락 획득 후 재고 감소 처리
            Stock stock = stockRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다."));

            stock.decrease(quantity);
            stockRepository.saveAndFlush(stock);

            log.info("[Redis Lock] 재고 감소 완료 - ID: {}, 남은 재고: {}, 스레드: {}",
                    id, stock.getQuantity(), Thread.currentThread().getName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Redis Lock] 락 획득 중 인터럽트 발생 - 재고 ID: {}", id, e);
            throw new IllegalStateException("재고 감소 처리 중 오류가 발생했습니다.", e);

        } finally {
            // 락 해제
            if (lockAcquired) {
                // 자신이 설정한 락인지 확인 후 삭제 (안전성 향상)
                String currentValue = redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(currentValue)) {
                    redisTemplate.delete(lockKey);
                    log.info("[Redis Lock] 락 해제 완료 - 재고 ID: {}, 스레드: {}",
                            id, Thread.currentThread().getName());
                } else {
                    log.warn("[Redis Lock] 락 해제 실패 - 다른 스레드가 락을 소유 중 - 재고 ID: {}", id);
                }
            }
        }
    }

    /**
     * 재고 조회
     *
     * @param id 재고 ID
     * @return 재고 엔티티
     */
    public Stock getStock(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다."));
    }
}
