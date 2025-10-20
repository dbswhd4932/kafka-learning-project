package com.example.hightraffic.service;

import com.example.hightraffic.domain.Comment;
import com.example.hightraffic.dto.*;
import com.example.hightraffic.exception.BusinessException;
import com.example.hightraffic.repository.CommentRepository;
import com.example.hightraffic.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    private static final int MAX_DEPTH_FOR_TWO_LEVEL = 2; // 2 depth 방식의 최대 깊이

    /**
     * 댓글 생성 (2 depth 방식)
     * - depth 0: 댓글 (parentId = null)
     * - depth 1: 대댓글 (parentId != null)
     * - depth 2 이상은 허용하지 않음
     */
    @Transactional
    public CommentResponse createCommentTwoDepth(CommentCreateRequest request) {
        // 게시글 존재 확인
        validatePostExists(request.getPostId());

        Comment comment;

        if (request.getParentId() == null) {
            // 루트 댓글 생성 (depth 0)
            comment = request.toRootEntity();
            log.info("루트 댓글 생성: postId={}", request.getPostId());
        } else {
            // 대댓글 생성 (depth 1)
            Comment parent = findCommentById(request.getParentId());

            // 2 depth 검증: 부모가 이미 대댓글이면 안됨
            if (parent.getDepth() >= 1) {
                throw new BusinessException("대댓글에는 답글을 달 수 없습니다. (최대 2 depth)", HttpStatus.BAD_REQUEST);
            }

            comment = request.toChildEntity(parent);
            log.info("대댓글 생성: postId={}, parentId={}", request.getPostId(), request.getParentId());
        }

        Comment savedComment = commentRepository.save(comment);
        return CommentResponse.from(savedComment);
    }

    /**
     * 댓글 생성 (무한 depth 방식)
     * - depth 제한 없음
     * - 계층형 구조로 무한히 중첩 가능
     */
    @Transactional
    public CommentResponse createCommentInfiniteDepth(CommentCreateRequest request) {
        // 게시글 존재 확인
        validatePostExists(request.getPostId());

        Comment comment;

        if (request.getParentId() == null) {
            // 루트 댓글 생성
            comment = request.toRootEntity();
            log.info("루트 댓글 생성: postId={}", request.getPostId());
        } else {
            // 자식 댓글 생성
            Comment parent = findCommentById(request.getParentId());
            comment = request.toChildEntity(parent);
            log.info("자식 댓글 생성: postId={}, parentId={}, depth={}",
                    request.getPostId(), request.getParentId(), comment.getDepth());
        }

        Comment savedComment = commentRepository.save(comment);
        return CommentResponse.from(savedComment);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommentResponse updateComment(Long id, CommentUpdateRequest request) {
        Comment comment = findCommentById(id);
        comment.update(request.getContent());
        log.info("댓글 수정: id={}", id);
        return CommentResponse.from(comment);
    }

    /**
     * 댓글 삭제
     * - 자식 댓글이 있으면 소프트 삭제 (내용만 "삭제된 댓글입니다"로 변경)
     * - 자식 댓글이 없으면 실제 삭제 가능
     */
    @Transactional
    public void deleteComment(Long id) {
        Comment comment = findCommentById(id);

        long childCount = commentRepository.countByParentId(id);

        if (childCount > 0) {
            // 자식 댓글이 있으면 소프트 삭제
            comment.delete();
            log.info("댓글 소프트 삭제 (자식 댓글 존재): id={}, childCount={}", id, childCount);
        } else {
            // 자식 댓글이 없으면 실제 삭제
            commentRepository.delete(comment);
            log.info("댓글 실제 삭제: id={}", id);
        }
    }

    /**
     * 2 Depth 방식 댓글 목록 조회
     * - 댓글 + 대댓글 구조
     * - 댓글별로 대댓글 목록이 포함됨
     *
     * 조회 전략:
     * 1. 모든 댓글을 한 번에 조회 (N+1 방지)
     * 2. 메모리에서 depth별로 그룹화
     * 3. 댓글에 대댓글을 매핑
     */
    public List<CommentWithRepliesResponse> getCommentsTwoDepth(Long postId) {
        // 게시글의 모든 댓글을 한 번에 조회
        List<Comment> allComments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        // depth별로 분리
        Map<Integer, List<Comment>> commentsByDepth = allComments.stream()
                .collect(Collectors.groupingBy(Comment::getDepth));

        // 루트 댓글들 (depth 0)
        List<Comment> rootComments = commentsByDepth.getOrDefault(0, new ArrayList<>());

        // 대댓글들 (depth 1)
        List<Comment> replies = commentsByDepth.getOrDefault(1, new ArrayList<>());

        // parentId별로 대댓글 그룹화
        Map<Long, List<Comment>> repliesByParentId = replies.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 응답 생성
        return rootComments.stream()
                .map(comment -> {
                    CommentWithRepliesResponse response = CommentWithRepliesResponse.from(comment);
                    // 해당 댓글의 대댓글들 추가
                    List<Comment> commentReplies = repliesByParentId.getOrDefault(comment.getId(), new ArrayList<>());
                    commentReplies.forEach(response::addReply);
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * 무한 Depth 방식 댓글 목록 조회
     * - 계층형 트리 구조
     * - 재귀적으로 자식 댓글을 포함
     *
     * 조회 전략:
     * 1. 모든 댓글을 한 번에 조회 (N+1 방지)
     * 2. 메모리에서 계층 구조 생성
     * 3. 재귀적으로 자식 댓글 연결
     */
    public List<CommentTreeResponse> getCommentsInfiniteDepth(Long postId) {
        // 게시글의 모든 댓글을 한 번에 조회
        List<Comment> allComments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        // id를 키로 하는 맵 생성 (빠른 조회를 위해)
        Map<Long, CommentTreeResponse> commentMap = allComments.stream()
                .collect(Collectors.toMap(
                        Comment::getId,
                        CommentTreeResponse::from,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        // 루트 댓글 목록
        List<CommentTreeResponse> rootComments = new ArrayList<>();

        // 계층 구조 생성
        for (Comment comment : allComments) {
            CommentTreeResponse response = commentMap.get(comment.getId());

            if (comment.getParentId() == null) {
                // 루트 댓글
                rootComments.add(response);
            } else {
                // 자식 댓글: 부모에 추가
                CommentTreeResponse parent = commentMap.get(comment.getParentId());
                if (parent != null) {
                    parent.addChild(response);
                }
            }
        }

        return rootComments;
    }

    /**
     * 특정 댓글 조회
     */
    public CommentResponse getComment(Long id) {
        Comment comment = findCommentById(id);
        return CommentResponse.from(comment);
    }

    /**
     * 게시글의 댓글 개수 조회
     */
    public long getCommentCount(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    /**
     * 게시글의 삭제되지 않은 댓글 개수 조회
     */
    public long getActiveCommentCount(Long postId) {
        return commentRepository.countByPostIdAndIsDeletedFalse(postId);
    }

    /**
     * 댓글 조회 헬퍼 메서드
     */
    private Comment findCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "댓글을 찾을 수 없습니다: id=" + id,
                        HttpStatus.NOT_FOUND
                ));
    }

    /**
     * 게시글 존재 확인
     */
    private void validatePostExists(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(
                    "게시글을 찾을 수 없습니다: id=" + postId,
                    HttpStatus.NOT_FOUND
            );
        }
    }
}
