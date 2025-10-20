package com.example.hightraffic.controller;

import com.example.hightraffic.domain.Comment;
import com.example.hightraffic.domain.Post;
import com.example.hightraffic.dto.CommentCreateRequest;
import com.example.hightraffic.dto.CommentUpdateRequest;
import com.example.hightraffic.repository.CommentRepository;
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
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    private Post testPost;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();

        // 테스트용 게시글 생성
        testPost = Post.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .author("테스터")
                .build();
        testPost = postRepository.save(testPost);
    }

    // ==================== 2 Depth 방식 테스트 ====================

    @Test
    @DisplayName("댓글 생성 테스트 (2 Depth - 루트 댓글)")
    void createRootCommentTwoDepth() throws Exception {
        // given
        CommentCreateRequest request = CommentCreateRequest.builder()
                .postId(testPost.getId())
                .parentId(null)
                .content("댓글 내용입니다")
                .author("댓글 작성자")
                .build();

        // when & then
        mockMvc.perform(post("/api/comments/two-depth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.postId").value(testPost.getId()))
                .andExpect(jsonPath("$.parentId").isEmpty())
                .andExpect(jsonPath("$.content").value("댓글 내용입니다"))
                .andExpect(jsonPath("$.depth").value(0))
                .andExpect(jsonPath("$.isDeleted").value(false));
    }

    @Test
    @DisplayName("대댓글 생성 테스트 (2 Depth)")
    void createReplyTwoDepth() throws Exception {
        // given
        Comment rootComment = Comment.createRoot(testPost.getId(), "루트 댓글", "작성자1");
        rootComment = commentRepository.save(rootComment);

        CommentCreateRequest request = CommentCreateRequest.builder()
                .postId(testPost.getId())
                .parentId(rootComment.getId())
                .content("대댓글 내용입니다")
                .author("댓글 작성자")
                .build();

        // when & then
        mockMvc.perform(post("/api/comments/two-depth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(rootComment.getId()))
                .andExpect(jsonPath("$.depth").value(1));
    }

    @Test
    @DisplayName("대대댓글 생성 실패 테스트 (2 Depth 제한)")
    void createReplyReplyTwoDepthShouldFail() throws Exception {
        // given
        Comment rootComment = Comment.createRoot(testPost.getId(), "루트 댓글", "작성자1");
        rootComment = commentRepository.save(rootComment);

        Comment reply = Comment.createChild(rootComment, testPost.getId(), "대댓글", "작성자2");
        reply = commentRepository.save(reply);

        CommentCreateRequest request = CommentCreateRequest.builder()
                .postId(testPost.getId())
                .parentId(reply.getId())
                .content("대대댓글 시도")
                .author("댓글 작성자")
                .build();

        // when & then
        mockMvc.perform(post("/api/comments/two-depth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("댓글 목록 조회 테스트 (2 Depth)")
    void getCommentsTwoDepth() throws Exception {
        // given
        Comment rootComment1 = Comment.createRoot(testPost.getId(), "댓글1", "작성자1");
        rootComment1 = commentRepository.save(rootComment1);

        Comment reply1 = Comment.createChild(rootComment1, testPost.getId(), "대댓글1", "작성자2");
        Comment reply2 = Comment.createChild(rootComment1, testPost.getId(), "대댓글2", "작성자3");
        commentRepository.save(reply1);
        commentRepository.save(reply2);

        // when & then
        mockMvc.perform(get("/api/comments/two-depth")
                        .param("postId", testPost.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(rootComment1.getId()))
                .andExpect(jsonPath("$[0].replies", hasSize(2)));
    }

    // ==================== 무한 Depth 방식 테스트 ====================

    @Test
    @DisplayName("댓글 생성 테스트 (무한 Depth - 루트 댓글)")
    void createRootCommentInfiniteDepth() throws Exception {
        // given
        CommentCreateRequest request = CommentCreateRequest.builder()
                .postId(testPost.getId())
                .parentId(null)
                .content("댓글 내용입니다")
                .author("댓글 작성자")
                .build();

        // when & then
        mockMvc.perform(post("/api/comments/infinite-depth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.depth").value(0));
    }

    @Test
    @DisplayName("깊은 댓글 생성 테스트 (무한 Depth - depth 3)")
    void createDeepCommentInfiniteDepth() throws Exception {
        // given
        Comment depth0 = Comment.createRoot(testPost.getId(), "depth 0", "작성자1");
        depth0 = commentRepository.save(depth0);

        Comment depth1 = Comment.createChild(depth0, testPost.getId(), "depth 1", "작성자2");
        depth1 = commentRepository.save(depth1);

        Comment depth2 = Comment.createChild(depth1, testPost.getId(), "depth 2", "작성자3");
        depth2 = commentRepository.save(depth2);

        CommentCreateRequest request = CommentCreateRequest.builder()
                .postId(testPost.getId())
                .parentId(depth2.getId())
                .content("depth 3")
                .author("작성자4")
                .build();

        // when & then
        mockMvc.perform(post("/api/comments/infinite-depth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.depth").value(3));
    }

    @Test
    @DisplayName("댓글 목록 조회 테스트 (무한 Depth)")
    void getCommentsInfiniteDepth() throws Exception {
        // given
        Comment root = Comment.createRoot(testPost.getId(), "루트", "작성자1");
        root = commentRepository.save(root);

        Comment child1 = Comment.createChild(root, testPost.getId(), "자식1", "작성자2");
        child1 = commentRepository.save(child1);

        Comment grandChild = Comment.createChild(child1, testPost.getId(), "손자", "작성자3");
        commentRepository.save(grandChild);

        // when & then
        mockMvc.perform(get("/api/comments/infinite-depth")
                        .param("postId", testPost.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(root.getId()))
                .andExpect(jsonPath("$[0].children", hasSize(1)))
                .andExpect(jsonPath("$[0].children[0].children", hasSize(1)));
    }

    // ==================== 공통 기능 테스트 ====================

    @Test
    @DisplayName("댓글 수정 테스트")
    void updateComment() throws Exception {
        // given
        Comment comment = Comment.createRoot(testPost.getId(), "원본 내용", "작성자");
        comment = commentRepository.save(comment);

        CommentUpdateRequest request = CommentUpdateRequest.builder()
                .content("수정된 내용")
                .build();

        // when & then
        mockMvc.perform(put("/api/comments/{id}", comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정된 내용"));
    }

    @Test
    @DisplayName("댓글 삭제 테스트 - 자식 댓글 없음 (실제 삭제)")
    void deleteCommentWithoutChildren() throws Exception {
        // given
        Comment comment = Comment.createRoot(testPost.getId(), "댓글", "작성자");
        comment = commentRepository.save(comment);

        // when & then
        mockMvc.perform(delete("/api/comments/{id}", comment.getId()))
                .andDo(print())
                .andExpect(status().isNoContent());

        // 삭제 확인
        mockMvc.perform(get("/api/comments/{id}", comment.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 삭제 테스트 - 자식 댓글 있음 (소프트 삭제)")
    void deleteCommentWithChildren() throws Exception {
        // given
        Comment parent = Comment.createRoot(testPost.getId(), "부모 댓글", "작성자1");
        parent = commentRepository.save(parent);

        Comment child = Comment.createChild(parent, testPost.getId(), "자식 댓글", "작성자2");
        commentRepository.save(child);

        // when
        mockMvc.perform(delete("/api/comments/{id}", parent.getId()))
                .andDo(print())
                .andExpect(status().isNoContent());

        // then - 소프트 삭제 확인
        mockMvc.perform(get("/api/comments/{id}", parent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDeleted").value(true))
                .andExpect(jsonPath("$.content").value("삭제된 댓글입니다."));
    }

    @Test
    @DisplayName("댓글 개수 조회 테스트")
    void getCommentCount() throws Exception {
        // given
        Comment comment1 = Comment.createRoot(testPost.getId(), "댓글1", "작성자1");
        Comment comment2 = Comment.createRoot(testPost.getId(), "댓글2", "작성자2");
        commentRepository.save(comment1);
        commentRepository.save(comment2);

        // when & then
        mockMvc.perform(get("/api/comments/count")
                        .param("postId", testPost.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    @DisplayName("활성 댓글 개수 조회 테스트")
    void getActiveCommentCount() throws Exception {
        // given
        Comment comment1 = Comment.createRoot(testPost.getId(), "댓글1", "작성자1");
        Comment comment2 = Comment.createRoot(testPost.getId(), "댓글2", "작성자2");
        comment1 = commentRepository.save(comment1);
        comment2 = commentRepository.save(comment2);

        // 하나 삭제
        comment1.delete();
        commentRepository.save(comment1);

        // when & then
        mockMvc.perform(get("/api/comments/count/active")
                        .param("postId", testPost.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 생성 실패")
    void createCommentOnNonExistentPost() throws Exception {
        // given
        CommentCreateRequest request = CommentCreateRequest.builder()
                .postId(9999L)
                .parentId(null)
                .content("댓글")
                .author("작성자")
                .build();

        // when & then
        mockMvc.perform(post("/api/comments/two-depth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 부모 댓글에 대댓글 생성 실패")
    void createReplyOnNonExistentParent() throws Exception {
        // given
        CommentCreateRequest request = CommentCreateRequest.builder()
                .postId(testPost.getId())
                .parentId(9999L)
                .content("대댓글")
                .author("작성자")
                .build();

        // when & then
        mockMvc.perform(post("/api/comments/two-depth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
