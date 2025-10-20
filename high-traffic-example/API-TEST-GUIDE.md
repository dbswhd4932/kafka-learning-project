# API 테스트 가이드

이 문서는 게시글과 댓글 API를 테스트하는 방법을 안내합니다.

## 사전 준비

### 1. Docker 서비스 실행

```bash
# high-traffic-example 디렉토리에서 실행
docker-compose up -d
```

서비스 확인:
```bash
docker-compose ps
```

다음 서비스들이 실행되어야 합니다:
- MySQL (포트 3306)
- Redis (포트 6379)
- Kafka (포트 9092)
- Zookeeper (포트 2181)
- Kafka UI (포트 8989)

### 2. 애플리케이션 실행

**방법 1: Gradle 사용**
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

**방법 2: IntelliJ IDEA**
1. `HighTrafficApplication` 클래스를 엽니다
2. 실행 설정 편집 (Edit Configurations)
3. VM options에 `-Dspring.profiles.active=local` 추가
4. 실행 (Run)

애플리케이션이 시작되면:
- Spring Boot: `http://localhost:8080`
- Actuator: `http://localhost:8080/actuator/health`
- Kafka UI: `http://localhost:8989`

### 3. 테스트 데이터 자동 생성

`local` 프로파일로 실행하면 자동으로 다음 데이터가 생성됩니다:
- 게시글 100개
- 처음 10개 게시글: 2 depth 댓글 (댓글 + 대댓글)
- 다음 10개 게시글: 무한 depth 댓글 (최대 4 depth)
- 나머지 게시글: 랜덤 댓글

## HTTP 파일 테스트

IntelliJ IDEA의 HTTP Client를 사용하여 API를 테스트할 수 있습니다.

### 파일 열기
`api-test.http` 파일을 IntelliJ에서 엽니다.

### 테스트 실행 방법

1. **개별 요청 실행**: 각 요청 옆의 ▶ 버튼 클릭
2. **순차 실행**: 위에서부터 순서대로 실행 (변수가 자동으로 저장됨)
3. **환경 설정**: 필요시 `http-client.env.json` 파일로 환경별 설정 가능

### 테스트 시나리오

#### 1. 게시글 CRUD 테스트
```
✓ 게시글 생성
✓ 게시글 목록 조회 (페이지 번호 방식)
✓ 게시글 목록 조회 (커서 방식)
✓ 게시글 상세 조회
✓ 게시글 수정
✓ 좋아요 증가/감소
✓ 인기 게시글 조회
```

#### 2. 댓글 테스트 (2 Depth)
```
✓ 루트 댓글 생성
✓ 대댓글 생성
✓ 대대댓글 생성 시도 (실패해야 함)
✓ 댓글 목록 조회 (댓글 + 대댓글 구조)
```

#### 3. 댓글 테스트 (무한 Depth)
```
✓ 계층형 댓글 생성 (Depth 0~4)
✓ 댓글 목록 조회 (트리 구조)
✓ 소프트 삭제 테스트
```

## API 엔드포인트

### 게시글 API

| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/posts | 게시글 생성 |
| GET | /api/posts/{id} | 게시글 상세 조회 |
| GET | /api/posts | 게시글 목록 (페이지 번호) |
| GET | /api/posts/cursor | 게시글 목록 (커서) |
| PUT | /api/posts/{id} | 게시글 수정 |
| DELETE | /api/posts/{id} | 게시글 삭제 |
| POST | /api/posts/{id}/like | 좋아요 증가 |
| DELETE | /api/posts/{id}/like | 좋아요 감소 |
| GET | /api/posts/top/viewed | 조회수 상위 10개 |
| GET | /api/posts/top/liked | 좋아요 상위 10개 |

### 댓글 API (2 Depth)

| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/comments/two-depth | 댓글/대댓글 생성 |
| GET | /api/comments/two-depth?postId={id} | 댓글 목록 조회 |

### 댓글 API (무한 Depth)

| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/comments/infinite-depth | 계층형 댓글 생성 |
| GET | /api/comments/infinite-depth?postId={id} | 댓글 목록 조회 (트리) |

### 댓글 공통 API

| Method | URL | Description |
|--------|-----|-------------|
| GET | /api/comments/{id} | 댓글 상세 조회 |
| PUT | /api/comments/{id} | 댓글 수정 |
| DELETE | /api/comments/{id} | 댓글 삭제 |
| GET | /api/comments/count?postId={id} | 댓글 개수 |
| GET | /api/comments/count/active?postId={id} | 활성 댓글 개수 |

## 응답 예시

### 게시글 목록 (페이지 번호)
```json
{
  "content": [
    {
      "id": 1,
      "title": "게시글 제목",
      "author": "작성자",
      "viewCount": 10,
      "likeCount": 5,
      "createdAt": "2025-10-20T14:00:00"
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 100,
  "totalPages": 10,
  "first": true,
  "last": false,
  "hasNext": true,
  "hasPrevious": false
}
```

### 댓글 목록 (2 Depth)
```json
[
  {
    "id": 1,
    "postId": 1,
    "content": "댓글 내용",
    "author": "작성자",
    "isDeleted": false,
    "createdAt": "2025-10-20T14:00:00",
    "replies": [
      {
        "id": 2,
        "parentId": 1,
        "content": "대댓글 내용",
        "author": "작성자2",
        "isDeleted": false,
        "createdAt": "2025-10-20T14:01:00"
      }
    ]
  }
]
```

### 댓글 목록 (무한 Depth)
```json
[
  {
    "id": 1,
    "postId": 1,
    "parentId": null,
    "content": "루트 댓글",
    "author": "작성자",
    "depth": 0,
    "isDeleted": false,
    "createdAt": "2025-10-20T14:00:00",
    "children": [
      {
        "id": 2,
        "parentId": 1,
        "content": "자식 댓글",
        "depth": 1,
        "children": [
          {
            "id": 3,
            "parentId": 2,
            "content": "손자 댓글",
            "depth": 2,
            "children": []
          }
        ]
      }
    ]
  }
]
```

## 주요 기능 테스트

### 1. 페이징 방식 비교

**페이지 번호 방식 (Offset-based)**
```http
GET http://localhost:8080/api/posts?page=0&size=10
```
- 특정 페이지 접근 가능
- 전체 페이지 수 제공
- 대량 데이터에서 성능 저하

**커서 방식 (Cursor-based)**
```http
GET http://localhost:8080/api/posts/cursor?cursor=100&size=10
```
- 일관성 있는 결과
- 높은 성능
- 무한 스크롤에 적합

### 2. 댓글 Depth 제한

**2 Depth 방식**
- 댓글 (depth 0) → 대댓글 (depth 1)
- 대댓글에 답글 불가 (400 에러)

**무한 Depth 방식**
- 제한 없이 계층 생성 가능
- 재귀적 트리 구조

### 3. 댓글 삭제 전략

**자식 댓글 있음**
- 소프트 삭제 (내용만 "삭제된 댓글입니다"로 변경)
- isDeleted = true

**자식 댓글 없음**
- 실제 삭제
- 404 에러 반환

## 트러블슈팅

### 애플리케이션이 시작되지 않음

1. Docker 서비스 확인
```bash
docker-compose ps
```

2. MySQL 연결 확인
```bash
docker-compose logs mysql
```

3. 포트 충돌 확인
```bash
lsof -i :8080
lsof -i :3306
```

### 테스트 데이터가 생성되지 않음

- `local` 프로파일로 실행했는지 확인
- 로그에서 "테스트 데이터 초기화" 메시지 확인

### HTTP 파일이 작동하지 않음

- IntelliJ IDEA Professional 버전 필요
- Community 버전은 HTTP Client 플러그인 설치 필요

## 다음 단계

1. Kafka 이벤트 발행 테스트
2. Redis 캐싱 적용
3. 성능 테스트 (JMeter, Gatling)
4. 동시성 테스트
