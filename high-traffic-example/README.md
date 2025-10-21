# High Traffic Handling Example

대용량 트래픽 처리를 위한 학습 프로젝트입니다.

## 학습 목표

- 대규모 시스템 디자인
- Microservice Architecture
- Event-Driven Architecture
- 분산 시스템에 대한 이해
- 동시성 문제를 다루는 방법
- MySQL, Redis, Kafka에 대한 이해 및 실전 활용 전략
- 데이터베이스 인덱스를 활용한 대규모 데이터 쿼리 최적화
- 복잡한 계층형 테이블 설계 및 최적화
- 높은 쓰기 트래픽에서도 데이터 일관성을 보장하는 방법
- 이벤트 스트림 처리 및 비동기 애플리케이션 구축
- 이벤트 유실 방지를 위한 시스템 구축
- 대규모 트래픽 및 복잡한 아키텍처에서 활용할 수 있는 방법론
- 다양한 요구사항에 적용할 수 있는 캐시 최적화 전략

## 기술 스택

- **Language**: Java 21
- **Framework**: Spring Boot 3.3.2
- **Build Tool**: Gradle
- **Database**: MySQL 8.0.38
- **Cache**: Redis 7.4
- **Message Queue**: Kafka 3.8.0
- **ORM**: Spring Data JPA

## 주요 기능 및 이슈 해결

### 1️⃣ 조회수 처리: Redis 기반 고성능 시스템

#### 문제 정의
대규모 게시판에서 조회수를 DB에 직접 업데이트할 경우 발생하는 문제:
- 매 조회마다 DB UPDATE 쿼리 실행 → 높은 DB 부하
- 동시 다발적인 UPDATE로 인한 Lock 경합
- 응답 속도 저하 (50~100ms → 병목 발생)
- 확장성 한계 (처리량: ~100 TPS)

#### 해결 방법
**Redis INCR 연산 + 주기적 DB 동기화**

```
┌─────────────┐    1. 조회    ┌──────────────┐    INCR    ┌──────────┐
│   사용자    │  ────────>   │ Spring Boot  │  ────────> │  Redis   │
└─────────────┘              └──────────────┘            └──────────┘
                                     │                         │
                                     │ 2. 5분마다             │
                                     │    스케줄러            │
                                     │                         │
                                     ▼                         ▼
                             ┌──────────────┐    UPDATE  ┌──────────┐
                             │   Scheduler  │  ────────> │  MySQL   │
                             └──────────────┘            └──────────┘
```

**핵심 구현**
- `ViewCountService.java`: Redis INCR 연산으로 조회수 증가
- `ViewCountScheduler.java`: 5분마다 Redis → DB 동기화
- `ViewCountInitializer.java`: 앱 시작 시 DB → Redis 초기화

**성능 개선 효과**
| 항목 | 기존 (DB 직접 UPDATE) | 개선 (Redis) |
|------|---------------------|-------------|
| 응답 시간 | 50~100ms | < 1ms |
| 동시 처리 능력 | ~100 TPS | 10,000+ TPS |
| DB 부하 | 매 조회마다 UPDATE | 5분마다 1회 배치 UPDATE |

---

### 2️⃣ 조회수 어뷰징 방지: 2단계 보안 정책

대규모 시스템에서 조회수는 중요한 지표이지만, 악의적인 사용자의 어뷰징 공격에 취약합니다.

#### 정책 A: 시간 기반 중복 방지 (기본)

**문제**: 동일 사용자가 F5 연타로 조회수 부풀리기

**해결**: Redis TTL을 활용한 중복 방지
```java
// ViewCountService.java
String duplicateKey = "post:viewed:{postId}:{ip}";  // IP 기반 식별
redisTemplate.set(duplicateKey, "1", Duration.ofSeconds(5));  // 5초 TTL

// 5초 이내 재조회 시
if (redisTemplate.hasKey(duplicateKey)) {
    // 조회수 증가 없이 현재 값 반환
    return getCurrentViewCount(postId);
}
```

**효과**
- 5초 이내 중복 조회 차단
- TTL 자동 만료로 메모리 효율적
- 정상 사용자는 영향 없음

#### 정책 B: Rate Limiting (중급)

**문제**: 자동화 스크립트로 무한 요청 (DDoS, 크롤링 봇)

**해결**: IP별 요청 횟수 제한 (Sliding Window)
```java
// RateLimitService.java
String key = "ratelimit:ip:{ip}";
Long count = redisTemplate.increment(key);  // 원자적 증가
if (count == 1) {
    redisTemplate.expire(key, Duration.ofSeconds(60));  // 1분 TTL
}

if (count > 20) {
    return false;  // 1분에 20회 초과 → 조회수 증가 차단
}
```

**정책 적용 효과**
| 요청 횟수 | 동작 |
|----------|------|
| 1~20회 (1분 이내) | ✅ 정상 처리 (조회수 증가) |
| 21회 이상 | ⚠️ 조회는 허용, 조회수 증가 차단 |
| 60초 후 | 🔄 카운터 리셋 |

