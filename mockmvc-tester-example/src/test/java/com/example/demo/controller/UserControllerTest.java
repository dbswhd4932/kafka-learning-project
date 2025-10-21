package com.example.demo.controller;

import com.example.demo.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MockMvcTester를 사용한 API 테스트 예제
 *
 * Spring 6.2에서 추가된 MockMvcTester는:
 * - AssertJ와 통합되어 fluent API 제공
 * - Static import가 필요 없는 모던한 방식
 * - 더 직관적이고 읽기 쉬운 테스트 코드
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void getAllUsers_ShouldReturnUserList() {
        // MockMvcTester의 fluent API 사용 - exchange() 없이 바로 검증
        assertThat(mockMvcTester.get().uri("/api/users")
                .accept(MediaType.APPLICATION_JSON))
                .hasStatusOk()  // 200 OK 확인
                .hasContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void getUserById_WhenExists_ShouldReturnUser() {
        var result = mockMvcTester.get().uri("/api/users/1")
                .accept(MediaType.APPLICATION_JSON);

        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(User.class)
                .satisfies(user -> {
                    assertThat(user.getId()).isEqualTo(1L);
                    assertThat(user.getName()).isEqualTo("John Doe");
                    assertThat(user.getEmail()).isEqualTo("john@example.com");
                });
    }

    @Test
    void getUserById_WhenNotExists_ShouldReturn404() {
        assertThat(mockMvcTester.get().uri("/api/users/999"))
                .hasStatus(HttpStatus.NOT_FOUND);  // 404 Not Found 확인
    }

    @Test
    void createUser_ShouldReturnCreatedUser() {
        String newUserJson = """
                {
                    "name": "Bob Wilson",
                    "email": "bob@example.com"
                }
                """;

        var result = mockMvcTester.post().uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newUserJson);

        assertThat(result)
                .hasStatus2xxSuccessful()  // 2xx 성공 상태 확인
                .hasStatus(HttpStatus.CREATED)  // 201 Created 확인
                .bodyJson()
                .convertTo(User.class)
                .satisfies(user -> {
                    assertThat(user.getId()).isNotNull();
                    assertThat(user.getName()).isEqualTo("Bob Wilson");
                    assertThat(user.getEmail()).isEqualTo("bob@example.com");
                });
    }

    @Test
    void deleteUser_WhenExists_ShouldReturn204() {
        assertThat(mockMvcTester.delete().uri("/api/users/1"))
                .hasStatus(HttpStatus.NO_CONTENT);  // 204 No Content 확인
    }

    @Test
    void deleteUser_WhenNotExists_ShouldReturn404() {
        assertThat(mockMvcTester.delete().uri("/api/users/999"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void demonstrateFluentAPI() {
        // MockMvcTester의 다양한 fluent API 메서드 예제
        var result = mockMvcTester.get().uri("/api/users/1")
                .accept(MediaType.APPLICATION_JSON);

        // 1. 상태 및 컨텐츠 타입 검증
        assertThat(result)
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON);

        // 2. JSON 본문을 객체로 변환하여 검증
        assertThat(result)
                .bodyJson()
                .convertTo(User.class)
                .satisfies(user -> {
                    assertThat(user.getName()).startsWith("John").contains("Doe");
                    assertThat(user.getEmail()).contains("@example.com");
                });
    }

    @Test
    void demonstrateMultipleAssertions() {
        // 여러 검증을 하나의 테스트에서 수행하는 예제
        var result = mockMvcTester.post().uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Alice Smith",
                            "email": "alice@test.com"
                        }
                        """);

        // 상태 코드 검증
        assertThat(result).hasStatus(HttpStatus.CREATED);

        // 컨텐츠 타입 검증
        assertThat(result).hasContentType(MediaType.APPLICATION_JSON);

        // 응답 본문을 객체로 변환하여 여러 속성 검증
        assertThat(result)
                .bodyJson()
                .convertTo(User.class)
                .satisfies(user -> {
                    assertThat(user.getId()).isPositive();
                    assertThat(user.getName()).isEqualTo("Alice Smith");
                    assertThat(user.getEmail())
                            .isNotBlank()
                            .contains("@")
                            .endsWith("test.com");
                });
    }
}
