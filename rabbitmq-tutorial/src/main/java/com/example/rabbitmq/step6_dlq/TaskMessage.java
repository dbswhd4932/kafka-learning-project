package com.example.rabbitmq.step6_dlq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 작업 메시지 DTO
 *
 * Dead Letter Queue와 Retry 재처리를 시연하기 위한 메시지 구조
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskMessage {

    /**
     * 작업 ID
     */
    private String taskId;

    /**
     * 작업 내용
     */
    private String taskContent;

    /**
     * 재시도 횟수
     */
    private Integer retryCount;

    /**
     * 실패 시뮬레이션 여부
     * (테스트용: true면 처리 중 예외 발생)
     */
    private Boolean shouldFail;

    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 간편 생성자
     */
    public TaskMessage(String taskContent, Boolean shouldFail) {
        this.taskId = java.util.UUID.randomUUID().toString();
        this.taskContent = taskContent;
        this.retryCount = 0;
        this.shouldFail = shouldFail;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 재시도 횟수 증가
     */
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
}
