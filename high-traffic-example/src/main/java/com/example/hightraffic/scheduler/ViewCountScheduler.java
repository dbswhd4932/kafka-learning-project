package com.example.hightraffic.scheduler;

import com.example.hightraffic.service.ViewCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 조회수 DB 동기화 스케줄러
 *
 * Redis에 저장된 조회수를 주기적으로 DB에 동기화합니다.
 *
 * 스케줄 전략:
 * - 5분마다 실행 (fixedDelay)
 * - 이전 작업이 완료된 후 5분 대기
 * - DB 부하 분산 효과
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountScheduler {

    private final ViewCountService viewCountService;

    /**
     * Redis → DB 조회수 동기화
     *
     * fixedDelay: 이전 작업 완료 후 5분 대기
     * - 장점: DB 부하 예측 가능, 안정적
     * - 단점: 동기화 주기가 불규칙할 수 있음
     *
     * 대안:
     * - fixedRate: 5분마다 정확히 실행 (이전 작업 완료 여부 무관)
     * - cron: "0 *5 * * * *" (매 5분 정각)
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000) // 5분 = 300,000ms
    public void syncViewCountToDatabase() {
        try {
            log.info("=== 조회수 DB 동기화 스케줄러 시작 ===");
            long startTime = System.currentTimeMillis();

            viewCountService.syncToDatabase();

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("=== 조회수 DB 동기화 스케줄러 완료 (소요시간: {}ms) ===", elapsedTime);

        } catch (Exception e) {
            log.error("조회수 DB 동기화 스케줄러 실행 중 에러 발생", e);
            // 예외를 삼켜서 스케줄러가 중단되지 않도록 함
        }
    }
}
