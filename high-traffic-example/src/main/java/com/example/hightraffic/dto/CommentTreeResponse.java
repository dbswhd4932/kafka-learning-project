package com.example.hightraffic.dto;

import com.example.hightraffic.domain.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 무한 Depth 댓글 응답 (계층형 트리 구조)
 *
 * 구조:
 * - 댓글 (depth 0)
 *   - 대댓글 (depth 1)
 *     - 대대댓글 (depth 2)
 *       - ...
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentTreeResponse {

    private Long id;
    private Long postId;
    private Long parentId;
    private String content;
    private String author;
    private Integer depth;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<CommentTreeResponse> children = new ArrayList<>();

    public static CommentTreeResponse from(Comment comment) {
        return CommentTreeResponse.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .author(comment.getAuthor())
                .depth(comment.getDepth())
                .isDeleted(comment.getIsDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .children(new ArrayList<>())
                .build();
    }

    public void addChild(CommentTreeResponse child) {
        this.children.add(child);
    }

    /**
     * 자식 댓글 개수 (재귀적으로 계산)
     */
    public int getTotalChildCount() {
        int count = this.children.size();
        for (CommentTreeResponse child : this.children) {
            count += child.getTotalChildCount();
        }
        return count;
    }
}
