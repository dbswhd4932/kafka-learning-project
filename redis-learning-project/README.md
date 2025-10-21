# Redis 학습 프로젝트

Redis를 기초부터 심화까지 학습할 수 있는 Spring Boot 프로젝트입니다.

## 학습 목표

1. 조회 성능 최적화 방법
2. 실무에서 자주 쓰는 Redis 활용 방법
3. Redis 기본 사용법
4. Redis의 캐싱 기능 활용
5. 부하 테스트를 활용한 성능 비교

## 기술 스택

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- MySQL 8.0
- Redis 7.2
- Docker & Docker Compose

## 프로젝트 구조

```
redis-learning-project/
├── src/main/java/com/example/redis/
│   ├── config/          # Redis 설정
│   ├── controller/      # REST API 컨트롤러
│   ├── domain/          # 엔티티
│   ├── dto/             # DTO
│   ├── repository/      # JPA 리포지토리
│   └── service/         # 비즈니스 로직
├── docker-compose.yml   # Docker 설정
└── init.sql            # 초기 데이터
```

## 시작하기

### 1. Docker 컨테이너 실행

```bash
cd redis-learning-project
docker-compose up -d
```

MySQL(포트 3307)과 Redis(포트 6380)가 실행됩니다.

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

또는 IDE에서 `RedisLearningApplication` 클래스를 실행합니다.

애플리케이션은 `http://localhost:8080`에서 실행됩니다.

## API 엔드포인트

### 성능 비교용 API

#### 1. 모든 상품 조회

**Redis 캐싱 적용:**
```
GET http://localhost:8080/api/products/with-cache
```

**캐싱 미적용:**
```
GET http://localhost:8080/api/products/without-cache
```

#### 2. ID로 상품 조회

**Redis 캐싱 적용:**
```
GET http://localhost:8080/api/products/with-cache/{id}
예: GET http://localhost:8080/api/products/with-cache/1
```

**캐싱 미적용:**
```
GET http://localhost:8080/api/products/without-cache/{id}
예: GET http://localhost:8080/api/products/without-cache/1
```

#### 3. 카테고리별 상품 조회

**Redis 캐싱 적용:**
```
GET http://localhost:8080/api/products/with-cache/category/{category}
예: GET http://localhost:8080/api/products/with-cache/category/전자기기
```

**캐싱 미적용:**
```
GET http://localhost:8080/api/products/without-cache/category/{category}
예: GET http://localhost:8080/api/products/without-cache/category/전자기기
```

#### 4. 캐시 삭제

```
DELETE http://localhost:8080/api/products/cache
```

### 사용자 API (캐싱 적용)

```
GET http://localhost:8080/api/users
GET http://localhost:8080/api/users/{id}
GET http://localhost:8080/api/users/username/{username}
```

## 성능 비교 테스트 (Postman)

### 1단계: 캐싱 미적용 성능 측정

1. Postman에서 다음 요청을 3회 연속 실행:
   ```
   GET http://localhost:8080/api/products/without-cache
   ```

2. 응답에서 `duration_ms` 값을 확인합니다.
   - 예상 결과: 매번 약 1000ms (1초) 소요

### 2단계: Redis 캐싱 적용 성능 측정

1. 첫 번째 요청 (캐시 미스):
   ```
   GET http://localhost:8080/api/products/with-cache
   ```
   - 예상 결과: 약 1000ms (DB 조회 + 캐시 저장)

2. 두 번째 요청 (캐시 히트):
   ```
   GET http://localhost:8080/api/products/with-cache
   ```
   - 예상 결과: 약 10ms 미만 (Redis에서 조회)

3. 세 번째 요청 (캐시 히트):
   ```
   GET http://localhost:8080/api/products/with-cache
   ```
   - 예상 결과: 약 10ms 미만 (Redis에서 조회)

### 3단계: 캐시 삭제 후 재측정

1. 캐시 삭제:
   ```
   DELETE http://localhost:8080/api/products/cache
   ```

2. 다시 조회:
   ```
   GET http://localhost:8080/api/products/with-cache
   ```
   - 예상 결과: 다시 약 1000ms (캐시 미스)

## 성능 개선 효과

| 구분 | 캐싱 미적용 | 캐싱 적용 (첫 조회) | 캐싱 적용 (이후 조회) |
|------|------------|-------------------|-------------------|
| 응답 시간 | ~1000ms | ~1000ms | ~10ms |
| 성능 개선율 | - | - | **약 100배** |

## Redis 핵심 개념 학습

### 1. Redis란? / Redis의 장점

#### Redis란?

**Redis**(Remote Dictionary Server)는 **인메모리 키-값(Key-Value) 저장소**입니다.

- **메모리 기반**: 데이터를 RAM에 저장하여 초고속 읽기/쓰기 제공
- **NoSQL 데이터베이스**: 스키마 없이 유연한 데이터 구조 지원
- **오픈소스**: BSD 라이선스 기반의 무료 소프트웨어
- **싱글 스레드**: 원자적(Atomic) 연산 보장으로 데이터 일관성 유지

