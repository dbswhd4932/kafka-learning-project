package com.example.redis.repository;

import com.example.redis.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 게시글 리포지토리
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
}
