package com.example.hightraffic.controller;

import com.example.hightraffic.dto.CommentWithRepliesResponse;
import com.example.hightraffic.dto.PostResponse;
import com.example.hightraffic.service.CommentService;
import com.example.hightraffic.service.PostService;
import com.example.hightraffic.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Thymeleaf 뷰 컨트롤러
 */
@Controller
@RequiredArgsConstructor
public class ViewController {

    private final PostService postService;
    private final CommentService commentService;

    /**
     * 홈 화면 (게시글 목록으로 리다이렉트)
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/posts";
    }

    /**
     * 게시글 목록 화면
     */
    @GetMapping("/posts")
    public String listPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        var pageResponse = postService.getPostsByPage(page, size);
        model.addAttribute("posts", pageResponse);
        model.addAttribute("currentPage", page);
        return "posts/list";
    }

    /**
     * 게시글 상세 화면 (댓글 포함)
     *
     * 조회수 증가 로직:
     * 1. 클라이언트 IP 추출
     * 2. Redis 기반 조회수 증가 (5초 중복 방지)
     * 3. 화면에 Redis 조회수 표시
     */
    @GetMapping("/posts/{id}")
    public String viewPost(
            @PathVariable Long id,
            HttpServletRequest request,
            Model model
    ) {
        // 클라이언트 IP 추출
        String clientIp = RequestUtils.getClientIp(request);

        // 게시글 조회 (조회수 증가 포함)
        PostResponse post = postService.getPostWithViewCount(id, clientIp);

        // 댓글 조회 (2 depth 방식)
        List<CommentWithRepliesResponse> comments = commentService.getCommentsTwoDepth(id);

        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("commentCount", commentService.getActiveCommentCount(id));

        return "posts/detail";
    }

    /**
     * 게시글 작성 화면
     */
    @GetMapping("/posts/new")
    public String newPostForm() {
        return "posts/form";
    }
}