#### Redis의 주요 장점

1. **빠른 성능**
   - 메모리 기반이라 디스크 I/O 없이 작동
   - 평균 응답 시간: 1ms 미만
   - 초당 수만~수십만 건의 요청 처리 가능

2. **다양한 데이터 구조 지원**
   - String, List, Set, Sorted Set, Hash, Bitmap, HyperLogLog 등
   - 각 구조에 특화된 명령어 제공

3. **데이터 영속성**
   - RDB(Redis Database): 주기적 스냅샷 저장
   - AOF(Append Only File): 모든 쓰기 작업 로그 저장
   - 메모리 기반이지만 데이터 손실 방지 가능

4. **고가용성**
   - Master-Slave 복제 지원
   - Redis Sentinel을 통한 자동 장애 조치
   - Redis Cluster로 데이터 샤딩 및 분산 처리

5. **원자적 연산**
   - 싱글 스레드 기반으로 Race Condition 없음
   - 트랜잭션 및 Lua 스크립트 지원

### 2. Redis 주요 사용 사례

#### 1) 캐싱 (Caching)
- **가장 일반적인 사용 사례**
- DB 조회 결과, API 응답, 세션 데이터 등을 캐싱
- 응답 시간 단축 및 DB 부하 감소

```java
// 예: 상품 정보 캐싱
@Cacheable(value = "product", key = "#id")
public ProductResponse getProduct(Long id) {
    return productRepository.findById(id);
}
```

#### 2) 세션 저장소 (Session Store)
- 분산 환경에서 사용자 세션 공유
- 로그인 상태, 장바구니 정보 등 저장
- Spring Session + Redis 조합으로 쉽게 구현

#### 3) 실시간 순위표 (Leaderboard)
- Sorted Set을 활용한 실시간 랭킹 시스템
- 게임 점수, 인기 상품, 실시간 검색어 등
- O(log N) 시간 복잡도로 빠른 순위 조회/업데이트

```redis
ZADD leaderboard 1000 "user1"
ZADD leaderboard 1500 "user2"
ZREVRANGE leaderboard 0 9 WITHSCORES  # 상위 10명 조회
```

#### 4) 메시지 브로커 (Message Broker)
- Pub/Sub 패턴으로 실시간 메시징
- 채팅, 알림, 이벤트 스트리밍
- List를 활용한 메시지 큐

#### 5) 분산 락 (Distributed Lock)
- 여러 서버에서 동시 접근 제어
- 재고 차감, 좌석 예약 등 동시성 이슈 해결
- Redisson 라이브러리로 구현

```java
// Redisson 분산 락 예제
RLock lock = redissonClient.getLock("stock:123");
try {
    if (lock.tryLock(10, 1, TimeUnit.SECONDS)) {
        // 재고 차감 로직
        decreaseStock(productId);
    }
} finally {
    lock.unlock();
}
```

#### 6) 속도 제한 (Rate Limiting)
- API 호출 횟수 제한
- IP별 요청 제한
- Sliding Window 알고리즘 구현

#### 7) 실시간 분석 (Real-time Analytics)
- 조회수, 좋아요 수 등 실시간 카운팅
- HyperLogLog를 활용한 고유 방문자 수 추정
- Bitmap을 활용한 사용자 활동 추적

### 3. 백엔드 채용 공고에 종종 등장하는 '대용량 트래픽 처리 경험', 'Redis 사용 경험'

#### 왜 Redis 경험이 중요한가?

현대 웹 서비스는 **대용량 트래픽**과 **빠른 응답 속도**를 요구합니다. Redis는 이러한 요구사항을 해결하는 핵심 기술입니다.

#### 채용 공고에서 요구하는 역량

1. **성능 최적화 능력**
   - DB 부하를 줄이고 응답 속도를 개선한 경험
   - 캐싱 전략을 이해하고 적절히 적용할 수 있는 능력
   - 병목 지점을 찾아 Redis로 해결한 경험

2. **대용량 트래픽 처리 경험**
   - 동시 접속자 수천~수만 명 상황 대응
   - Redis를 활용한 수평 확장(Scale-out) 경험
   - 트래픽 급증 상황(예: 이벤트, 프로모션)에서의 안정성 확보

3. **분산 시스템 이해**
   - 여러 서버 환경에서 데이터 일관성 유지
   - 세션 공유, 분산 락 등 분산 환경 특화 기능 활용
   - Redis Cluster, Sentinel 등 고가용성 구성 경험

#### 실무에서 Redis를 활용한 문제 해결 사례

**사례 1: 메인 페이지 조회 속도 개선**
- 문제: 메인 페이지 로딩에 3초 소요 (인기 상품, 추천 상품 등 복잡한 쿼리)
- 해결: Redis 캐싱 적용으로 100ms 이하로 단축 (30배 개선)
- 효과: 사용자 이탈률 감소, 서버 비용 절감

