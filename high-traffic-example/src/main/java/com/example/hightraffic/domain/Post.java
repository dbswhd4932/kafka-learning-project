package com.example.hightraffic.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 엔티티
 *
 * Primary Key 전략:
 * - GenerationType.IDENTITY: MySQL의 AUTO_INCREMENT 사용
 * - 장점: 간단하고 직관적, 데이터베이스에서 자동으로 ID 생성
 * - 단점: batch insert 최적화가 어려움, ID를 얻기 위해 즉시 INSERT 실행 필요
 *
 * 대안:
 * - GenerationType.SEQUENCE: Oracle, PostgreSQL에 적합
 * - GenerationType.TABLE: 모든 DB에서 사용 가능하지만 성능이 좋지 않음
 * - UUID: 분산 시스템에서 유용하지만 인덱스 성능이 떨어질 수 있음
 */
@Entity
@Table(
    name = "posts",
    indexes = {
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_title", columnList = "title"),
        @Index(name = "idx_author", columnList = "author")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(nullable = false)
    private Long viewCount;

    @Column(nullable = false)
    private Long likeCount;

    @Builder
    public Post(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.viewCount = 0L;
        this.likeCount = 0L;
    }

    /**
     * 게시글 수정
     */
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    /**
     * 조회수 증가
     */
    public void increaseViewCount() {
        this.viewCount++;
    }

    /**
     * 좋아요 증가
     */
    public void increaseLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 감소
     */
    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}
