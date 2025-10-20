package com.example.hightraffic.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 댓글 엔티티
 *
 * 계층형 댓글 구조 설계:
 *
 * 1. 2 Depth 방식 (댓글 + 대댓글):
 *    - depth 0: 댓글 (parentId = null)
 *    - depth 1: 대댓글 (parentId != null)
 *    - 비즈니스 로직에서 depth > 1 제한
 *
 * 2. 무한 Depth 방식 (계층형 구조):
 *    - depth 제한 없음
 *    - 재귀적으로 부모-자식 관계 형성
 *
 * 삭제 전략:
 * - 소프트 삭제 (Soft Delete) 적용
 * - 자식 댓글이 있는 경우: 내용만 삭제 표시 ("삭제된 댓글입니다")
 * - 자식 댓글이 없는 경우: 실제 삭제 가능
 *
 * 인덱스 전략:
 * - (postId, parentId, createdAt): 특정 게시글의 댓글 조회 최적화
 * - (postId, depth, createdAt): depth별 조회 최적화
 */
@Entity
@Table(
    name = "comments",
    indexes = {
        @Index(name = "idx_post_parent_created", columnList = "post_id, parent_id, created_at"),
        @Index(name = "idx_post_depth_created", columnList = "post_id, depth, created_at"),
        @Index(name = "idx_parent_id", columnList = "parent_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(nullable = false)
    private Integer depth;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Comment(Long postId, Long parentId, String content, String author, Integer depth) {
        this.postId = postId;
        this.parentId = parentId;
        this.content = content;
        this.author = author;
        this.depth = depth != null ? depth : 0;
        this.isDeleted = false;
    }

    /**
     * 댓글 수정
     */
    public void update(String content) {
        if (this.isDeleted) {
            throw new IllegalStateException("삭제된 댓글은 수정할 수 없습니다.");
        }
        this.content = content;
    }

    /**
     * 댓글 삭제 (소프트 삭제)
     */
    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.content = "삭제된 댓글입니다.";
    }

    /**
     * 최상위 댓글인지 확인
     */
    public boolean isRootComment() {
        return this.parentId == null;
    }

    /**
     * 특정 depth 제한 검증 (2 depth 방식용)
     */
    public static void validateMaxDepth(Integer depth, int maxDepth) {
        if (depth >= maxDepth) {
            throw new IllegalArgumentException(
                String.format("댓글 깊이는 최대 %d까지만 허용됩니다.", maxDepth)
            );
        }
    }

    /**
     * 부모 댓글로부터 자식 댓글 생성
     */
    public static Comment createChild(Comment parent, Long postId, String content, String author) {
        return Comment.builder()
                .postId(postId)
                .parentId(parent.getId())
                .content(content)
                .author(author)
                .depth(parent.getDepth() + 1)
                .build();
    }

    /**
     * 루트 댓글 생성
     */
    public static Comment createRoot(Long postId, String content, String author) {
        return Comment.builder()
                .postId(postId)
                .parentId(null)
                .content(content)
                .author(author)
                .depth(0)
                .build();
    }
}