**사례 2: 실시간 이벤트 페이지의 동시성 이슈**
- 문제: 선착순 쿠폰 이벤트에서 재고 초과 지급 발생
- 해결: Redis 분산 락으로 재고 차감 동기화
- 효과: 데이터 정합성 보장, 서비스 신뢰도 향상

**사례 3: 로그인 세션 관리**
- 문제: 서버 증설 시 세션 불일치로 로그아웃 현상
- 해결: Spring Session + Redis로 세션 중앙 관리
- 효과: 무중단 배포 가능, 사용자 경험 개선

#### 이 프로젝트로 배울 수 있는 실무 역량

✅ Redis를 활용한 **캐싱 전략** 이해
✅ 성능 개선 **전후 비교 및 측정** 경험
✅ Spring Boot와 Redis **통합 설정** 능력
✅ 실무에서 바로 적용 가능한 **코드 패턴**
✅ 면접에서 설명할 수 있는 **구체적인 수치와 경험**

### 4. 캐시(Cache), 캐싱(Caching)이란?

#### 캐시란?

**캐시(Cache)**는 자주 사용하는 데이터를 빠르게 접근할 수 있는 **임시 저장소**입니다.

- 원본 데이터 저장소(DB)보다 빠른 저장소(메모리)에 복사본 저장
- 동일한 요청에 대해 원본 조회 없이 캐시에서 즉시 응답
- 컴퓨터 과학의 보편적인 개념 (CPU 캐시, DNS 캐시, 웹 브라우저 캐시 등)

#### 캐싱이 필요한 이유

1. **응답 속도 향상**
   - DB 조회: 100ms → Redis 조회: 1ms (100배 빠름)
   - 사용자 대기 시간 감소로 UX 개선

2. **DB 부하 감소**
   - 동일한 쿼리 반복 실행 방지
   - DB 서버 자원 절약 (CPU, 메모리, 디스크 I/O)
   - 더 많은 트래픽 처리 가능

3. **비용 절감**
   - DB 서버 증설 대신 Redis로 해결
   - 클라우드 환경에서 컴퓨팅 비용 절감

4. **가용성 향상**
   - DB 장애 시에도 캐시된 데이터로 서비스 지속 가능
   - 시스템 안정성 증대

#### 캐싱하기 좋은 데이터

✅ **읽기가 많고 쓰기가 적은 데이터**
- 상품 정보, 카테고리 목록, 설정 값

✅ **계산 비용이 높은 데이터**
- 복잡한 집계 쿼리 결과, 통계 데이터

✅ **자주 조회되는 데이터**
- 인기 게시글, 베스트 상품, 메인 페이지 데이터

✅ **약간의 지연이 허용되는 데이터**
- 조회수, 좋아요 수 등 실시간성이 덜 중요한 데이터

#### 캐싱을 피해야 하는 데이터

❌ **실시간 정합성이 중요한 데이터**
- 금융 거래 정보, 재고 수량 (단, 분산 락과 함께 사용하면 가능)

❌ **민감한 개인 정보**
- 비밀번호, 결제 정보 (보안 위험)

❌ **거의 조회되지 않는 데이터**
- 캐싱 오버헤드만 발생

### 5. 데이터를 캐싱할 때 사용하는 전략

#### 1) Cache Aside (Lazy Loading)

**가장 일반적인 캐싱 전략**으로, 이 프로젝트에서 사용하는 방식입니다.

**동작 방식:**

```
1. 애플리케이션이 데이터 요청
2. 캐시에 데이터가 있는지 확인 (Cache Hit / Cache Miss)
3-1. Cache Hit: 캐시에서 데이터 반환
3-2. Cache Miss:
     → DB에서 데이터 조회
     → 조회한 데이터를 캐시에 저장
     → 데이터 반환
```

**코드 예시:**

```java
@Cacheable(value = "product", key = "#id")
public ProductResponse getProduct(Long id) {
    // Cache Miss 시에만 이 메서드가 실행됨
    return productRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("상품 없음"));
}
```

**장점:**
- 필요한 데이터만 캐싱 (메모리 효율적)
- 구현이 간단하고 직관적
- 캐시 장애 시에도 서비스 가능 (DB 조회로 fallback)

**단점:**
- 첫 요청은 느림 (Cache Miss)
- 캐시 만료 후 첫 요청도 느림
- Cache Miss 빈도가 높으면 효과 감소

#### 2) Write Around

**쓰기 작업 시 캐시를 거치지 않고 DB에만 저장**하는 전략입니다.

**동작 방식:**

```
[쓰기]
1. 데이터를 DB에 직접 저장
2. 캐시는 업데이트하지 않음 (또는 삭제)

[읽기]
1. Cache Aside 방식과 동일
2. Cache Miss 시 DB에서 조회 후 캐싱
```

