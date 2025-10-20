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
 * 2 Depth 댓글 응답 (댓글 + 대댓글)
 *
 * 구조:
 * - 댓글 (depth 0)
 *   - 대댓글 목록 (depth 1)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentWithRepliesResponse {

    private Long id;
    private Long postId;
    private String content;
    private String author;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<ReplyResponse> replies = new ArrayList<>();

    public static CommentWithRepliesResponse from(Comment comment) {
        return CommentWithRepliesResponse.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .content(comment.getContent())
                .author(comment.getAuthor())
                .isDeleted(comment.getIsDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(new ArrayList<>())
                .build();
    }

    public void addReply(Comment reply) {
        this.replies.add(ReplyResponse.from(reply));
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplyResponse {
        private Long id;
        private Long parentId;
        private String content;
        private String author;
        private Boolean isDeleted;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ReplyResponse from(Comment comment) {
            return ReplyResponse.builder()
                    .id(comment.getId())
                    .parentId(comment.getParentId())
                    .content(comment.getContent())
                    .author(comment.getAuthor())
                    .isDeleted(comment.getIsDeleted())
                    .createdAt(comment.getCreatedAt())
                    .updatedAt(comment.getUpdatedAt())
                    .build();
        }
    }
}
