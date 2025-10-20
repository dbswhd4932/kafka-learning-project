package com.example.hightraffic.service;

import com.example.hightraffic.domain.Post;
import com.example.hightraffic.dto.*;
import com.example.hightraffic.exception.BusinessException;
import com.example.hightraffic.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    /**
     * 게시글 생성
     */
    @Transactional
    public PostResponse createPost(PostCreateRequest request) {
        Post post = request.toEntity();
        Post savedPost = postRepository.save(post);
        log.info("게시글 생성 완료: id={}, title={}", savedPost.getId(), savedPost.getTitle());
        return PostResponse.from(savedPost);
    }

    /**
     * 게시글 단건 조회
     */
    @Transactional
    public PostResponse getPost(Long id) {
        Post post = findPostById(id);
        post.increaseViewCount();
        log.debug("게시글 조회: id={}, viewCount={}", post.getId(), post.getViewCount());
        return PostResponse.from(post);
    }

    /**
     * 게시글 목록 조회 - 페이지 번호 방식 (Offset-based)
     *
     * 사용 사례:
     * - 일반적인 게시판 (페이지 번호로 이동)
     * - 검색 결과 페이지
     * - 관리자 페이지
     *
     * 성능 고려사항:
     * - OFFSET이 커질수록 성능 저하 (예: OFFSET 10000이면 10000개를 스캔 후 버림)
     * - COUNT(*) 쿼리가 추가로 실행되어 총 개수를 조회
     * - 데이터가 많을 경우 인덱스를 활용해도 느려질 수 있음
     */
    public PageResponse<PostListResponse> getPostsByPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);

        Page<PostListResponse> responsePage = postPage.map(PostListResponse::from);

        log.debug("페이지 조회: page={}, size={}, total={}", page, size, postPage.getTotalElements());
        return PageResponse.from(responsePage);
    }

    /**
     * 게시글 목록 조회 - 커서 방식 (Cursor-based) - 무한 스크롤
     *
     * 사용 사례:
     * - 모바일 앱의 무한 스크롤
     * - 소셜 미디어 피드
     * - 실시간 업데이트가 필요한 목록
     *
     * 성능 고려사항:
     * - 인덱스를 활용하여 빠른 조회 (WHERE id < ? ORDER BY id DESC)
     * - COUNT(*) 쿼리가 없어서 빠름
     * - 일관성 있는 결과 (데이터 추가/삭제 시에도 중복/누락 없음)
     *
     * @param cursor 마지막으로 조회한 게시글 ID (null이면 첫 페이지)
     * @param size 조회할 개수
     */
    public CursorPageResponse<PostListResponse> getPostsByCursor(Long cursor, int size) {
        // size + 1개를 조회하여 다음 페이지 존재 여부 확인
        Pageable pageable = PageRequest.of(0, size + 1);

        List<Post> posts;
        if (cursor == null) {
            // 첫 페이지: 커서가 없으면 최신 게시글부터 조회
            posts = postRepository.findAllByOrderByIdDesc(pageable);
        } else {
            // 다음 페이지: 커서 이후의 게시글 조회
            posts = postRepository.findPostsByCursor(cursor, pageable);
        }

        // 다음 페이지 존재 여부 확인
        boolean hasNext = posts.size() > size;

        // size개만큼만 반환 (size + 1개를 조회했으므로)
        List<PostListResponse> content = posts.stream()
                .limit(size)
                .map(PostListResponse::from)
                .collect(Collectors.toList());

        // 다음 커서는 마지막 게시글의 ID
        Long nextCursor = hasNext && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;

        log.debug("커서 조회: cursor={}, size={}, hasNext={}", cursor, size, hasNext);
        return CursorPageResponse.of(content, nextCursor, hasNext);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public PostResponse updatePost(Long id, PostUpdateRequest request) {
        Post post = findPostById(id);
        post.update(request.getTitle(), request.getContent());
        log.info("게시글 수정 완료: id={}", id);
        return PostResponse.from(post);
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void deletePost(Long id) {
        Post post = findPostById(id);
        postRepository.delete(post);
        log.info("게시글 삭제 완료: id={}", id);
    }

    /**
     * 좋아요 증가
     */
    @Transactional
    public PostResponse increaseLike(Long id) {
        Post post = findPostById(id);
        post.increaseLikeCount();
        log.debug("좋아요 증가: id={}, likeCount={}", post.getId(), post.getLikeCount());
        return PostResponse.from(post);
    }

    /**
     * 좋아요 감소
     */
    @Transactional
    public PostResponse decreaseLike(Long id) {
        Post post = findPostById(id);
        post.decreaseLikeCount();
        log.debug("좋아요 감소: id={}, likeCount={}", post.getId(), post.getLikeCount());
        return PostResponse.from(post);
    }

    /**
     * 조회수 상위 게시글 조회
     */
    public List<PostListResponse> getTopViewedPosts() {
        return postRepository.findTop10ByOrderByViewCountDesc()
                .stream()
                .map(PostListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 좋아요 상위 게시글 조회
     */
    public List<PostListResponse> getTopLikedPosts() {
        return postRepository.findTop10ByOrderByLikeCountDesc()
                .stream()
                .map(PostListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 게시글 조회 헬퍼 메서드
     */
    private Post findPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "게시글을 찾을 수 없습니다: id=" + id,
                        HttpStatus.NOT_FOUND
                ));
    }
}