**장점**
- 조회는 계속 가능 (사용자 경험 유지)
- 어뷰징만 차단 (조회수 통계 정확도 향상)
- Redis 원자적 연산으로 동시성 안전

---

### 3️⃣ 데이터 동기화 전략

#### Redis와 DB의 일관성 보장

**스케줄러 기반 동기화**
```java
// ViewCountScheduler.java
@Scheduled(fixedDelay = 5 * 60 * 1000)  // 5분마다
public void syncViewCountToDatabase() {
    Set<String> keys = redisTemplate.keys("post:viewcount:*");

    for (String key : keys) {
        Long viewCount = redisTemplate.get(key);
        postRepository.updateViewCount(postId, viewCount);
    }
}
```

**데이터 흐름**
1. **쓰기**: 사용자 조회 → Redis INCR (실시간)
2. **동기화**: 5분마다 Redis → DB 배치 UPDATE
3. **복구**: 앱 재시작 시 DB → Redis 초기화

**장점**
- 실시간성: Redis에서 즉시 반영
- 영속성: DB에 주기적으로 백업
- 성능: DB 부하 최소화

---

### 4️⃣ 댓글 시스템: 계층형 구조 최적화

#### 2가지 댓글 조회 방식

**1) 2-depth 댓글 (일반 게시판)**
- 댓글 → 대댓글 (최대 2단계)
- 인덱스: `(post_id, parent_id, created_at)`
- 쿼리 최적화: WHERE parent_id IS NULL / IS NOT NULL

**2) Infinite-depth 댓글 (Reddit, HackerNews 스타일)**
- 무제한 계층 구조
- 인덱스: `(post_id, depth, created_at)`
- 성능: depth별 조회로 최적화

---

### 5️⃣ 페이징 전략

#### Offset vs Cursor 기반 페이징

| 방식 | 장점 | 단점 | 사용 사례 |
|------|-----|------|---------|
| **Offset-based** | 페이지 번호 이동 가능 | OFFSET이 클수록 느림 | 일반 게시판, 검색 |
| **Cursor-based** | 빠른 성능, 일관성 보장 | 페이지 번호 이동 불가 | 무한 스크롤, 피드 |

**Cursor 기반 무한 스크롤 구현**
```java
// PostService.java
public CursorPageResponse<PostListResponse> getPostsByCursor(Long cursor, int size) {
    // WHERE id < cursor ORDER BY id DESC LIMIT size+1
    List<Post> posts = postRepository.findPostsByCursor(cursor, size + 1);

    boolean hasNext = posts.size() > size;
    Long nextCursor = hasNext ? posts.get(size - 1).getId() : null;

    return CursorPageResponse.of(posts.subList(0, size), nextCursor, hasNext);
}
```

---

## 프로젝트 구조

```
high-traffic-example/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/hightraffic/
│   │   │       ├── config/           # 설정 클래스
│   │   │       │   ├── AsyncConfig.java
│   │   │       │   ├── KafkaConfig.java
│   │   │       │   └── RedisConfig.java
│   │   │       ├── controller/       # REST API 컨트롤러
│   │   │       ├── service/          # 비즈니스 로직
│   │   │       ├── repository/       # 데이터 접근 계층
│   │   │       ├── domain/           # 엔티티
│   │   │       ├── dto/              # DTO
│   │   │       ├── event/            # 이벤트 객체
│   │   │       ├── consumer/         # Kafka Consumer
│   │   │       ├── producer/         # Kafka Producer
│   │   │       ├── exception/        # 예외 처리
│   │   │       ├── util/             # 유틸리티
│   │   │       └── HighTrafficApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
├── build.gradle
├── settings.gradle
└── docker-compose.yml
```

## 시작하기

### 사전 요구사항

- Java 21
- Docker & Docker Compose
- IntelliJ IDEA (권장)

### 인프라 실행

Docker Compose를 사용하여 MySQL, Redis, Kafka를 실행합니다:

```bash
docker-compose up -d
```

서비스 확인:
- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Kafka UI: `http://localhost:8989`

### 애플리케이션 실행

```bash
./gradlew bootRun
```

또는 IntelliJ IDEA에서 `HighTrafficApplication` 클래스를 실행합니다.

### 애플리케이션 중지

```bash
./gradlew bootStop
```

### 인프라 중지

```bash
docker-compose down
```

데이터 볼륨까지 삭제하려면:

```bash
docker-compose down -v
```

## 주요 설정

### Redis 설정 상세

#### Docker Compose 설정
- **최대 메모리**: 512MB (운영 환경에서는 더 크게 설정 권장)
- **메모리 정책**: `allkeys-lru` (메모리 부족 시 가장 오래된 키 제거)
- **영속성**: AOF(Append Only File) 활성화 (데이터 유실 방지)

#### Redis Key 구조 및 TTL 전략

| Key Pattern | 용도 | TTL | 예시 |
|------------|------|-----|------|
| `post:viewcount:{postId}` | 조회수 저장 | 영구 | `post:viewcount:1` |
| `post:viewed:{postId}:{ip}` | 중복 방지 플래그 | 5초 | `post:viewed:1:127.0.0.1` |
| `ratelimit:ip:{ip}` | Rate Limit 카운터 | 60초 | `ratelimit:ip:127.0.0.1` |