**코드 예시:**

```java
// 상품 생성 시 캐시 삭제
@CacheEvict(value = "products", allEntries = true)
public Product createProduct(ProductRequest request) {
    return productRepository.save(request.toEntity());
}

// 상품 수정 시 해당 캐시만 삭제
@CacheEvict(value = "product", key = "#id")
public Product updateProduct(Long id, ProductRequest request) {
    Product product = productRepository.findById(id)
        .orElseThrow();
    product.update(request);
    return productRepository.save(product);
}
```

**장점:**
- 쓰기 작업이 빠름 (캐시 업데이트 불필요)
- 캐시와 DB 간 불일치 가능성 낮음
- Cache Aside와 함께 사용하기 좋음

**단점:**
- 쓰기 후 첫 읽기는 Cache Miss
- 자주 수정되는 데이터는 캐시 효과 감소

#### 3) Write Through (참고)

**쓰기 작업 시 캐시와 DB에 동시에 저장**하는 전략입니다.

```
1. 데이터를 캐시에 먼저 저장
2. 캐시가 DB에 동기적으로 저장
3. 완료 응답
```

**장점:**
- 쓰기 후 즉시 읽기 가능 (항상 Cache Hit)
- 데이터 일관성 보장

**단점:**
- 쓰기 성능 저하 (DB와 캐시 모두 업데이트)
- 사용되지 않는 데이터도 캐싱됨 (메모리 낭비)

#### 4) Write Back (Write Behind) (참고)

**쓰기 작업을 캐시에만 하고, 나중에 배치로 DB에 저장**하는 전략입니다.

```
1. 데이터를 캐시에만 저장
2. 일정 시간 후 또는 일정 개수가 쌓이면 DB에 배치 저장
```

**장점:**
- 쓰기 성능이 매우 빠름
- DB 쓰기 부하 감소 (배치 처리)

**단점:**
- 캐시 장애 시 데이터 손실 위험
- 구현 복잡도 높음
- 데이터 일관성 이슈

### 6. Cache Aside, Write Around 전략의 한계점 / 해결 방법

#### 주요 한계점

#### 1) Cache Stampede (캐시 스탬피드)

**문제:**
- 인기 데이터의 캐시가 만료되는 순간, 대량의 요청이 동시에 DB로 몰림
- DB에 순간적으로 엄청난 부하 발생
- 서비스 장애로 이어질 수 있음

```
시나리오:
11:00:00 - 인기 상품 캐시 만료 (TTL 10분)
11:00:01 - 동시 요청 1000건 → 모두 Cache Miss → DB 조회 1000회
```

**해결 방법:**

**A. Lock을 활용한 Single Flight Pattern**
```java
private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

public ProductResponse getProduct(Long id) {
    String cacheKey = "product:" + id;

    // 캐시 확인
    ProductResponse cached = getFromCache(cacheKey);
    if (cached != null) return cached;

    // Lock을 이용해 한 번만 DB 조회
    Object lock = locks.computeIfAbsent(cacheKey, k -> new Object());
    synchronized (lock) {
        // Double-checked locking
        cached = getFromCache(cacheKey);
        if (cached != null) return cached;

        // DB 조회 및 캐싱
        ProductResponse data = productRepository.findById(id);
        saveToCache(cacheKey, data);
        return data;
    }
}
```

**B. Probabilistic Early Expiration (확률적 조기 만료)**
```java
// TTL이 거의 끝나갈 때 미리 갱신
long ttl = redis.ttl(cacheKey);
long totalTtl = 600; // 10분

// 남은 시간이 20% 이하이면 10% 확률로 미리 갱신
if (ttl < totalTtl * 0.2 && Math.random() < 0.1) {
    refreshCache(cacheKey);
}
```

#### 2) Cache Warming (캐시 워밍) 문제

**문제:**
- 서버 재시작 또는 캐시 전체 삭제 후, 캐시가 텅 빔
- 초기 요청들이 모두 Cache Miss → DB에 부하

**해결 방법:**

**A. 서버 시작 시 미리 캐시 채우기**
```java
@Component
public class CacheWarmer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        log.info("캐시 워밍 시작");

        // 인기 상품 100개 미리 캐싱
        List<Product> popularProducts = productRepository
            .findTop100ByOrderByViewCountDesc();

        popularProducts.forEach(product -> {
            productService.getProduct(product.getId()); // 캐싱 유도
        });

        log.info("캐시 워밍 완료");
    }
}
```

**B. 백그라운드 캐시 갱신**
```java
@Scheduled(fixedRate = 300000) // 5분마다
public void refreshPopularCache() {
    // 인기 데이터를 주기적으로 갱신
    popularProductIds.forEach(id -> {
        ProductResponse data = productRepository.findById(id);
        cacheManager.getCache("product").put(id, data);
    });
}
```

