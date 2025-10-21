# MockMvcTester 완벽 가이드

Spring Framework 6.2 (Spring Boot 3.4+)에서 추가된 **MockMvcTester**를 사용한 현대적인 API 테스트 가이드입니다.

## 목차

- [MockMvcTester란?](#mockmvctester란)
- [왜 MockMvcTester를 사용해야 하는가?](#왜-mockmvctester를-사용해야-하는가)
- [환경 설정](#환경-설정)
- [기본 사용법](#기본-사용법)
- [상세 사용 예제](#상세-사용-예제)
- [장점과 단점](#장점과-단점)
- [마이그레이션 가이드](#마이그레이션-가이드)
- [고려사항 및 베스트 프랙티스](#고려사항-및-베스트-프랙티스)
- [자주 묻는 질문 (FAQ)](#자주-묻는-질문-faq)

---

## MockMvcTester란?

**MockMvcTester**는 Spring Framework 6.2에서 도입된 새로운 테스트 도구로, 기존 MockMvc의 현대적 대안입니다.

### 핵심 특징

- ✅ **AssertJ 통합**: AssertJ의 강력한 fluent assertions 완벽 지원
- ✅ **Static Import 불필요**: 깔끔하고 읽기 쉬운 코드
- ✅ **타입 안정성**: 컴파일 타임에 오류 감지
- ✅ **직관적인 API**: 자연스러운 메서드 체이닝
- ✅ **향상된 가독성**: 테스트 의도를 명확하게 표현

---

## 왜 MockMvcTester를 사용해야 하는가?

### 기존 MockMvc의 문제점

```java
// ❌ 기존 MockMvc: 복잡하고 장황함
mockMvc.perform(get("/api/users/1")
        .accept(MediaType.APPLICATION_JSON))
    .andExpect(status().isOk())
    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    .andExpect(jsonPath("$.id").value(1))
    .andExpect(jsonPath("$.name").value("John Doe"))
    .andExpect(jsonPath("$.email").value("john@example.com"));
```

**문제점:**
- 수많은 static import 필요 (`get`, `status`, `content`, `jsonPath` 등)
- `andExpect` 체인이 길어지면 가독성 저하
- 복잡한 JSON 검증 시 코드가 난잡해짐
- 타입 안정성 부족

### MockMvcTester의 해결책

```java
// ✅ MockMvcTester: 간결하고 명확함
var result = mockMvcTester.get().uri("/api/users/1")
    .accept(MediaType.APPLICATION_JSON);

assertThat(result)
    .hasStatusOk()
    .hasContentType(MediaType.APPLICATION_JSON)
    .bodyJson()
    .convertTo(User.class)
    .satisfies(user -> {
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getName()).isEqualTo("John Doe");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
    });
```

**장점:**
- Static import 최소화 (AssertJ의 `assertThat`만 필요)
- 객체로 직접 변환하여 타입 안정성 확보
- AssertJ의 강력한 검증 기능 활용 가능
- 테스트 의도가 명확하게 드러남

---

## 환경 설정

### 요구사항

- **Java**: 17 이상
- **Spring Boot**: 3.4.0 이상
- **Spring Framework**: 6.2 이상

### build.gradle 설정

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.0'
    id 'io.spring.dependency-management' version '1.1.6'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### 테스트 클래스 설정

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;  // 자동 주입

    // 테스트 메서드...
}
```

#### 통합 테스트에서 사용

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserIntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    // 테스트 메서드...
}
```

#### 수동 설정 (특수한 경우)

```java
@BeforeEach
void setUp(WebApplicationContext context) {
    this.mockMvcTester = MockMvcTester.from(context);
}
```

---

## 기본 사용법

### 1. HTTP 메서드별 기본 요청

#### GET 요청

```java
@Test
void testGetRequest() {
    // 기본 GET 요청
    var result = mockMvcTester.get().uri("/api/users");

    assertThat(result).hasStatusOk();
}
```

#### POST 요청

```java
@Test
void testPostRequest() {
    String requestBody = """
        {
            "name": "John Doe",
            "email": "john@example.com"
        }
        """;

    var result = mockMvcTester.post().uri("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody);

    assertThat(result).hasStatus(HttpStatus.CREATED);
}
```

#### PUT 요청

```java
@Test
void testPutRequest() {
    String updateBody = """
        {
            "name": "Jane Doe",
            "email": "jane@example.com"
        }
        """;

    var result = mockMvcTester.put().uri("/api/users/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(updateBody);

    assertThat(result).hasStatusOk();
}
```

#### DELETE 요청

```java
@Test
void testDeleteRequest() {
    var result = mockMvcTester.delete().uri("/api/users/1");

    assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
}
```

#### PATCH 요청

```java
@Test
void testPatchRequest() {
    String patchBody = """
        {
            "email": "newemail@example.com"
        }
        """;

    var result = mockMvcTester.patch().uri("/api/users/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(patchBody);

    assertThat(result).hasStatusOk();
}
```

### 2. 상태 코드 검증

```java
@Test
void testStatusCodes() {
    // 200 OK
    assertThat(mockMvcTester.get().uri("/api/users"))
        .hasStatusOk();

    // 201 Created
    assertThat(result)
        .hasStatus(HttpStatus.CREATED);

    // 204 No Content
    assertThat(result)
        .hasStatus(HttpStatus.NO_CONTENT);

    // 400 Bad Request
    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST);

    // 404 Not Found
    assertThat(result)
        .hasStatus(HttpStatus.NOT_FOUND);

    // 2xx 범위 검증
    assertThat(result)
        .hasStatus2xxSuccessful();

    // 4xx 범위 검증
    assertThat(result)
        .hasStatus4xxClientError();

    // 5xx 범위 검증
    assertThat(result)
        .hasStatus5xxServerError();
}
```

### 3. 응답 본문 검증

#### JSON 응답을 객체로 변환

```java
@Test
void testJsonToObject() {
    var result = mockMvcTester.get().uri("/api/users/1");

    assertThat(result)
        .hasStatusOk()
        .bodyJson()
        .convertTo(User.class)
        .satisfies(user -> {
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getName()).isNotBlank();
            assertThat(user.getEmail()).contains("@");
        });
}
```

#### 텍스트 응답 검증

```java
@Test
void testTextResponse() {
    var result = mockMvcTester.get().uri("/api/hello");

    assertThat(result)
        .hasStatusOk()
        .bodyText()
        .isEqualTo("Hello, World!");
}
```

### 4. 헤더 검증

```java
@Test
void testResponseHeaders() {
    var result = mockMvcTester.get().uri("/api/users/1");

    assertThat(result)
        .hasStatusOk()
        .hasContentType(MediaType.APPLICATION_JSON)
        .headers()
        .containsKey("X-Custom-Header")
        .hasValue("X-Custom-Header", "custom-value");
}
```

### 5. 요청 파라미터 및 헤더 전송

```java
@Test
void testRequestParametersAndHeaders() {
    var result = mockMvcTester.get().uri("/api/users")
        .param("page", "0")
        .param("size", "10")
        .param("sort", "name,asc")
        .header("Authorization", "Bearer token")
        .header("X-Request-ID", "12345")
        .accept(MediaType.APPLICATION_JSON);

    assertThat(result).hasStatusOk();
}
```

---

## 상세 사용 예제

### 예제 1: 복잡한 JSON 응답 검증

```java
@Test
void testComplexJsonResponse() {
    var result = mockMvcTester.get().uri("/api/users/1");

    // 방법 1: 객체 변환 후 검증 (권장)
    assertThat(result)
        .bodyJson()
        .convertTo(User.class)
        .satisfies(user -> {
            assertThat(user.getId()).isPositive();
            assertThat(user.getName())
                .isNotNull()
                .startsWith("John")
                .endsWith("Doe");
            assertThat(user.getEmail())
                .isNotBlank()
                .matches("^[A-Za-z0-9+_.-]+@(.+)$");
            assertThat(user.getCreatedAt())
                .isNotNull()
                .isBefore(LocalDateTime.now());
        });
}
```

### 예제 2: 리스트 응답 검증

```java
@Test
void testListResponse() {
    var result = mockMvcTester.get().uri("/api/users");

    assertThat(result)
        .bodyJson()
        .convertTo(new ParameterizedTypeReference<List<User>>() {})
        .satisfies(users -> {
            assertThat(users)
                .isNotEmpty()
                .hasSizeGreaterThan(0)
                .allSatisfy(user -> {
                    assertThat(user.getId()).isNotNull();
                    assertThat(user.getEmail()).contains("@");
                });
        });
}
```

### 예제 3: 에러 응답 검증

```java
@Test
void testErrorResponse() {
    var result = mockMvcTester.get().uri("/api/users/999");

    assertThat(result)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .convertTo(ErrorResponse.class)
        .satisfies(error -> {
            assertThat(error.getStatus()).isEqualTo(404);
            assertThat(error.getMessage()).contains("User not found");
            assertThat(error.getTimestamp()).isNotNull();
        });
}
```

### 예제 4: 검증 에러 테스트

```java
@Test
void testValidationErrors() {
    String invalidUser = """
        {
            "name": "",
            "email": "invalid-email"
        }
        """;

    var result = mockMvcTester.post().uri("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidUser);

    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .convertTo(ValidationErrorResponse.class)
        .satisfies(errors -> {
            assertThat(errors.getFieldErrors())
                .hasSize(2)
                .extracting("field")
                .contains("name", "email");
        });
}
```

### 예제 5: 파일 업로드 테스트

```java
@Test
void testFileUpload() {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "test.txt",
        MediaType.TEXT_PLAIN_VALUE,
        "Hello, World!".getBytes()
    );

    var result = mockMvcTester.multipart()
        .uri("/api/upload")
        .file(file)
        .param("description", "Test file");

    assertThat(result)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .convertTo(FileUploadResponse.class)
        .satisfies(response -> {
            assertThat(response.getFileName()).isEqualTo("test.txt");
            assertThat(response.getFileSize()).isEqualTo(13L);
        });
}
```

### 예제 6: 쿠키 및 세션 테스트

```java
@Test
void testCookiesAndSession() {
    var result = mockMvcTester.get().uri("/api/profile")
        .cookie(new Cookie("SESSION", "abc123"))
        .sessionAttr("userId", 1L);

    assertThat(result)
        .hasStatusOk()
        .cookies()
        .containsKey("JSESSIONID");
}
```

### 예제 7: 리다이렉트 테스트

```java
@Test
void testRedirect() {
    var result = mockMvcTester.post().uri("/api/login")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("username", "user")
        .param("password", "pass");

    assertThat(result)
        .hasStatus(HttpStatus.FOUND)
        .hasRedirectedUrl("/dashboard");
}
```

### 예제 8: 예외 처리 테스트

```java
@Test
void testExceptionHandling() {
    var result = mockMvcTester.get().uri("/api/error-endpoint");

    assertThat(result)
        .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        .bodyJson()
        .convertTo(ErrorResponse.class)
        .satisfies(error -> {
            assertThat(error.getMessage()).contains("Internal server error");
        });
}
```

### 예제 9: 페이지네이션 테스트

```java
@Test
void testPagination() {
    var result = mockMvcTester.get().uri("/api/users")
        .param("page", "0")
        .param("size", "5")
        .param("sort", "name,asc");

    assertThat(result)
        .hasStatusOk()
        .bodyJson()
        .convertTo(PagedResponse.class)
        .satisfies(page -> {
            assertThat(page.getContent()).hasSize(5);
            assertThat(page.getTotalElements()).isGreaterThan(5);
            assertThat(page.getPageNumber()).isZero();
            assertThat(page.isSorted()).isTrue();
        });
}
```

### 예제 10: 조건부 요청 테스트

```java
@Test
void testConditionalRequest() {
    // 첫 번째 요청으로 ETag 받기
    var firstResult = mockMvcTester.get().uri("/api/users/1");
    String etag = firstResult.getResponse().getHeader("ETag");

    // ETag를 사용한 조건부 요청
    var secondResult = mockMvcTester.get().uri("/api/users/1")
        .header("If-None-Match", etag);

    assertThat(secondResult)
        .hasStatus(HttpStatus.NOT_MODIFIED);
}
```

---

## 장점과 단점

### 장점 ✅

#### 1. **향상된 가독성**
- AssertJ의 fluent API로 테스트 의도가 명확
- 자연스러운 영어 문장처럼 읽힘
- 코드 리뷰 시 이해하기 쉬움

#### 2. **타입 안정성**
```java
// ✅ 컴파일 타임에 오류 감지
User user = assertThat(result)
    .bodyJson()
    .convertTo(User.class);  // 타입이 명확함
```

#### 3. **Static Import 최소화**
```java
// MockMvc: 수많은 static import 필요
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

// MockMvcTester: 하나만 필요
import static org.assertj.core.api.Assertions.assertThat;
```

#### 4. **강력한 AssertJ 기능 활용**
```java
assertThat(result)
    .bodyJson()
    .convertTo(User.class)
    .satisfies(user -> {
        // AssertJ의 모든 검증 메서드 사용 가능
        assertThat(user.getName())
            .isNotBlank()
            .startsWith("John")
            .doesNotContain("123")
            .hasSizeBetween(3, 50);
    });
```

#### 5. **복잡한 객체 검증이 쉬움**
```java
// 중첩된 객체, 리스트, 맵 등을 쉽게 검증
assertThat(result)
    .bodyJson()
    .convertTo(Order.class)
    .satisfies(order -> {
        assertThat(order.getItems())
            .hasSize(3)
            .extracting("productName")
            .contains("Product A", "Product B");
    });
```

#### 6. **일관된 API**
- 모든 HTTP 메서드에 동일한 패턴 적용
- 학습 곡선이 낮음

#### 7. **IDE 자동완성 지원**
- 메서드 체이닝으로 IDE가 다음 단계를 제안
- 오타 및 실수 감소

### 단점 ❌

#### 1. **Spring 버전 제약**
- Spring Framework 6.2 이상 필수
- Spring Boot 3.4 이상 필요
- 레거시 프로젝트에서 사용 불가

#### 2. **학습 곡선**
- 기존 MockMvc에 익숙한 팀은 초기 적응 필요
- AssertJ 경험이 없다면 추가 학습 필요

#### 3. **문서 및 레퍼런스 부족**
- 비교적 최신 기능으로 커뮤니티 자료 제한적
- Stack Overflow 등에서 예제 찾기 어려움

#### 4. **기존 테스트 코드 마이그레이션 비용**
- 대규모 프로젝트에서 모든 테스트를 변경하려면 시간 소요
- 점진적 마이그레이션 전략 필요

#### 5. **JSON Path 직접 사용 불편**
```java
// MockMvc: JSON Path 직접 사용 편리
.andExpect(jsonPath("$.users[0].name").value("John"))

// MockMvcTester: 객체 변환 권장 (JSON Path는 덜 직관적)
// 간단한 필드 검증 시에는 오히려 번거로울 수 있음
```

#### 6. **일부 고급 기능은 여전히 MockMvc 필요**
- 특정 필터 체인 테스트
- 일부 저수준 MVC 기능 검증

---

## 마이그레이션 가이드

### MockMvc에서 MockMvcTester로 전환

#### 1. 기본 GET 요청

```java
// Before: MockMvc
@Test
void testGetUser_MockMvc() {
    mockMvc.perform(get("/api/users/1"))
        .andExpect(status().isOk());
}

// After: MockMvcTester
@Test
void testGetUser_MockMvcTester() {
    assertThat(mockMvcTester.get().uri("/api/users/1"))
        .hasStatusOk();
}
```

#### 2. POST 요청 with JSON

```java
// Before: MockMvc
@Test
void testCreateUser_MockMvc() throws Exception {
    String json = "{\"name\":\"John\"}";

    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("John"));
}

// After: MockMvcTester
@Test
void testCreateUser_MockMvcTester() {
    String json = "{\"name\":\"John\"}";

    var result = mockMvcTester.post().uri("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json);

    assertThat(result)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .convertTo(User.class)
        .satisfies(user -> {
            assertThat(user.getName()).isEqualTo("John");
        });
}
```

#### 3. 복잡한 JSON 검증

```java
// Before: MockMvc (Hamcrest)
@Test
void testComplexJson_MockMvc() throws Exception {
    mockMvc.perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].name", is("John")))
        .andExpect(jsonPath("$[0].email", containsString("@")));
}

// After: MockMvcTester (AssertJ)
@Test
void testComplexJson_MockMvcTester() {
    var result = mockMvcTester.get().uri("/api/users");

    assertThat(result)
        .hasStatusOk()
        .bodyJson()
        .convertTo(new ParameterizedTypeReference<List<User>>() {})
        .satisfies(users -> {
            assertThat(users).hasSize(2);
            assertThat(users.get(0).getName()).isEqualTo("John");
            assertThat(users.get(0).getEmail()).contains("@");
        });
}
```

#### 4. 헤더 검증

```java
// Before: MockMvc
@Test
void testHeaders_MockMvc() throws Exception {
    mockMvc.perform(get("/api/users/1"))
        .andExpect(header().string("Content-Type", "application/json"))
        .andExpect(header().exists("X-Custom-Header"));
}

// After: MockMvcTester
@Test
void testHeaders_MockMvcTester() {
    var result = mockMvcTester.get().uri("/api/users/1");

    assertThat(result)
        .hasContentType(MediaType.APPLICATION_JSON)
        .headers()
        .containsKey("X-Custom-Header");
}
```

### 점진적 마이그레이션 전략

1. **새로운 테스트는 MockMvcTester 사용**
   - 신규 기능 테스트부터 적용

2. **수정이 필요한 기존 테스트만 전환**
   - 버그 수정이나 기능 변경 시 함께 마이그레이션

3. **팀 교육 및 코드 리뷰**
   - 팀원들과 베스트 프랙티스 공유
   - 코드 리뷰에서 피드백

4. **양쪽 방식 혼용 가능**
   - 한 프로젝트에서 MockMvc와 MockMvcTester 공존 가능
   - 완벽한 일관성보다는 점진적 개선

---

## 고려사항 및 베스트 프랙티스

### 1. 언제 MockMvcTester를 사용해야 하는가?

#### ✅ 사용하면 좋은 경우

- **새 프로젝트 시작 시**
  - Spring Boot 3.4+ 사용
  - 처음부터 현대적인 테스트 작성

- **복잡한 JSON 응답 검증**
  - 중첩된 객체 구조
  - 리스트/맵 등 컬렉션 검증

- **타입 안정성이 중요한 경우**
  - 리팩토링이 빈번한 프로젝트
  - 대규모 팀 협업

- **AssertJ에 익숙한 팀**
  - 이미 AssertJ 사용 중
  - 일관된 테스트 스타일 선호

#### ❌ 사용하지 않는 것이 나은 경우

- **레거시 Spring 버전**
  - Spring Boot 3.4 미만
  - 업그레이드 계획 없음

- **간단한 API 테스트만 수행**
  - MockMvc로 충분
  - 마이그레이션 비용 대비 효과 적음

- **팀이 MockMvc에 익숙함**
  - 변경 저항이 큼
  - 학습 시간 부족

### 2. 성능 고려사항

MockMvcTester와 MockMvc는 **성능상 차이가 거의 없습니다**. 둘 다 내부적으로 동일한 MockMvc 인프라를 사용합니다.

```java
// 성능 차이 없음
var mockMvcResult = mockMvcTester.get().uri("/api/users");  // MockMvcTester
var result = mockMvc.perform(get("/api/users"));            // MockMvc
```

### 3. 테스트 가독성 향상 팁

#### 변수명을 명확하게

```java
// ❌ 나쁜 예
var r = mockMvcTester.get().uri("/api/users/1");
assertThat(r).hasStatusOk();

// ✅ 좋은 예
var getUserResult = mockMvcTester.get().uri("/api/users/1");
assertThat(getUserResult).hasStatusOk();
```

#### 메서드 추출로 재사용성 높이기

```java
// Helper 메서드
private MvcTestResult createUser(String name, String email) {
    String json = String.format("""
        {
            "name": "%s",
            "email": "%s"
        }
        """, name, email);

    return mockMvcTester.post().uri("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json);
}

@Test
void testCreateMultipleUsers() {
    assertThat(createUser("John", "john@example.com"))
        .hasStatus(HttpStatus.CREATED);

    assertThat(createUser("Jane", "jane@example.com"))
        .hasStatus(HttpStatus.CREATED);
}
```

#### Given-When-Then 패턴 활용

```java
@Test
@DisplayName("사용자 생성 시 올바른 응답을 반환해야 한다")
void shouldReturnCreatedUserWhenCreatingUser() {
    // Given
    String newUserJson = """
        {
            "name": "John Doe",
            "email": "john@example.com"
        }
        """;

    // When
    var result = mockMvcTester.post().uri("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(newUserJson);

    // Then
    assertThat(result)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .convertTo(User.class)
        .satisfies(user -> {
            assertThat(user.getId()).isNotNull();
            assertThat(user.getName()).isEqualTo("John Doe");
            assertThat(user.getEmail()).isEqualTo("john@example.com");
        });
}
```

### 4. 예외 처리 및 디버깅

#### 실패한 테스트 디버깅

```java
@Test
void debugFailedTest() {
    var result = mockMvcTester.get().uri("/api/users/999");

    // 응답 본문 출력하여 디버깅
    System.out.println("Response body: " + result.getResponse().getContentAsString());

    // 상태 코드 확인
    System.out.println("Status: " + result.getResponse().getStatus());

    assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
}
```

#### 예외 처리 명시적으로 테스트

```java
@Test
void testUnhandledException() {
    var result = mockMvcTester.get().uri("/api/error");

    // 예외가 발생했는지 확인
    assertThat(result)
        .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);

    // 예외 메시지 검증
    assertThat(result)
        .bodyJson()
        .convertTo(ErrorResponse.class)
        .satisfies(error -> {
            assertThat(error.getMessage()).isNotBlank();
        });
}
```

### 5. 보안 테스트

#### 인증/인가 테스트

```java
@Test
void testWithAuthentication() {
    var result = mockMvcTester.get().uri("/api/admin/users")
        .header("Authorization", "Bearer valid-token");

    assertThat(result).hasStatusOk();
}

@Test
void testUnauthorizedAccess() {
    var result = mockMvcTester.get().uri("/api/admin/users");

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
}
```

#### Spring Security와 함께 사용

```java
@Test
@WithMockUser(roles = "ADMIN")
void testWithMockUser() {
    var result = mockMvcTester.get().uri("/api/admin/dashboard");

    assertThat(result).hasStatusOk();
}
```

### 6. 테스트 데이터 관리

#### @BeforeEach로 공통 설정

```java
private User testUser;

@BeforeEach
void setUp() {
    testUser = new User(1L, "Test User", "test@example.com");
    // 데이터베이스 초기화 등
}

@Test
void testGetUser() {
    var result = mockMvcTester.get().uri("/api/users/" + testUser.getId());

    assertThat(result)
        .hasStatusOk()
        .bodyJson()
        .convertTo(User.class)
        .isEqualTo(testUser);
}
```

---

## 자주 묻는 질문 (FAQ)

### Q1: MockMvc와 MockMvcTester를 같이 사용할 수 있나요?

**A:** 네, 가능합니다. 한 프로젝트, 심지어 한 테스트 클래스 내에서도 혼용할 수 있습니다.

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;  // 기존 방식

    @Autowired
    private MockMvcTester mockMvcTester;  // 새로운 방식

    // 둘 다 사용 가능
}
```

### Q2: JSON Path를 직접 사용할 수 있나요?

**A:** 가능하지만, 객체 변환을 권장합니다. JSON Path가 필요하다면 MockMvc 사용을 고려하세요.

```java
// MockMvcTester에서는 객체 변환 권장
assertThat(result)
    .bodyJson()
    .convertTo(User.class)
    .satisfies(user -> assertThat(user.getName()).isEqualTo("John"));
```

### Q3: 성능 차이가 있나요?

**A:** 없습니다. 두 방식 모두 동일한 MockMvc 인프라를 사용하므로 성능은 동일합니다.

### Q4: 모든 기존 테스트를 마이그레이션해야 하나요?

**A:** 아니요. 점진적으로 마이그레이션하거나, 새 테스트만 MockMvcTester를 사용해도 됩니다.

### Q5: Spring Boot 3.3 이하에서 사용할 수 있나요?

**A:** 아니요. MockMvcTester는 Spring Framework 6.2 (Spring Boot 3.4) 이상에서만 사용 가능합니다.

### Q6: WebTestClient와 어떻게 다른가요?

**A:**
- **MockMvcTester**: Servlet 기반 Spring MVC 테스트 (동기)
- **WebTestClient**: WebFlux 리액티브 애플리케이션 테스트 (비동기)

Spring MVC 애플리케이션에서는 MockMvcTester를, Spring WebFlux에서는 WebTestClient를 사용하세요.

### Q7: RestAssured와 비교하면?

**A:**
- **MockMvcTester**: Spring 생태계 통합, 빠른 단위 테스트
- **RestAssured**: 독립적인 라이브러리, 실제 HTTP 요청

단위 테스트는 MockMvcTester, E2E 테스트는 RestAssured를 권장합니다.

---

## 프로젝트 실행

### 테스트 실행
```bash
./gradlew test
```

### 특정 테스트만 실행
```bash
./gradlew test --tests UserControllerTest
```

### 애플리케이션 실행
```bash
./gradlew bootRun
```

### API 엔드포인트

- `GET /api/users` - 모든 사용자 조회
- `GET /api/users/{id}` - 특정 사용자 조회
- `POST /api/users` - 새 사용자 생성
- `DELETE /api/users/{id}` - 사용자 삭제

---

## 참고 자료

- [Spring Framework 6.2 공식 문서](https://docs.spring.io/spring-framework/reference/testing/mockmvc/assertj.html)
- [MockMvcTester JavaDoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/servlet/assertj/MockMvcTester.html)
- [AssertJ 공식 문서](https://assertj.github.io/doc/)
- [Spring Boot 3.4 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.4-Release-Notes)

---

## 요구사항

- **Java**: 17 이상
- **Spring Boot**: 3.4.0 이상
- **Spring Framework**: 6.2 이상
- **Gradle**: 8.5 이상

---

## 라이선스

이 프로젝트는 학습 목적으로 작성되었습니다.
