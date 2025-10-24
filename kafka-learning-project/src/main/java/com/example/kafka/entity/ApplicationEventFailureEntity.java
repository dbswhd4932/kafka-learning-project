package com.example.kafka.entity;

import com.example.kafka.enums.ApplicationEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 이벤트 발행 실패 이력 Entity
 * - Kafka 전송 실패 시 저장
 * - 재처리를 위한 데이터 보관
 */
@Entity
@Table(name = "application_event_failure")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationEventFailureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 실패한 이벤트의 Payload (JSON)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    /**
     * 에러 메시지
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 이벤트 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApplicationEventType eventType;

    /**
     * 재처리 여부
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean retried = false;

    /**
     * 생성 시간
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 정적 팩토리 메서드
     */
    public static ApplicationEventFailureEntity toEntity(String payload, String errorMessage, ApplicationEventType eventType) {
        return ApplicationEventFailureEntity.builder()
                .payload(payload)
                .errorMessage(errorMessage)
                .eventType(eventType)
                .retried(false)
                .build();
    }

    /**
     * 재처리 완료 표시
     */
    public void markAsRetried() {
        this.retried = true;
    }
}
