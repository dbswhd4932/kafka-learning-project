package com.example.hightraffic.controller;

import com.example.hightraffic.dto.*;
import com.example.hightraffic.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 댓글 API 컨트롤러
 *
 * 두 가지 방식의 댓글 시스템 제공:
 * 1. 2 Depth 방식 (/api/comments/two-depth/*)
 *    - 댓글 + 대댓글 (최대 2단계)
 *    - 일반적인 게시판에 적합
 *
 * 2. 무한 Depth 방식 (/api/comments/infinite-depth/*)
 *    - 계층형 트리 구조 (무제한)
 *    - Reddit, HackerNews 스타일
 */
@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ==================== 2 Depth 방식 API ====================

    /**
     * 댓글 생성 (2 Depth 방식)
     *
     * POST /api/comments/two-depth
     *
     * Request Body:
     * {
     *   "postId": 1,
     *   "parentId": null,  // null이면 댓글, 값이 있으면 대댓글
     *   "content": "댓글 내용",
     *   "author": "작성자"
     * }
     */
    @PostMapping("/two-depth")
    public ResponseEntity<CommentResponse> createCommentTwoDepth(@Valid @RequestBody CommentCreateRequest request) {
        log.info("댓글 생성 요청 (2 depth): postId={}, parentId={}", request.getPostId(), request.getParentId());
        CommentResponse response = commentService.createCommentTwoDepth(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 댓글 목록 조회 (2 Depth 방식)
     *
     * GET /api/comments/two-depth?postId=1
     *
     * Response:
     * [
     *   {
     *     "id": 1,
     *     "content": "댓글",
     *     "replies": [
     *       {
     *         "id": 2,
     *         "content": "대댓글"
     *       }
     *     ]
     *   }
     * ]
     */
    @GetMapping("/two-depth")
    public ResponseEntity<List<CommentWithRepliesResponse>> getCommentsTwoDepth(@RequestParam Long postId) {
        log.info("댓글 목록 조회 요청 (2 depth): postId={}", postId);
        List<CommentWithRepliesResponse> response = commentService.getCommentsTwoDepth(postId);
        return ResponseEntity.ok(response);
    }

    // ==================== 무한 Depth 방식 API ====================

    /**
     * 댓글 생성 (무한 Depth 방식)
     *
     * POST /api/comments/infinite-depth
     *
     * Request Body:
     * {
     *   "postId": 1,
     *   "parentId": null,  // null이면 루트 댓글, 값이 있으면 자식 댓글
     *   "content": "댓글 내용",
     *   "author": "작성자"
     * }
     */
    @PostMapping("/infinite-depth")
    public ResponseEntity<CommentResponse> createCommentInfiniteDepth(@Valid @RequestBody CommentCreateRequest request) {
        log.info("댓글 생성 요청 (무한 depth): postId={}, parentId={}", request.getPostId(), request.getParentId());
        CommentResponse response = commentService.createCommentInfiniteDepth(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 댓글 목록 조회 (무한 Depth 방식)
     *
     * GET /api/comments/infinite-depth?postId=1
     *
     * Response:
     * [
     *   {
     *     "id": 1,
     *     "content": "댓글",
     *     "depth": 0,
     *     "children": [
     *       {
     *         "id": 2,
     *         "content": "대댓글",
     *         "depth": 1,
     *         "children": [
     *           {
     *             "id": 3,
     *             "content": "대대댓글",
     *             "depth": 2,
     *             "children": []
     *           }
     *         ]
     *       }
     *     ]
     *   }
     * ]
     */
    @GetMapping("/infinite-depth")
    public ResponseEntity<List<CommentTreeResponse>> getCommentsInfiniteDepth(@RequestParam Long postId) {
        log.info("댓글 목록 조회 요청 (무한 depth): postId={}", postId);
        List<CommentTreeResponse> response = commentService.getCommentsInfiniteDepth(postId);
        return ResponseEntity.ok(response);
    }

    // ==================== 공통 API ====================

    /**
     * 댓글 단건 조회
     *
     * GET /api/comments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> getComment(@PathVariable Long id) {
        log.info("댓글 조회 요청: id={}", id);
        CommentResponse response = commentService.getComment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 수정
     *
     * PUT /api/comments/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        log.info("댓글 수정 요청: id={}", id);
        CommentResponse response = commentService.updateComment(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 댓글 삭제
     *
     * DELETE /api/comments/{id}
     *
     * 삭제 정책:
     * - 자식 댓글이 있으면: 소프트 삭제 (내용만 "삭제된 댓글입니다"로 변경)
     * - 자식 댓글이 없으면: 실제 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        log.info("댓글 삭제 요청: id={}", id);
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글의 댓글 개수 조회
     *
     * GET /api/comments/count?postId=1
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getCommentCount(@RequestParam Long postId) {
        log.info("댓글 개수 조회 요청: postId={}", postId);
        long count = commentService.getCommentCount(postId);
        return ResponseEntity.ok(count);
    }

    /**
     * 게시글의 활성 댓글 개수 조회 (삭제되지 않은 댓글)
     *
     * GET /api/comments/count/active?postId=1
     */
    @GetMapping("/count/active")
    public ResponseEntity<Long> getActiveCommentCount(@RequestParam Long postId) {
        log.info("활성 댓글 개수 조회 요청: postId={}", postId);
        long count = commentService.getActiveCommentCount(postId);
        return ResponseEntity.ok(count);
    }
}