#### 3) Stale Data (오래된 데이터) 문제

**문제:**
- 캐시된 데이터가 업데이트되지 않아 사용자에게 오래된 정보 제공
- DB의 데이터와 캐시 데이터가 불일치

**해결 방법:**

**A. 적절한 TTL 설정**
```yaml
spring:
  cache:
    redis:
      time-to-live: 600000  # 10분 (데이터 특성에 맞게 조정)
```

**B. 데이터 변경 시 캐시 무효화**
```java
@CacheEvict(value = "product", key = "#id")
public void updateProduct(Long id, ProductRequest request) {
    productRepository.update(id, request);
    // 캐시가 자동으로 삭제됨
}

@CacheEvict(value = "products", allEntries = true)
public void createProduct(ProductRequest request) {
    productRepository.save(request);
    // 전체 목록 캐시 삭제
}
```

**C. Cache-Aside + Pub/Sub 패턴**
```java
// 다중 서버 환경에서 캐시 동기화
public void updateProduct(Long id) {
    productRepository.update(id);

    // Redis Pub/Sub으로 다른 서버에 알림
    redisTemplate.convertAndSend("cache:invalidate",
        new CacheInvalidateMessage("product", id));
}
```

#### 4) Thundering Herd (우레떼) 문제

**문제:**
- 여러 서버에서 동시에 같은 데이터를 캐시하려고 시도
- DB에 중복 조회 발생

**해결 방법:**

**분산 락 사용 (Redisson)**
```java
RLock lock = redissonClient.getLock("cache:product:" + id);

try {
    if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
        // 다시 한번 캐시 확인 (다른 서버가 이미 캐싱했을 수 있음)
        ProductResponse cached = getFromCache(cacheKey);
        if (cached != null) return cached;

        // DB 조회 및 캐싱
        ProductResponse data = productRepository.findById(id);
        saveToCache(cacheKey, data);
        return data;
    }
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

#### 5) Memory Overhead (메모리 부족) 문제

**문제:**
- 너무 많은 데이터를 캐싱하면 메모리 부족
- Redis 메모리 초과 시 데이터 손실 또는 성능 저하

**해결 방법:**

**A. 적절한 Eviction Policy 설정**
```yaml
# Redis 설정
maxmemory 2gb
maxmemory-policy allkeys-lru  # LRU: 가장 오래 사용되지 않은 키 삭제
```

**B. 캐싱 대상 선별**
```java
// 조회수가 높은 데이터만 캐싱
@Cacheable(value = "product", key = "#id",
    condition = "#result.viewCount > 1000")
public ProductResponse getProduct(Long id) {
    return productRepository.findById(id);
}
```

**C. 캐시 크기 모니터링**
```java
@Scheduled(fixedRate = 60000)
public void monitorCacheSize() {
    Cache cache = cacheManager.getCache("product");
    RedisCache redisCache = (RedisCache) cache;

    long size = redisTemplate.getConnectionFactory()
        .getConnection()
        .dbSize();

    if (size > THRESHOLD) {
        log.warn("캐시 크기 임계값 초과: {}", size);
        // 알림 또는 대응 조치
    }
}
```

### 7. Redis 기본 명령어 익히기

Redis CLI에 접속하여 직접 실습할 수 있습니다.

```bash
# Redis CLI 접속
docker exec -it redis-learning-redis redis-cli
```

#### 1) String 타입

**가장 기본적인 키-값 저장**

```redis
# 값 저장
SET user:1:name "김철수"
SET user:1:age 30

# 값 조회
GET user:1:name
# 결과: "김철수"

# 여러 값 한번에 저장/조회
MSET user:2:name "이영희" user:2:age 25
MGET user:1:name user:2:name
# 결과: 1) "김철수" 2) "이영희"

# 숫자 증가/감소 (원자적 연산)
SET view:count 0
INCR view:count        # 1
INCRBY view:count 10   # 11
DECR view:count        # 10

# TTL과 함께 저장 (초 단위)
SETEX session:abc123 3600 "user_data"  # 1시간 후 만료

# 키가 없을 때만 저장
SETNX lock:product:1 "locked"  # 분산 락 구현에 활용
```

#### 2) Hash 타입

**객체를 필드-값 쌍으로 저장**

```redis
# 해시에 필드 저장
HSET product:1 name "노트북" price 1500000 stock 50

# 특정 필드 조회
HGET product:1 name
# 결과: "노트북"

# 모든 필드 조회
HGETALL product:1
# 결과:
# 1) "name"
# 2) "노트북"
# 3) "price"
# 4) "1500000"
# 5) "stock"
# 6) "50"

# 여러 필드 조회
HMGET product:1 name price
# 결과: 1) "노트북" 2) "1500000"

# 필드 값 증가
HINCRBY product:1 stock -1  # 재고 감소
HGET product:1 stock
# 결과: "49"

