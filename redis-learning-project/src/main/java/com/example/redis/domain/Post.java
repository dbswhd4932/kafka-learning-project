package com.example.redis.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 게시글 엔티티
 * - 조회수 동시성 테스트를 위한 엔티티
 */
@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Post implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String author;

    /**
     * DB에 저장된 조회수
     * - Redis와 주기적으로 동기화
     */
    @Column(nullable = false)
    private Long viewCount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (viewCount == null) {
            viewCount = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * DB 조회수 증가 (동기화 시 사용)
     */
    public void increaseViewCount(Long count) {
        this.viewCount += count;
    }

    /**
     * DB 조회수 설정 (동기화 시 사용)
     */
    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }
}