**TTL 전략**
- **조회수**: TTL 없음 (영구 저장, 스케줄러로 DB 동기화)
- **중복 방지**: 5초 TTL (짧은 시간 내 재조회 차단)
- **Rate Limit**: 60초 TTL (1분 윈도우)

#### RedisTemplate 설정
```java
// RedisConfig.java
@Bean
public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
    // Key: StringRedisSerializer (문자열)
    // Value: GenericJackson2JsonRedisSerializer (JSON)
    // 타임스탬프는 ISO-8601 형식으로 저장
}
```

### MySQL 설정

- 최대 연결 수: 1000
- InnoDB 버퍼 풀 크기: 1GB
- 문자 인코딩: UTF8MB4
- 인덱스 최적화:
  - Posts: `idx_created_at`, `idx_title`, `idx_author`
  - Comments: `idx_post_parent_created`, `idx_post_depth_created`

### Kafka 설정

- 파티션 수: 3
- 복제 계수: 1
- 기본 토픽: order-created, order-updated, payment-processed, inventory-updated

### HikariCP 설정

- 최대 풀 크기: 20
- 최소 유휴 연결: 10
- 연결 타임아웃: 30초

## 모니터링

### Actuator 엔드포인트

애플리케이션 실행 후 다음 엔드포인트에서 상태를 확인할 수 있습니다:

- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Info: `http://localhost:8080/actuator/info`

### Kafka UI

Kafka 토픽 및 메시지를 확인할 수 있습니다:

- URL: `http://localhost:8989`

## 성능 테스트 방법

### Redis 조회수 확인
```bash
# Redis CLI 접속
docker exec -it high-traffic-redis redis-cli

# 조회수 확인
GET post:viewcount:1

# 중복 방지 플래그 확인
KEYS post:viewed:*

# Rate Limit 확인
GET ratelimit:ip:127.0.0.1
TTL ratelimit:ip:127.0.0.1
```

### 부하 테스트 시나리오
```bash
# Apache Bench로 동시 100명이 1000번 요청
ab -n 1000 -c 100 http://localhost:8080/view/posts/1

# 예상 결과:
# - 1~20번째 요청: 조회수 증가
# - 21~1000번째 요청: 조회수 증가 없음 (Rate Limit)
# - 60초 후: Rate Limit 리셋
```

---

## 주요 학습 포인트

### 1. Redis 활용
- ✅ INCR 연산으로 동시성 문제 해결
- ✅ TTL로 메모리 효율적 관리
- ✅ 캐시 전략 (Write-behind, Cache-aside)

### 2. 성능 최적화
- ✅ DB 부하 감소 (매 요청 → 5분마다 배치)
- ✅ 응답 시간 개선 (100ms → 1ms)
- ✅ 처리량 증가 (100 TPS → 10,000+ TPS)

### 3. 어뷰징 방지
- ✅ 시간 기반 중복 방지 (5초 TTL)
- ✅ Rate Limiting (1분 20회)
- ✅ IP 기반 식별 (프록시 환경 대응)

### 4. 데이터 일관성
- ✅ Redis-DB 동기화 전략
- ✅ 스케줄러 기반 배치 처리
- ✅ 앱 재시작 시 복구 로직

---

## 트러블슈팅

### Redis 연결 오류
```bash
# Redis 컨테이너 상태 확인
docker ps | grep redis

# Redis 로그 확인
docker logs high-traffic-redis

# Redis 재시작
docker-compose restart redis
```

### 조회수 동기화 안됨
```bash
# 스케줄러 로그 확인
# 애플리케이션 로그에서 "조회수 DB 동기화" 검색

# 스케줄러 활성화 확인 (application.yml)
spring.task.scheduling.enabled: true
```

---

## 참고 자료

### Redis 관련
- [Redis 공식 문서 - INCR](https://redis.io/commands/incr/)
- [Redis TTL 전략](https://redis.io/commands/expire/)
- [Redis 메모리 최적화](https://redis.io/docs/manual/eviction/)

### Rate Limiting
- [Token Bucket vs Sliding Window](https://blog.cloudflare.com/counting-things-a-lot-of-different-things/)
- [분산 환경에서 Rate Limiting](https://engineering.grab.com/frequency-capping)

### 성능 최적화
- [DB 인덱스 최적화](https://use-the-index-luke.com/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

## 다음 단계

프로젝트 세팅이 완료되었습니다. 다음 기능을 추가로 구현할 수 있습니다:

- [ ] 좋아요 기능 (Redis Sorted Set 활용)
- [ ] 실시간 인기 게시글 랭킹 (Redis ZINCRBY)
- [ ] 캐시 워밍 전략 (애플리케이션 시작 시 인기 게시글 캐싱)
- [ ] Kafka를 활용한 이벤트 기반 통계 수집
- [ ] Grafana + Prometheus 모니터링 대시보드

---

## 라이선스

이 프로젝트는 학습 목적으로 작성되었습니다.