# 필드 존재 확인
HEXISTS product:1 name  # 1 (존재)
HEXISTS product:1 color # 0 (없음)

# 필드 삭제
HDEL product:1 stock
```

#### 3) List 타입

**순서가 있는 문자열 리스트 (양방향 큐)**

```redis
# 왼쪽에 추가 (앞에 삽입)
LPUSH notifications "새 댓글" "좋아요 알림"

# 오른쪽에 추가 (뒤에 삽입)
RPUSH notifications "새 팔로워"

# 범위 조회 (0부터 시작, -1은 끝)
LRANGE notifications 0 -1
# 결과:
# 1) "좋아요 알림"
# 2) "새 댓글"
# 3) "새 팔로워"

# 왼쪽에서 제거 (FIFO 큐)
LPOP notifications  # "좋아요 알림"

# 오른쪽에서 제거 (스택)
RPOP notifications  # "새 팔로워"

# 리스트 길이
LLEN notifications  # 1

# 특정 인덱스 조회
LINDEX notifications 0  # "새 댓글"
```

**메시지 큐 구현 예시:**
```redis
# Producer (메시지 생성자)
RPUSH task:queue "send_email:user123"
RPUSH task:queue "process_payment:order456"

# Consumer (메시지 소비자)
BLPOP task:queue 5  # 5초 대기, 데이터 있으면 즉시 반환
```

#### 4) Set 타입

**중복 없는 문자열 집합**

```redis
# 멤버 추가
SADD tags:1 "전자기기" "할인" "인기"

# 멤버 조회
SMEMBERS tags:1
# 결과: 1) "전자기기" 2) "할인" 3) "인기"

# 멤버 존재 확인
SISMEMBER tags:1 "할인"  # 1 (존재)
SISMEMBER tags:1 "품절"  # 0 (없음)

# 멤버 제거
SREM tags:1 "할인"

# 집합 크기
SCARD tags:1  # 2

# 집합 연산
SADD tags:2 "전자기기" "신상품"
SINTER tags:1 tags:2      # 교집합: "전자기기"
SUNION tags:1 tags:2      # 합집합: "전자기기", "인기", "신상품"
SDIFF tags:1 tags:2       # 차집합: "인기"
```

**실무 활용 예시:**
```redis
# 좋아요 기능
SADD like:post:123 "user1" "user2" "user3"
SCARD like:post:123              # 좋아요 수: 3
SISMEMBER like:post:123 "user1"  # user1이 좋아요 눌렀는지 확인

# 중복 방지 (이메일 발송 기록)
SADD sent:email:daily "user1@example.com"
if SISMEMBER sent:email:daily "user1@example.com"
    # 이미 발송됨 → 스킵
```

#### 5) Sorted Set 타입

**점수(score)로 정렬되는 집합**

```redis
# 멤버 추가 (score value)
ZADD leaderboard 1000 "user1"
ZADD leaderboard 1500 "user2"
ZADD leaderboard 1200 "user3"

# 점수 기준 오름차순 조회
ZRANGE leaderboard 0 -1 WITHSCORES
# 결과:
# 1) "user1"
# 2) "1000"
# 3) "user3"
# 4) "1200"
# 5) "user2"
# 6) "1500"

# 점수 기준 내림차순 조회 (랭킹)
ZREVRANGE leaderboard 0 2 WITHSCORES  # 상위 3명
# 결과:
# 1) "user2"
# 2) "1500"
# 3) "user3"
# 4) "1200"
# 5) "user1"
# 6) "1000"

# 특정 멤버 점수 조회
ZSCORE leaderboard "user1"  # "1000"

# 점수 증가
ZINCRBY leaderboard 100 "user1"  # 1100

# 등수 조회 (0부터 시작)
ZREVRANK leaderboard "user1"  # 2 (3등)

# 점수 범위로 조회
ZRANGEBYSCORE leaderboard 1000 1300
# 결과: 1) "user1" 2) "user3"

# 멤버 삭제
ZREM leaderboard "user1"
```

**실무 활용 예시:**
```redis
# 실시간 검색어 순위
ZINCRBY trending:search 1 "아이폰"
ZINCRBY trending:search 1 "갤럭시"
ZINCRBY trending:search 3 "아이폰"
ZREVRANGE trending:search 0 9 WITHSCORES  # 인기 검색어 Top 10

# 시간순 정렬 (타임스탬프를 score로 사용)
ZADD timeline 1672531200 "post:123"  # 2023-01-01 00:00:00
ZADD timeline 1672617600 "post:124"  # 2023-01-02 00:00:00
ZREVRANGE timeline 0 19  # 최신 20개 게시글
```

#### 6) Key 관리 명령어

```redis
# 모든 키 조회 (프로덕션에서는 위험! SCAN 사용 권장)
KEYS *
KEYS user:*  # 패턴 매칭

# 키 존재 확인
EXISTS user:1:name  # 1 (존재), 0 (없음)

