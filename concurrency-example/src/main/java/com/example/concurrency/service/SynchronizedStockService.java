package com.example.concurrency.service;

import com.example.concurrency.domain.Stock;
import com.example.concurrency.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Synchronized를 사용한 재고 관리 서비스
 *
 * [동작 원리]
 * synchronized 키워드를 사용하여 메서드 레벨에서 동시성을 제어합니다.
 * Java의 모니터 락(Monitor Lock)을 사용하여 한 번에 하나의 스레드만 메서드에 접근할 수 있도록 합니다.
 *
 * [장점]
 * 1. 구현이 매우 간단함 - synchronized 키워드만 추가하면 됨
 * 2. 단일 서버 환경에서는 확실하게 동작
 * 3. 추가 라이브러리나 인프라 불필요
 * 4. 코드 이해가 쉬움
 *
 * [단점]
 * 1. 단일 JVM 내에서만 동작 (분산 환경에서는 동작하지 않음)
 * 2. 성능 저하 - 모든 요청이 순차적으로 처리됨
 * 3. 재고 ID가 달라도 모두 대기해야 함 (메서드 전체가 락)
 *
 * [사용 불가능한 환경]
 * ❌ Kubernetes Pod가 여러 개인 경우 (각 Pod는 독립적인 JVM)
 * ❌ Auto Scaling 사용 시 (인스턴스가 늘어나면 동시성 문제 발생)
 * ❌ 로드 밸런서 뒤에 여러 서버가 있는 경우
 * ❌ MSA 환경에서 여러 서비스가 같은 재고에 접근하는 경우
 *
 * [사용 가능한 환경]
 * ✅ 단일 서버 환경 (로컬 개발, 소규모 서비스)
 * ✅ 프로토타입 또는 MVP 단계
 * ✅ 트래픽이 매우 적은 초기 단계
 * ✅ 레거시 시스템 (단일 서버로만 운영)
 *
 * [트랜잭션 처리]
 * - @Transactional 없이 synchronized만 사용하면 동작은 하지만 원자성이 보장되지 않음
 * - 조회와 저장이 별도의 트랜잭션으로 실행되어 중간에 예외 발생 시 롤백 불가
 * - 이 서비스에서는 @Transactional(readOnly = false)를 명시적으로 사용하여 안전성 확보
 *
 * [왜 @Transactional과 synchronized를 함께 사용해도 되는가?]
 * - synchronized로 메서드 전체를 동기화하므로 트랜잭션 시작부터 커밋까지 순차 처리됨
 * - AOP 프록시 이슈는 synchronized 범위가 트랜잭션 범위보다 작을 때 발생
 * - 여기서는 메서드 레벨 synchronized이므로 문제없음
 *
 * [주의사항]
 * - 단, 이 방법은 단일 JVM에서만 동작합니다
 * - 여러 Pod/서버 환경에서는 Redis 분산 락을 사용해야 합니다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SynchronizedStockService {

    private final StockRepository stockRepository;

    /**
     * synchronized를 사용한 재고 감소
     *
     * [동작 과정]
     * 1. Thread A가 synchronized 락 획득
     * 2. AOP 프록시가 트랜잭션 시작
     * 3. Thread A가 재고 조회 및 감소
     * 4. Thread A가 DB 저장
     * 5. AOP 프록시가 트랜잭션 커밋
     * 6. Thread A가 synchronized 락 해제
     * 7. Thread B가 synchronized 락 획득 (Thread A 완료 후)
     * 8. Thread B의 트랜잭션 시작 및 처리
     *
     * [왜 @Transactional + synchronized가 여기서는 안전한가?]
     * - synchronized가 메서드 레벨에 적용되어 있음
     * - 트랜잭션 시작(프록시) → synchronized 메서드 실행 → 트랜잭션 커밋(프록시)
     * - 하지만 synchronized가 먼저 락을 걸어서 전체 흐름이 순차 처리됨
     * - 따라서 트랜잭션 커밋 전에 다른 스레드가 진입할 수 없음
     *
     * [원자성 보장]
     * - @Transactional로 조회-수정-저장이 하나의 트랜잭션으로 묶임
     * - 중간에 예외 발생 시 자동 롤백
     * - synchronized로 동시 접근 차단
     *
     * [특징]
     * - 메서드 전체에 synchronized 적용
     * - 재고 ID가 달라도 순차 처리됨 (성능 저하 요인)
     * - 단일 JVM 내에서는 100% 동시성 문제 해결
     *
     * [분산 환경에서 동작하지 않는 이유]
     * ```
     * [Pod A - JVM 1] synchronized 락 획득 → 재고 감소
     * [Pod B - JVM 2] 별도의 JVM이므로 독립적으로 락 획득 → 재고 감소
     * → Lost Update 발생!
     * ```
     *
     * @param id 재고 ID
     * @param quantity 감소할 수량
     * @throws IllegalArgumentException 재고를 찾을 수 없는 경우
     * @throws IllegalStateException 재고가 부족한 경우
     */
    @Transactional
    public synchronized void decrease(Long id, Long quantity) {
        log.info("[Synchronized] 재고 감소 시작 - ID: {}, 수량: {}, 스레드: {}",
                id, quantity, Thread.currentThread().getName());

        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다."));

        stock.decrease(quantity);
        // @Transactional이 있으므로 save()만 해도 트랜잭션 커밋 시 자동 반영됨
        // 하지만 명시적으로 saveAndFlush()를 사용하여 즉시 DB 반영
        stockRepository.saveAndFlush(stock);

        log.info("[Synchronized] 재고 감소 완료 - ID: {}, 남은 재고: {}, 스레드: {}",
                id, stock.getQuantity(), Thread.currentThread().getName());
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
