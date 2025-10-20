package com.example.hightraffic.repository;

import com.example.hightraffic.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 게시글의 모든 댓글 조회 (생성일시 순)
     * - 2 depth, 무한 depth 모두 사용 가능
     */
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    /**
     * 특정 게시글의 루트 댓글만 조회 (parentId가 null인 댓글)
     * - 2 depth 방식에서 사용
     * - 무한 depth 방식에서도 최상위 댓글 조회에 사용
     */
    List<Comment> findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(Long postId);

    /**
     * 특정 부모 댓글의 자식 댓글들 조회
     * - 2 depth 방식: 대댓글 조회
     * - 무한 depth 방식: 직계 자식 조회
     */
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);

    /**
     * 특정 게시글의 특정 depth 댓글만 조회
     * - 2 depth 방식에서 depth별로 조회할 때 사용
     */
    List<Comment> findByPostIdAndDepthOrderByCreatedAtAsc(Long postId, Integer depth);

    /**
     * 특정 댓글의 자식 댓글 개수 조회
     * - 삭제 여부 판단에 사용 (자식이 있으면 소프트 삭제, 없으면 하드 삭제)
     */
    long countByParentId(Long parentId);

    /**
     * 특정 게시글의 댓글 개수 조회
     */
    long countByPostId(Long postId);

    /**
     * 특정 게시글의 삭제되지 않은 댓글 개수 조회
     */
    long countByPostIdAndIsDeletedFalse(Long postId);

    /**
     * 배치 조회 최적화: 특정 게시글의 모든 댓글을 한 번에 조회
     * - N+1 문제 방지
     */
    @Query("SELECT c FROM Comment c WHERE c.postId = :postId ORDER BY c.createdAt ASC")
    List<Comment> findAllByPostIdWithBatch(@Param("postId") Long postId);

    /**
     * 특정 depth 이하의 댓글만 조회 (무한 depth 제한용)
     */
    @Query("SELECT c FROM Comment c WHERE c.postId = :postId AND c.depth <= :maxDepth ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdAndMaxDepth(@Param("postId") Long postId, @Param("maxDepth") Integer maxDepth);

    /**
     * 작성자별 댓글 조회
     */
    List<Comment> findByAuthorOrderByCreatedAtDesc(String author);

    /**
     * 삭제되지 않은 댓글만 조회
     */
    List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);
}