# 키 삭제
DEL user:1:name
DEL product:1 product:2 product:3  # 여러 키 한번에 삭제

# 키 TTL 확인 (초 단위)
TTL session:abc123
# 결과: 3600 (남은 시간), -1 (만료 없음), -2 (키 없음)

# 키 만료 시간 설정
EXPIRE user:session 1800  # 30분 후 만료

# 키 타입 확인
TYPE user:1:name     # string
TYPE product:1       # hash
TYPE leaderboard     # zset

# 키 이름 변경
RENAME old:key new:key

# 데이터베이스 내 키 개수
DBSIZE
```

#### 7) 실용 명령어

```redis
# 트랜잭션
MULTI               # 트랜잭션 시작
SET balance 1000
DECRBY balance 100
GET balance
EXEC                # 트랜잭션 실행

# 파이프라인처럼 한번에 실행됨
# 결과:
# 1) OK
# 2) (integer) 900
# 3) "900"

# Pub/Sub (메시징)
# 터미널 1: 구독자
SUBSCRIBE notifications

# 터미널 2: 발행자
PUBLISH notifications "새로운 메시지!"

# 데이터베이스 전환 (0~15)
SELECT 1  # 1번 DB로 전환

# 현재 DB 모든 키 삭제 (주의!)
FLUSHDB

# 모든 DB의 모든 키 삭제 (매우 위험!)
FLUSHALL

# Redis 서버 정보
INFO
INFO memory   # 메모리 정보만
INFO stats    # 통계 정보만
```

### 8. Redis에서 Key 네이밍 컨벤션 익히기

**일관된 네이밍 규칙은 유지보수성과 가독성을 크게 향상**시킵니다.

#### 기본 원칙

#### 1) 콜론(:)으로 계층 구조 표현

가장 보편적인 Redis 네이밍 컨벤션입니다.

```redis
# ✅ 좋은 예
user:1:profile
user:1:settings
product:123:info
product:123:reviews
order:456:items

# ❌ 나쁜 예
user_1_profile        # 계층 구조 불명확
user-1-profile        # Redis에서는 : 권장
user/1/profile        # 슬래시는 비권장
```

**이유:**
- Redis 클라이언트에서 계층 구조로 시각화 가능
- SCAN으로 패턴 검색이 쉬움
- 가독성이 높고 일관성 유지

#### 2) 의미 있는 접두사 사용

**리소스 타입:ID:세부정보** 형식을 따릅니다.

```redis
# 사용자 관련
user:1:profile          # 사용자 프로필
user:1:sessions         # 사용자 세션 목록
user:1:cart             # 장바구니
user:email:test@example.com  # 이메일로 조회

# 상품 관련
product:123:info        # 상품 정보
product:123:stock       # 재고 수량
product:category:electronics  # 카테고리별 상품 목록

# 캐시 관련
cache:product:123       # 상품 캐시
cache:products:all      # 전체 상품 목록 캐시
cache:products:category:electronics  # 카테고리별 캐시

# 세션 관련
session:abc123def       # 세션 데이터
session:user:1          # 특정 사용자 세션

# 임시 데이터
temp:upload:xyz789      # 임시 업로드 파일
temp:verification:email:test@example.com  # 이메일 인증 임시 데이터

# 락 관련
lock:product:123        # 상품 수정 락
lock:order:456          # 주문 처리 락
```

#### 3) 복수형과 단수형 구분

```redis
# 단일 객체는 단수형
user:1:profile          # 한 명의 사용자 프로필
product:123:info        # 하나의 상품 정보

# 컬렉션은 복수형
users:active            # 활성 사용자 Set
products:trending       # 인기 상품 Sorted Set
notifications:user:1    # 사용자 알림 List
```

#### 4) 환경 구분

**개발/스테이징/프로덕션 환경을 접두사로 구분**할 수 있습니다.

```redis
# 환경별 접두사
dev:user:1:profile
staging:user:1:profile
prod:user:1:profile

# 또는 Redis DB 번호로 구분 (0~15)
SELECT 0  # 개발 환경
SELECT 1  # 스테이징
SELECT 2  # 프로덕션
```

#### 실무 네이밍 예시

#### 1) 캐싱

```redis
# Spring Cache에서 자동 생성되는 형식
cache:products::all                    # @Cacheable(value = "products", key = "'all'")
cache:product::123                     # @Cacheable(value = "product", key = "#id")
cache:products::category:electronics   # @Cacheable(value = "products", key = "'category:' + #category")

# RedisTemplate 직접 사용
cache:api:product:123
cache:api:products:page:1:size:10
cache:query:popular-products
```

#### 2) 세션 스토어

```redis
spring:session:sessions:abc123         # Spring Session
spring:session:expirations:1672531200  # 만료 시간 인덱스
spring:session:sessions:expires:abc123 # 세션 만료 시간
```

#### 3) 실시간 기능

```redis
# 조회수
view:count:post:123
view:count:product:456

