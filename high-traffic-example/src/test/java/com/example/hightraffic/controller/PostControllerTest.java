package com.example.hightraffic.controller;

import com.example.hightraffic.domain.Post;
import com.example.hightraffic.dto.PostCreateRequest;
import com.example.hightraffic.dto.PostUpdateRequest;
import com.example.hightraffic.repository.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.bootstrap-servers=localhost:9092"
})
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
    }

    @Test
    @DisplayName("게시글 생성 테스트")
    void createPost() throws Exception {
        // given
        PostCreateRequest request = PostCreateRequest.builder()
                .title("테스트 게시글")
                .content("테스트 내용입니다.")
                .author("테스터")
                .build();

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("테스트 게시글"))
                .andExpect(jsonPath("$.content").value("테스트 내용입니다."))
                .andExpect(jsonPath("$.author").value("테스터"))
                .andExpect(jsonPath("$.viewCount").value(0))
                .andExpect(jsonPath("$.likeCount").value(0));
    }

    @Test
    @DisplayName("게시글 단건 조회 테스트")
    void getPost() throws Exception {
        // given
        Post post = Post.builder()
                .title("테스트 게시글")
                .content("테스트 내용입니다.")
                .author("테스터")
                .build();
        Post savedPost = postRepository.save(post);

        // when & then
        mockMvc.perform(get("/api/posts/{id}", savedPost.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedPost.getId()))
                .andExpect(jsonPath("$.title").value("테스트 게시글"))
                .andExpect(jsonPath("$.viewCount").value(1)); // 조회 시 조회수 증가
    }

    @Test
    @DisplayName("게시글 목록 조회 테스트 - 페이지 번호 방식")
    void getPostsByPage() throws Exception {
        // given
        for (int i = 1; i <= 15; i++) {
            Post post = Post.builder()
                    .title("게시글 " + i)
                    .content("내용 " + i)
                    .author("작성자 " + i)
                    .build();
            postRepository.save(post);
        }

        // when & then
        mockMvc.perform(get("/api/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    @DisplayName("게시글 목록 조회 테스트 - 커서 방식 (첫 페이지)")
    void getPostsByCursorFirstPage() throws Exception {
        // given
        for (int i = 1; i <= 15; i++) {
            Post post = Post.builder()
                    .title("게시글 " + i)
                    .content("내용 " + i)
                    .author("작성자 " + i)
                    .build();
            postRepository.save(post);
        }

        // when & then
        mockMvc.perform(get("/api/posts/cursor")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").exists())
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("게시글 목록 조회 테스트 - 커서 방식 (다음 페이지)")
    void getPostsByCursorNextPage() throws Exception {
        // given
        for (int i = 1; i <= 15; i++) {
            Post post = Post.builder()
                    .title("게시글 " + i)
                    .content("내용 " + i)
                    .author("작성자 " + i)
                    .build();
            postRepository.save(post);
        }

        // 첫 페이지 조회 후 커서 값을 사용
        Long cursor = 10L;

        // when & then
        mockMvc.perform(get("/api/posts/cursor")
                        .param("cursor", cursor.toString())
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(10))));
    }

    @Test
    @DisplayName("게시글 수정 테스트")
    void updatePost() throws Exception {
        // given
        Post post = Post.builder()
                .title("원본 제목")
                .content("원본 내용")
                .author("테스터")
                .build();
        Post savedPost = postRepository.save(post);

        PostUpdateRequest request = PostUpdateRequest.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .build();

        // when & then
        mockMvc.perform(put("/api/posts/{id}", savedPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.content").value("수정된 내용"))
                .andExpect(jsonPath("$.author").value("테스터")); // 작성자는 변경되지 않음
    }

    @Test
    @DisplayName("게시글 삭제 테스트")
    void deletePost() throws Exception {
        // given
        Post post = Post.builder()
                .title("삭제할 게시글")
                .content("내용")
                .author("테스터")
                .build();
        Post savedPost = postRepository.save(post);

        // when & then
        mockMvc.perform(delete("/api/posts/{id}", savedPost.getId()))
                .andDo(print())
                .andExpect(status().isNoContent());

        // 삭제 후 조회 시 404 에러
        mockMvc.perform(get("/api/posts/{id}", savedPost.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("좋아요 증가 테스트")
    void increaseLike() throws Exception {
        // given
        Post post = Post.builder()
                .title("테스트 게시글")
                .content("내용")
                .author("테스터")
                .build();
        Post savedPost = postRepository.save(post);

        // when & then
        mockMvc.perform(post("/api/posts/{id}/like", savedPost.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));
    }

    @Test
    @DisplayName("좋아요 감소 테스트")
    void decreaseLike() throws Exception {
        // given
        Post post = Post.builder()
                .title("테스트 게시글")
                .content("내용")
                .author("테스터")
                .build();
        post.increaseLikeCount();
        post.increaseLikeCount();
        Post savedPost = postRepository.save(post);

        // when & then
        mockMvc.perform(delete("/api/posts/{id}/like", savedPost.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));
    }

    @Test
    @DisplayName("조회수 상위 게시글 조회 테스트")
    void getTopViewedPosts() throws Exception {
        // given
        for (int i = 1; i <= 5; i++) {
            Post post = Post.builder()
                    .title("게시글 " + i)
                    .content("내용 " + i)
                    .author("작성자")
                    .build();
            for (int j = 0; j < i * 10; j++) {
                post.increaseViewCount();
            }
            postRepository.save(post);
        }

        // when & then
        mockMvc.perform(get("/api/posts/top/viewed"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].viewCount").exists());
    }

    @Test
    @DisplayName("좋아요 상위 게시글 조회 테스트")
    void getTopLikedPosts() throws Exception {
        // given
        for (int i = 1; i <= 5; i++) {
            Post post = Post.builder()
                    .title("게시글 " + i)
                    .content("내용 " + i)
                    .author("작성자")
                    .build();
            for (int j = 0; j < i * 5; j++) {
                post.increaseLikeCount();
            }
            postRepository.save(post);
        }

        // when & then
        mockMvc.perform(get("/api/posts/top/liked"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    @DisplayName("유효하지 않은 요청 테스트 - 제목 누락")
    void createPostWithoutTitle() throws Exception {
        // given
        PostCreateRequest request = PostCreateRequest.builder()
                .content("내용")
                .author("작성자")
                .build();

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 테스트")
    void getPostNotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/posts/{id}", 9999L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
