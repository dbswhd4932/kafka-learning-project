package com.example.concurrency.service;

import com.example.concurrency.domain.Stock;
import com.example.concurrency.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Redisson을 사용한 분산 락 기반 재고 관리 서비스
 *
 * [동작 원리]
 * Redisson의 RLock을 사용하여 분산 락을 구현합니다.
 * Lettuce와 달리 Pub/Sub 방식을 사용하여 스핀 락의 단점을 개선했습니다.
 *
 * [Lettuce vs Redisson 비교]
 *
 * | 항목 | Lettuce (SETNX) | Redisson (RLock) |
 * |------|----------------|------------------|
 * | 구현 방식 | 스핀 락 (계속 재시도) | Pub/Sub (대기 후 알림) |
 * | CPU 사용률 | 높음 (계속 polling) | 낮음 (이벤트 기반) |
 * | 성능 | 느림 | 빠름 |
 * | 구현 난이도 | 높음 (수동 구현) | 낮음 (내장 기능) |
 * | 락 해제 보장 | 수동 처리 필요 | 자동 처리 (Watchdog) |
 * | TTL 갱신 | 수동 | 자동 (Watchdog) |
 *
 * [Redisson의 장점]
 * 1. Pub/Sub 방식으로 효율적인 대기
 *    - 락이 해제되면 Redis가 대기 중인 클라이언트에게 알림
 *    - 불필요한 재시도 감소
 *
 * 2. Watchdog 메커니즘
 *    - 락을 획득한 스레드가 작업 중이면 자동으로 TTL 연장
 *    - 작업이 완료되지 않았는데 락이 해제되는 문제 방지
 *
 * 3. 구현이 간단
 *    - tryLock(), unlock()만 사용하면 됨
 *    - 락 해제, TTL 관리 등 자동 처리
 *
 * [장점]
 * 1. 분산 환경에서 동작 (여러 Pod/서버)
 * 2. Lettuce보다 성능이 좋음 (Pub/Sub 방식)
 * 3. CPU 사용률이 낮음 (스핀 락 없음)
 * 4. 구현이 간단 (Lettuce보다 코드가 짧음)
 * 5. 락 해제 자동 보장 (Watchdog)
 *
 * [단점]
 * 1. Redis 인프라 필요
 * 2. Redisson 라이브러리 추가 의존성
 * 3. Redis 장애 시 서비스 영향
 * 4. 네트워크 I/O 발생 (synchronized보다 느림)
 *
 * [사용 가능한 환경]
 * ✅ Kubernetes Pod가 여러 개인 경우
 * ✅ Auto Scaling 사용 시
 * ✅ 로드 밸런서 뒤에 여러 서버가 있는 경우
 * ✅ MSA 환경
 * ✅ 높은 트래픽 환경 (Lettuce보다 성능 좋음)
 *
 * [트랜잭션 처리]
 * - @Transactional을 반드시 사용해야 합니다!
 * - 락 획득 후 DB 작업 중 예외 발생 시 롤백 필요
 * - 락은 finally 블록에서 해제, 트랜잭션은 @Transactional이 자동 롤백
 *
 * [주의사항]
 * - tryLock() 타임아웃 설정 필수
 * - finally 블록에서 반드시 unlock() 호출
 * - unlock() 전에 isHeldByCurrentThread() 확인 권장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedissonStockService {

    private final StockRepository stockRepository;
    private final RedissonClient redissonClient;

    private static final String LOCK_KEY_PREFIX = "stock:lock:";
    private static final long LOCK_WAIT_TIME_SECONDS = 5L;  // 락 획득 대기 시간
    private static final long LOCK_LEASE_TIME_SECONDS = 3L; // 락 유지 시간

    /**
     * Redisson 분산 락을 사용한 재고 감소
     *
     * [동작 과정]
     * 1. Redisson RLock 객체 생성 (재고 ID별)
     * 2. tryLock()으로 락 획득 시도 (최대 5초 대기)
     * 3. 락 획득 성공 시:
     *    - 트랜잭션 시작 (@Transactional)
     *    - 재고 조회 및 감소
     *    - DB 저장
     *    - 트랜잭션 커밋
     *    - 락 해제 (finally)
     * 4. 락 획득 실패 시: 예외 발생
     *
     * [Pub/Sub 방식 동작]
     * ```
     * Thread A: tryLock() → 성공 → 재고 감소
     * Thread B: tryLock() → 실패 → Redis subscribe (대기)
     * Thread C: tryLock() → 실패 → Redis subscribe (대기)
     * Thread A: unlock() → Redis publish (알림)
     * Thread B: 알림 받음 → tryLock() → 성공 → 재고 감소
     * Thread C: 대기 중...
     * ```
     *
     * [Lettuce vs Redisson 동작 비교]
     *
     * **Lettuce (스핀 락)**
     * ```
     * while (!락 획득) {
     *     Thread.sleep(50ms);  // CPU 계속 사용
     *     락 획득 재시도;
     * }
     * ```
     *
     * **Redisson (Pub/Sub)**
     * ```
     * if (!락 획득) {
     *     Redis subscribe (대기);  // CPU 사용 안 함
     *     락 해제 알림 받으면 깨어남;
     * }
     * ```
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
     * unlock()              ← 락 해제
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
        RLock lock = redissonClient.getLock(lockKey);

        log.info("[Redisson Lock] 락 획득 시도 - 재고 ID: {}, 스레드: {}",
                id, Thread.currentThread().getName());

        try {
            // tryLock(대기시간, 락유지시간, 시간단위)
            // - 대기시간: 락 획득을 위해 최대 얼마나 기다릴지
            // - 락유지시간: 락을 획득한 후 최대 얼마나 유지할지 (Watchdog 사용 시 자동 연장)
            boolean available = lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, TimeUnit.SECONDS);

            if (!available) {
                log.error("[Redisson Lock] 락 획득 타임아웃 - 재고 ID: {}, 스레드: {}",
                        id, Thread.currentThread().getName());
                throw new IllegalStateException("락 획득에 실패했습니다. 잠시 후 다시 시도해주세요.");
            }

            log.info("[Redisson Lock] 락 획득 성공 - 재고 ID: {}, 스레드: {}",
                    id, Thread.currentThread().getName());

            // 트랜잭션 시작 (@Transactional)
            // 락 획득 후 DB 작업 수행
            Stock stock = stockRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다."));

            stock.decrease(quantity);
            stockRepository.saveAndFlush(stock);

            log.info("[Redisson Lock] 재고 감소 완료 - ID: {}, 남은 재고: {}, 스레드: {}",
                    id, stock.getQuantity(), Thread.currentThread().getName());

            // 트랜잭션 커밋 (@Transactional)

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Redisson Lock] 락 획득 중 인터럽트 발생 - 재고 ID: {}", id, e);
            throw new IllegalStateException("재고 감소 처리 중 오류가 발생했습니다.", e);

        } finally {
            // 락 해제
            // isHeldByCurrentThread(): 현재 스레드가 락을 소유하고 있는지 확인
            // 다른 스레드의 락을 해제하는 것을 방지
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[Redisson Lock] 락 해제 완료 - 재고 ID: {}, 스레드: {}",
                        id, Thread.currentThread().getName());
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
