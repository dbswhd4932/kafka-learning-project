package com.example.hightraffic.controller;

import com.example.hightraffic.dto.*;
import com.example.hightraffic.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 게시글 API 컨트롤러
 *
 * RESTful API 설계:
 * - GET /api/posts: 게시글 목록 조회
 * - GET /api/posts/{id}: 게시글 상세 조회
 * - POST /api/posts: 게시글 생성
 * - PUT /api/posts/{id}: 게시글 수정
 * - DELETE /api/posts/{id}: 게시글 삭제
 */
@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 게시글 생성
     *
     * POST /api/posts
     */
    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody PostCreateRequest request) {
        log.info("게시글 생성 요청: title={}", request.getTitle());
        PostResponse response = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 게시글 상세 조회
     *
     * GET /api/posts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        log.info("게시글 조회 요청: id={}", id);
        PostResponse response = postService.getPost(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 목록 조회 - 페이지 번호 방식
     *
     * GET /api/posts?page=0&size=10
     *
     * 페이지 번호 기반 페이징:
     * - page: 페이지 번호 (0부터 시작)
     * - size: 페이지당 게시글 수
     *
     * 응답 예시:
     * {
     *   "content": [...],
     *   "pageNumber": 0,
     *   "pageSize": 10,
     *   "totalElements": 100,
     *   "totalPages": 10,
     *   "first": true,
     *   "last": false,
     *   "hasNext": true,
     *   "hasPrevious": false
     * }
     */
    @GetMapping
    public ResponseEntity<PageResponse<PostListResponse>> getPostsByPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("게시글 목록 조회 요청 (페이지): page={}, size={}", page, size);
        PageResponse<PostListResponse> response = postService.getPostsByPage(page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 목록 조회 - 커서 방식 (무한 스크롤)
     *
     * GET /api/posts/cursor?cursor=100&size=10
     *
     * 커서 기반 페이징:
     * - cursor: 마지막으로 조회한 게시글 ID (첫 요청 시 null)
     * - size: 조회할 게시글 수
     *
     * 응답 예시:
     * {
     *   "content": [...],
     *   "nextCursor": 90,
     *   "hasNext": true,
     *   "size": 10
     * }
     *
     * 클라이언트 사용 예시:
     * 1. 첫 요청: GET /api/posts/cursor?size=10
     * 2. 다음 요청: GET /api/posts/cursor?cursor=90&size=10
     * 3. 다음 요청: GET /api/posts/cursor?cursor=80&size=10
     */
    @GetMapping("/cursor")
    public ResponseEntity<CursorPageResponse<PostListResponse>> getPostsByCursor(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("게시글 목록 조회 요청 (커서): cursor={}, size={}", cursor, size);
        CursorPageResponse<PostListResponse> response = postService.getPostsByCursor(cursor, size);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 수정
     *
     * PUT /api/posts/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        log.info("게시글 수정 요청: id={}", id);
        PostResponse response = postService.updatePost(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 삭제
     *
     * DELETE /api/posts/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        log.info("게시글 삭제 요청: id={}", id);
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 좋아요 증가
     *
     * POST /api/posts/{id}/like
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<PostResponse> increaseLike(@PathVariable Long id) {
        log.info("좋아요 증가 요청: id={}", id);
        PostResponse response = postService.increaseLike(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 좋아요 감소
     *
     * DELETE /api/posts/{id}/like
     */
    @DeleteMapping("/{id}/like")
    public ResponseEntity<PostResponse> decreaseLike(@PathVariable Long id) {
        log.info("좋아요 감소 요청: id={}", id);
        PostResponse response = postService.decreaseLike(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 조회수 상위 게시글 조회
     *
     * GET /api/posts/top/viewed
     */
    @GetMapping("/top/viewed")
    public ResponseEntity<List<PostListResponse>> getTopViewedPosts() {
        log.info("조회수 상위 게시글 조회 요청");
        List<PostListResponse> response = postService.getTopViewedPosts();
        return ResponseEntity.ok(response);
    }

    /**
     * 좋아요 상위 게시글 조회
     *
     * GET /api/posts/top/liked
     */
    @GetMapping("/top/liked")
    public ResponseEntity<List<PostListResponse>> getTopLikedPosts() {
        log.info("좋아요 상위 게시글 조회 요청");
        List<PostListResponse> response = postService.getTopLikedPosts();
        return ResponseEntity.ok(response);
    }
}
