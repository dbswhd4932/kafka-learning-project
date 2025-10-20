package com.example.hightraffic.repository;

import com.example.hightraffic.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 페이지 번호 기반 조회 (Offset-based Pagination)
     * - 장점: 특정 페이지로 바로 이동 가능, 전체 페이지 수 확인 가능
     * - 단점: 데이터가 많아질수록 성능 저하 (OFFSET이 크면 느려짐)
     *         데이터 추가/삭제 시 중복 또는 누락 가능
     */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 커서 기반 조회 (Cursor-based Pagination) - 무한 스크롤용
     * - 장점: 일관성 있는 결과, 성능이 좋음 (인덱스 활용)
     * - 단점: 특정 페이지로 이동 불가, 전체 개수 파악 어려움
     *
     * WHERE id < :cursor: 마지막으로 조회한 게시글 ID보다 작은 것만 조회
     * ORDER BY id DESC: 최신 게시글부터 조회
     */
    @Query("SELECT p FROM Post p WHERE p.id < :cursor ORDER BY p.id DESC")
    List<Post> findPostsByCursor(@Param("cursor") Long cursor, Pageable pageable);

    /**
     * 첫 페이지 조회 (커서가 없을 때)
     */
    List<Post> findAllByOrderByIdDesc(Pageable pageable);

    /**
     * 제목으로 검색 (페이지 번호 기반)
     */
    Page<Post> findByTitleContainingOrderByCreatedAtDesc(String title, Pageable pageable);

    /**
     * 작성자로 검색 (페이지 번호 기반)
     */
    Page<Post> findByAuthorOrderByCreatedAtDesc(String author, Pageable pageable);

    /**
     * 조회수 상위 게시글 조회
     */
    List<Post> findTop10ByOrderByViewCountDesc();

    /**
     * 좋아요 상위 게시글 조회
     */
    List<Post> findTop10ByOrderByLikeCountDesc();
}
