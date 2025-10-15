package com.example.async.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 성능 테스트를 위한 서비스
 */
@Service
@Slf4j
public class PerformanceTestService {

    /**
     * 동기 방식 작업 (1초 소요)
     */
    public String syncTask(String taskName) {
        log.info("[{}] Starting sync task: {}", Thread.currentThread().getName(), taskName);
        try {
            Thread.sleep(1000); // 1초 작업 시뮬레이션
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[{}] Completed sync task: {}", Thread.currentThread().getName(), taskName);
        return "Completed: " + taskName;
    }

    /**
     * 비동기 방식 작업 (1초 소요)
     */
    @Async("taskExecutor")
    public CompletableFuture<String> asyncTask(String taskName) {
        log.info("[{}] Starting async task: {}", Thread.currentThread().getName(), taskName);
        try {
            Thread.sleep(1000); // 1초 작업 시뮬레이션
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[{}] Completed async task: {}", Thread.currentThread().getName(), taskName);
        return CompletableFuture.completedFuture("Completed: " + taskName);
    }
}