# 좋아요
like:post:123              # Set: 좋아요 누른 사용자 목록
like:count:post:123        # String: 좋아요 개수

# 실시간 순위
leaderboard:daily:2023-01-01
leaderboard:weekly:2023-W01
leaderboard:all-time
```

#### 4) Rate Limiting (속도 제한)

```redis
rate:limit:api:user:123:2023-01-01-10  # 시간별 API 호출 제한
rate:limit:login:ip:192.168.1.1        # IP별 로그인 시도 제한
```

#### 5) 분산 락

```redis
lock:stock:product:123     # 재고 차감 락
lock:order:create:user:1   # 주문 생성 락
lock:payment:order:456     # 결제 처리 락
```

#### 네이밍 길이 고려사항

```redis
# ✅ 적절한 길이 (명확하면서 간결)
product:123:info
user:1:cart

# ⚠️ 너무 길면 메모리 낭비
product:information:detailed:full:123
user:shopping:cart:items:list:1

# ❌ 너무 짧으면 의미 파악 어려움
p:123
u:1:c
```

**권장사항:**
- 최대 100자 이내 권장
- 자주 사용되는 키는 짧게 (메모리 절약)
- 명확성과 간결성의 균형 유지

#### 실제 프로젝트 적용 예시

이 프로젝트에서 사용하는 네이밍:

```java
// ProductService.java
@Cacheable(value = "products", key = "'all'")
// → Redis Key: products::all

@Cacheable(value = "product", key = "#id")
// → Redis Key: product::123

@Cacheable(value = "products", key = "'category:' + #category")
// → Redis Key: products::category:Electronics
```

실제 Redis에 저장된 모습:
```redis
127.0.0.1:6379> KEYS *
1) "products::all"
2) "product::1"
3) "product::2"
4) "products::category:Electronics"
5) "products::category:전자기기"
```

#### 네이밍 컨벤션 체크리스트

✅ **DO (권장)**
- 콜론(:)으로 계층 구조 표현
- 의미 있는 접두사 사용
- 일관된 네이밍 규칙 유지
- 복수형/단수형 명확히 구분
- 환경별로 분리 (필요시)

❌ **DON'T (비권장)**
- 공백 사용 (띄어쓰기)
- 특수문자 남용
- 너무 긴 키 이름
- 일관성 없는 네이밍
- 한글/특수문자 키 (가능하면 영문)

## Redis 주요 학습 포인트

### 1. Redis 설정 (`RedisConfig.java`)

- `RedisTemplate`: Redis에 직접 데이터 저장/조회
- `CacheManager`: Spring Cache 추상화 사용
- JSON 직렬화 설정
- TTL(Time To Live) 설정

### 2. 캐싱 어노테이션

- `@Cacheable`: 조회 결과를 캐시에 저장
- `@CacheEvict`: 캐시 삭제
- `@CachePut`: 캐시 업데이트
- `@EnableCaching`: 캐싱 기능 활성화

### 3. 성능 최적화 전략

- 자주 조회되는 데이터 캐싱
- 변경이 적은 데이터 우선 캐싱
- 적절한 TTL 설정
- 캐시 무효화 전략

## 다음 단계

이 프로젝트는 기본적인 Redis 캐싱을 다룹니다. 추가로 학습할 수 있는 주제:

1. **Redis 데이터 구조**
   - String, Hash, List, Set, Sorted Set
   - RedisTemplate을 사용한 직접 조작

2. **분산 락 (Distributed Lock)**
   - 동시성 제어
   - Redisson 활용

3. **세션 관리**
   - Spring Session + Redis
   - 분산 환경에서의 세션 공유

4. **실시간 순위 시스템**
   - Sorted Set 활용
   - 리더보드 구현

5. **Pub/Sub 메시징**
   - 이벤트 기반 아키텍처
   - 실시간 알림

## 문제 해결

### Docker 컨테이너가 실행되지 않는 경우

```bash
# 기존 컨테이너 중지 및 삭제
docker-compose down

# 다시 시작
docker-compose up -d

# 컨테이너 상태 확인
docker-compose ps
```

### Redis 연결 오류

```bash
# Redis 컨테이너 로그 확인
docker logs redis-learning-redis

# Redis CLI 접속 테스트
docker exec -it redis-learning-redis redis-cli ping
# 응답: PONG
```

### MySQL 연결 오류

```bash
# MySQL 컨테이너 로그 확인
docker logs redis-learning-mysql

# MySQL 접속 테스트
docker exec -it redis-learning-mysql mysql -uuser -puser1234 redis_learning
```

## 참고 자료

- [Spring Data Redis 공식 문서](https://spring.io/projects/spring-data-redis)
- [Redis 공식 문서](https://redis.io/documentation)
- [Spring Cache 추상화](https://docs.spring.io/spring-framework/reference/integration/cache.html)
