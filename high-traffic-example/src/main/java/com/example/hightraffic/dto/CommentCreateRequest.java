package com.example.hightraffic.dto;

import com.example.hightraffic.domain.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateRequest {

    @NotNull(message = "게시글 ID는 필수입니다")
    private Long postId;

    private Long parentId;

    @NotBlank(message = "댓글 내용은 필수입니다")
    private String content;

    @NotBlank(message = "작성자는 필수입니다")
    private String author;

    /**
     * 루트 댓글로 변환
     */
    public Comment toRootEntity() {
        return Comment.createRoot(postId, content, author);
    }

    /**
     * 자식 댓글로 변환
     */
    public Comment toChildEntity(Comment parent) {
        return Comment.createChild(parent, postId, content, author);
    }
}
