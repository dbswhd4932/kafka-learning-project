# Spring Boot 비동기 통신 예제 프로젝트

Spring Boot, JPA, Gradle을 활용한 비동기 통신 패턴 예제 프로젝트입니다.

## 목차
- [프로젝트 스펙](#프로젝트-스펙)
- [주요 기능](#주요-기능)
- [비동기 처리 핵심 개념](#비동기-처리-핵심-개념)
- [스레드 풀 설정 완벽 가이드](#스레드-풀-설정-완벽-가이드)
- [API 엔드포인트](#api-엔드포인트)
- [테스트 실행](#테스트-실행)
- [운영 환경 고려사항](#운영-환경-고려사항)
- [트러블슈팅](#트러블슈팅)

---

## 프로젝트 스펙

- Java 17 (Java 22 호환)
- Spring Boot 3.2.0
- Spring Data JPA
- Gradle 8.12
- H2 Database (In-Memory)
- Lombok

---

## 주요 기능

### 1. 비동기 처리 패턴

#### @Async 어노테이션 활용
- 메서드 레벨에서 비동기 실행
- 별도의 스레드 풀에서 작업 수행
- CompletableFuture를 통한 결과 반환

#### 사용자 관리 (UserService)
- **동기 방식**: 사용자 생성 + 이메일 전송 완료 대기 (~3초)
- **비동기 방식**: 사용자 생성 즉시 반환, 이메일은 백그라운드 처리 (~즉시)

#### 주문 처리 (OrderService)
- 주문 생성 후 백그라운드에서 비동기 처리
- 여러 주문을 병렬로 처리 (CompletableFuture 활용)

### 2. 스레드 풀 설정

`AsyncConfig.java`에서 커스텀 Executor 설정:

- **taskExecutor**: 일반 비동기 작업용
  - Core Pool Size: 5
  - Max Pool Size: 10
  - Queue Capacity: 100

- **emailExecutor**: 이메일 전송 전용
  - Core Pool Size: 5
  - Max Pool Size: 10
  - Queue Capacity: 50

---

## 비동기 처리 핵심 개념

### 1. @EnableAsync - 비동기 기능 활성화

```java
@SpringBootApplication
@EnableAsync  // ⭐ 이것이 없으면 @Async가 동작하지 않음!
public class AsyncCommunicationApplication {
    public static void main(String[] args) {
        SpringApplication.run(AsyncCommunicationApplication.class, args);
    }
}
```

### 2. @Async 메서드 - 비동기 실행

#### 기본 사용법
```java
@Async("taskExecutor")  // ⭐ Executor 이름 지정 (권장)
public void processOrderAsync(Long orderId) {
    // 별도 스레드에서 실행됨
    // 메인 스레드는 즉시 반환
}
```

#### ⚠️ 왜 Executor 이름을 명시해야 할까?

```java
// ❌ 나쁜 예: Executor 지정 안 함
@Async
public void someMethod() {
    // Spring 기본 SimpleAsyncTaskExecutor 사용
    // → 매번 새 스레드 생성 (비효율적, 리소스 낭비!)
}

// ✅ 좋은 예: Executor 명시
@Async("emailExecutor")
public void someMethod() {
    // emailExecutor 스레드 풀 사용
    // → 스레드 재사용, 리소스 효율적
}
```

**Executor를 명시하는 이유:**
1. **리소스 효율성**: 스레드 재사용으로 생성/삭제 비용 절감
2. **작업 격리**: 이메일 발송과 주문 처리를 별도 스레드 풀로 분리
3. **성능 제어**: 작업 유형별로 스레드 개수 조정 가능
4. **장애 격리**: 한 작업이 느려져도 다른 작업에 영향 없음

### 3. CompletableFuture - 비동기 결과 반환

```java
@Async("emailExecutor")
public CompletableFuture<Boolean> sendEmailWithResult(String to, String subject, String body) {
    // 작업 수행
    boolean success = sendEmail(to, subject, body);

    // 결과 반환 (호출자가 나중에 받을 수 있음)
    return CompletableFuture.completedFuture(success);
}
```

### 4. 병렬 처리 - 여러 작업 동시 실행

```java
// 3개 작업을 동시에 시작
CompletableFuture<Order> future1 = orderService.processOrderWithResult(order1.getId());
CompletableFuture<Order> future2 = orderService.processOrderWithResult(order2.getId());
CompletableFuture<Order> future3 = orderService.processOrderWithResult(order3.getId());

// 모든 작업이 완료될 때까지 대기
CompletableFuture.allOf(future1, future2, future3).join();

// 개별 결과 가져오기
Order result1 = future1.join();
Order result2 = future2.join();
Order result3 = future3.join();
```

**성능 비교:**
- 순차 실행: 3초 + 3초 + 3초 = **9초**
- 병렬 실행: max(3초, 3초, 3초) = **3초** ✅

### 5. 체이닝 - 순차적 비동기 실행

```java
emailService.sendEmailWithResult(email, "Step 1", "First")
    .thenCompose(result -> emailService.sendEmailWithResult(email, "Step 2", "Second"))
    .thenCompose(result -> emailService.sendEmailWithResult(email, "Step 3", "Third"))
    .thenApply(result -> {
        log.info("All steps completed!");
        return result;
    });
```

---

## 스레드 풀 설정 완벽 가이드

### AsyncConfig.java 설정 파일

```java
@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);           // ⭐ 핵심 설정
        executor.setMaxPoolSize(10);           // ⭐ 핵심 설정
        executor.setQueueCapacity(100);        // ⭐ 핵심 설정
        executor.setThreadNamePrefix("async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        return executor;
    }

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);           // ⭐ 핵심 설정
        executor.setMaxPoolSize(10);           // ⭐ 핵심 설정
        executor.setQueueCapacity(50);         // ⭐ 핵심 설정
        executor.setThreadNamePrefix("email-");
        executor.initialize();

        return executor;
    }
}
```

### 각 설정의 의미와 동작 원리

#### 1. CorePoolSize (기본 스레드 개수)

```java
executor.setCorePoolSize(5);  // 기본적으로 유지하는 스레드 개수
```

**의미:**
- 항상 유지되는 기본 스레드 개수
- 작업이 없어도 이 개수만큼은 항상 살아있음 (대기 상태)

**예시:**
```
CorePoolSize = 5

작업 없을 때:  [대기] [대기] [대기] [대기] [대기]  ← 5개 스레드 유지
작업 3개 요청:  [실행] [실행] [실행] [대기] [대기]  ← 3개 실행, 2개 대기
```

#### 2. MaxPoolSize (최대 스레드 개수)

```java
executor.setMaxPoolSize(10);  // 최대로 늘어날 수 있는 스레드 개수
```

**의미:**
- 큐가 가득 찰 때 추가로 생성할 수 있는 최대 스레드 개수
- **주의**: 큐가 꽉 차야만 CorePoolSize를 넘어서 늘어남!

**동작 순서:**
```
1. 작업 요청
2. CorePoolSize(5) 이하면 → 새 스레드 생성
3. CorePoolSize(5) 이상이면 → 큐에 넣음
4. 큐가 가득 차면 → MaxPoolSize(10)까지 스레드 생성
5. MaxPoolSize도 초과하면 → RejectedExecutionException 발생!
```

#### 3. QueueCapacity (큐 용량)

```java
executor.setQueueCapacity(100);  // 대기 큐의 크기
```

**의미:**
- 모든 스레드가 작업 중일 때 대기하는 작업을 저장하는 큐의 크기

**실제 동작 예시:**
```
CorePoolSize = 5, QueueCapacity = 100

작업 5개 요청  → 5개 스레드 실행 (큐: 0)
작업 10개 요청 → 5개 실행, 5개 큐 대기 (큐: 5)
작업 110개 요청 → 5개 실행, 100개 큐 대기, 5개 추가 스레드 생성 (MaxPoolSize까지)
```

### ⚠️ 실제 발생한 문제 사례

#### 문제 상황
```java
// 잘못된 설정
executor.setCorePoolSize(2);  // ❌ 너무 작음!
executor.setMaxPoolSize(5);
executor.setQueueCapacity(50);
```

**3개 작업 요청 시 동작:**
```
Time 0s:  [email-1] Task 1 시작 (실행)
          [email-2] Task 2 시작 (실행)
          Task 3 → 큐에 대기 (큐가 안 찼으므로 스레드 추가 생성 안 됨!)

Time 3s:  [email-1] Task 1 완료
          [email-1] Task 3 시작 (큐에서 꺼내서 실행)

Time 6s:  [email-1] Task 3 완료
```

**결과**: 3개 작업이 병렬 실행될 것 같지만, 실제로는 6초 소요! ❌

#### 해결 방법
```java
// 올바른 설정
executor.setCorePoolSize(5);  // ✅ 충분한 크기!
executor.setMaxPoolSize(10);
executor.setQueueCapacity(50);
```

**3개 작업 요청 시 동작:**
```
Time 0s:  [email-1] Task 1 시작
          [email-2] Task 2 시작
          [email-3] Task 3 시작  ← 모두 동시 실행!

Time 3s:  모두 완료 ✅
```

**결과**: 3초에 모두 완료! ✅

### 4. 기타 유용한 설정

#### ThreadNamePrefix - 스레드 이름 접두사
```java
executor.setThreadNamePrefix("email-");
```

**로그에서 확인:**
```
[email-1] Sending email to: test@example.com
[email-2] Sending email to: admin@example.com
[async-1] Processing order: 123
[async-2] Processing order: 456
```

→ 어떤 작업이 어떤 스레드 풀에서 실행되는지 명확하게 파악 가능!

#### WaitForTasksToCompleteOnShutdown
```java
executor.setWaitForTasksToCompleteOnShutdown(true);
```

**의미:**
- 애플리케이션 종료 시 진행 중인 작업을 기다릴지 여부
- `true`: 모든 작업 완료 후 종료 (데이터 손실 방지)
- `false`: 즉시 종료 (작업 중단될 수 있음)

#### AwaitTerminationSeconds
```java
executor.setAwaitTerminationSeconds(60);
```

**의미:**
- 종료 시 최대 대기 시간 (초)
- 60초 안에 작업이 끝나지 않으면 강제 종료

---

## 스레드 풀 크기 설정 가이드

### 작업 유형에 따른 권장 크기

#### 1. CPU 집약적 작업 (계산, 데이터 처리)
```java
// CPU 코어 수 기준
int cpuCount = Runtime.getRuntime().availableProcessors();
executor.setCorePoolSize(cpuCount);
executor.setMaxPoolSize(cpuCount * 2);
```

**이유:** CPU를 많이 사용하므로 CPU 코어 수만큼만 병렬 실행

#### 2. I/O 집약적 작업 (네트워크, 파일, DB)
```java
// CPU 코어 수 * 2 이상
int cpuCount = Runtime.getRuntime().availableProcessors();
executor.setCorePoolSize(cpuCount * 2);
executor.setMaxPoolSize(cpuCount * 4);
```

**이유:** I/O 대기 시간이 많아 더 많은 스레드 활용 가능

#### 3. 혼합형 작업
```java
executor.setCorePoolSize(10);
executor.setMaxPoolSize(20);
```

**이유:** 부하 테스트를 통해 최적값 찾기

### 프로젝트별 설정 예시

#### 이 프로젝트의 설정 (이메일 발송)
```java
// 이메일 발송: I/O 집약적 (네트워크 대기 시간 많음)
executor.setCorePoolSize(5);   // 동시에 5개까지 발송
executor.setMaxPoolSize(10);   // 최대 10개까지 확장
executor.setQueueCapacity(50); // 50개까지 대기 가능
```

**계산 근거:**
- 이메일 발송 시 네트워크 대기 시간이 대부분
- 3개 작업을 동시에 처리하려면 CorePoolSize ≥ 3
- 여유있게 5로 설정
- 트래픽 증가 시 10까지 확장 가능

---

## API 엔드포인트

### 사용자 관리 API

#### 1. 동기 방식 사용자 생성 (약 3초 소요)
```bash
POST http://localhost:8080/api/users/sync
Content-Type: application/json

{
  "name": "홍길동",
  "email": "hong@example.com"
}
```
- 응답 시간: ~3초 (이메일 전송 대기)

#### 2. 비동기 방식 사용자 생성 (즉시 반환)
```bash
POST http://localhost:8080/api/users/async
Content-Type: application/json

{
  "name": "김철수",
  "email": "kim@example.com"
}
```
- 응답 시간: 즉시 (이메일은 백그라운드 처리)

#### 3. 비동기 사용자 조회
```bash
GET http://localhost:8080/api/users/1/async
```

#### 4. 모든 사용자 조회 (비동기)
```bash
GET http://localhost:8080/api/users/async
```

### 주문 관리 API

#### 1. 주문 생성 (비동기 처리)
```bash
POST http://localhost:8080/api/orders
Content-Type: application/json

{
  "productName": "노트북",
  "amount": 1500000,
  "customerEmail": "customer@example.com"
}
```
- 주문 즉시 생성, 처리는 백그라운드 (~5초)

#### 2. 배치 주문 생성 (병렬 처리)
```bash
POST http://localhost:8080/api/orders/batch
Content-Type: application/json

[
  {
    "productName": "노트북",
    "amount": 1500000,
    "customerEmail": "customer1@example.com"
  },
  {
    "productName": "마우스",
    "amount": 50000,
    "customerEmail": "customer2@example.com"
  }
]
```
- 여러 주문을 병렬로 처리

#### 3. 주문 조회
```bash
GET http://localhost:8080/api/orders/1
```

#### 4. 상태별 주문 조회
```bash
GET http://localhost:8080/api/orders/status/COMPLETED
```

### 데모 API - 성능 비교

#### 1. 동기 방식 (순차 실행 - 약 9초)
```bash
GET http://localhost:8080/api/demo/sync?email=test@example.com
```
- 3개 작업 순차 실행 (~9초)

#### 2. 비동기 방식 (병렬 실행 - 약 3초)
```bash
GET http://localhost:8080/api/demo/async?email=test@example.com
```
- 3개 작업 병렬 실행 (~3초)

#### 3. 대량 이메일 전송
```bash
GET http://localhost:8080/api/demo/bulk?count=5
```

#### 4. 비동기 체이닝
```bash
GET http://localhost:8080/api/demo/chain?email=test@example.com
```
- CompletableFuture 체이닝 예제

---

## 실행 방법

### 1. 프로젝트 빌드
```bash
cd async-communication-example
./gradlew build
```

### 2. 애플리케이션 실행
```bash
./gradlew bootRun
```

또는

```bash
java -jar build/libs/async-communication-example-0.0.1-SNAPSHOT.jar
```

### 3. H2 콘솔 접속
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:asyncdb
Username: sa
Password: (비워두기)
```

---

## 테스트 실행

### 성능 비교 테스트
```bash
# 모든 테스트 실행
./gradlew test

# 성능 테스트만 실행
./gradlew test --tests SyncVsAsyncPerformanceTest

# 컨트롤러 테스트만 실행
./gradlew test --tests AsyncDemoControllerTest
```

### 테스트 결과 예시
```
============================================================
📊 성능 비교 결과
============================================================
작업 개수: 10개 (각 작업당 1초 소요)
동기 방식 소요 시간:   10050ms (10.05초)
비동기 방식 소요 시간: 1028ms (1.028초)
단축된 시간: 9022ms (9.022초)
성능 개선율: 89.77%
============================================================
```

자세한 테스트 가이드는 `TEST_GUIDE.md` 참고

---

## 성능 비교

### 동기 방식
- 사용자 생성 + 이메일 전송: ~3초
- 3개 이메일 순차 전송: ~9초
- 10개 작업 순차 실행: ~10초

### 비동기 방식
- 사용자 생성 (이메일 백그라운드): 즉시
- 3개 이메일 병렬 전송: ~3초 (66% 개선)
- 10개 작업 병렬 실행: ~1초 (90% 개선)

---

## 로그 확인

애플리케이션 실행 시 콘솔에서 스레드 이름을 확인할 수 있습니다:

```
[async-1] Processing order: 123      ← taskExecutor 스레드
[async-2] Processing order: 456      ← taskExecutor 스레드
[email-1] Sending email to: test@example.com  ← emailExecutor 스레드
[email-2] Sending email to: admin@example.com ← emailExecutor 스레드
[http-nio-8080-exec-1] Request received        ← 메인 요청 처리 스레드
```

**스레드별 역할:**
- `[async-*]`: 일반 비동기 작업 (주문 처리 등)
- `[email-*]`: 이메일 전송 전용
- `[http-nio-*]`: HTTP 요청 처리 (메인 스레드)

---

## 운영 환경 고려사항

### 1. 스레드 풀 크기 조정

#### 모니터링 지표
```java
ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) applicationContext.getBean("taskExecutor");

int activeCount = executor.getActiveCount();           // 현재 작업 중인 스레드 수
int poolSize = executor.getPoolSize();                 // 현재 스레드 풀 크기
int queueSize = executor.getThreadPoolExecutor()
                        .getQueue().size();            // 대기 중인 작업 수
```

#### 조정 기준
1. **activeCount가 항상 CorePoolSize에 가까움** → CorePoolSize 증가 고려
2. **queueSize가 자주 높음** → CorePoolSize 또는 MaxPoolSize 증가
3. **스레드가 거의 사용되지 않음** → CorePoolSize 감소 (리소스 절약)

### 2. 타임아웃 설정

```java
// CompletableFuture 타임아웃
CompletableFuture<String> future = asyncService.longRunningTask();

try {
    String result = future.get(10, TimeUnit.SECONDS);  // 10초 타임아웃
} catch (TimeoutException e) {
    log.error("작업이 10초 내에 완료되지 않음");
    future.cancel(true);  // 작업 취소
}
```

### 3. 예외 처리 전략

```java
@Async("taskExecutor")
public CompletableFuture<Order> processOrder(Long orderId) {
    try {
        // 작업 수행
        Order order = orderRepository.findById(orderId).orElseThrow();
        // 처리 로직...
        return CompletableFuture.completedFuture(order);

    } catch (Exception e) {
        log.error("주문 처리 실패: {}", orderId, e);

        // 실패한 Future 반환
        return CompletableFuture.failedFuture(e);
    }
}

// 호출 측에서 예외 처리
asyncService.processOrder(orderId)
    .exceptionally(ex -> {
        log.error("주문 처리 중 예외 발생", ex);
        return null;  // 기본값 반환
    })
    .thenAccept(order -> {
        if (order != null) {
            log.info("주문 처리 완료: {}", order);
        }
    });
```

### 4. 데드락 방지

```java
// ❌ 나쁜 예: 비동기 메서드에서 다른 비동기 메서드 결과를 동기적으로 대기
@Async("taskExecutor")
public void badExample() {
    CompletableFuture<String> future = anotherAsyncMethod();
    String result = future.join();  // ← 데드락 발생 가능!
}

// ✅ 좋은 예: 체이닝 사용
@Async("taskExecutor")
public CompletableFuture<String> goodExample() {
    return anotherAsyncMethod()
        .thenApply(result -> {
            // 결과 처리
            return processResult(result);
        });
}
```

### 5. 리소스 제한

```yaml
# application.yml
spring:
  task:
    execution:
      pool:
        # 전역 스레드 풀 설정
        core-size: 5
        max-size: 10
        queue-capacity: 100
        keep-alive: 60s
```

### 6. 모니터링 설정

```java
@Configuration
public class AsyncConfig {

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // ... 설정 ...

        // 거부 정책 설정 (큐가 가득 찰 때)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // CallerRunsPolicy: 호출한 스레드에서 직접 실행
        // AbortPolicy: RejectedExecutionException 발생 (기본값)
        // DiscardPolicy: 조용히 무시
        // DiscardOldestPolicy: 가장 오래된 작업 버리고 새 작업 추가

        return executor;
    }
}
```

### 7. 프로덕션 권장 설정

```java
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    // CPU 코어 수 기반 자동 설정
    int cpuCount = Runtime.getRuntime().availableProcessors();

    executor.setCorePoolSize(cpuCount * 2);
    executor.setMaxPoolSize(cpuCount * 4);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("app-async-");

    // 종료 시 작업 완료 대기
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);

    // 거부 정책: 호출 스레드에서 실행 (데이터 손실 방지)
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

    executor.initialize();
    return executor;
}
```

### 8. 성능 테스트

**부하 테스트 필수 항목:**
1. 동시 요청 수 증가 테스트
2. 스레드 풀 포화 상태 테스트
3. 메모리 사용량 모니터링
4. 응답 시간 측정

**도구:**
- JMeter: HTTP 부하 테스트
- VisualVM: JVM 모니터링
- Actuator: Spring Boot 메트릭

### 9. 환경별 설정 분리

```yaml
# application-dev.yml (개발 환경)
async:
  task-executor:
    core-pool-size: 2
    max-pool-size: 4
    queue-capacity: 10

# application-prod.yml (운영 환경)
async:
  task-executor:
    core-pool-size: 10
    max-pool-size: 20
    queue-capacity: 200
```

```java
@Configuration
public class AsyncConfig {

    @Value("${async.task-executor.core-pool-size:5}")
    private int corePoolSize;

    @Value("${async.task-executor.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${async.task-executor.queue-capacity:100}")
    private int queueCapacity;

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        // ...
        return executor;
    }
}
```

---

## 주의사항

### 1. @Async 프록시 이슈

```java
// ❌ 동작하지 않음: 같은 클래스 내부 호출
@Service
public class UserService {

    public void createUser() {
        // 비동기로 동작하지 않음! (프록시를 거치지 않음)
        this.sendEmail();
    }

    @Async
    public void sendEmail() {
        // ...
    }
}

// ✅ 올바른 방법: 다른 클래스로 분리
@Service
public class UserService {
    private final EmailService emailService;

    public void createUser() {
        // 비동기로 동작함! (프록시를 거침)
        emailService.sendEmail();
    }
}

@Service
public class EmailService {
    @Async
    public void sendEmail() {
        // ...
    }
}
```

### 2. 예외 처리

비동기 메서드의 예외는 호출자에게 전파되지 않으므로:
- CompletableFuture 사용 시 `exceptionally()` 처리
- void 반환 시 메서드 내부에서 try-catch 필수

### 3. 트랜잭션 주의

```java
// ❌ 위험: 비동기 메서드에서 트랜잭션
@Async
@Transactional  // 별도 스레드에서 실행되므로 트랜잭션 분리됨!
public void asyncMethod() {
    // 트랜잭션이 예상대로 동작하지 않을 수 있음
}

// ✅ 권장: 비동기 호출 전에 트랜잭션 완료
@Transactional
public void syncMethod() {
    // DB 작업 완료
    repository.save(entity);

    // 트랜잭션 커밋 후 비동기 호출
    asyncService.doSomething();
}
```

### 4. 스레드 풀 크기

- 너무 작으면: 작업 대기 시간 증가
- 너무 크면: 메모리 낭비, 컨텍스트 스위칭 비용 증가
- **권장**: 부하 테스트로 최적값 찾기

---

## 트러블슈팅

### 문제 1: 비동기가 동작하지 않음

**증상:** `@Async` 메서드가 동기로 실행됨

**원인 및 해결:**
1. `@EnableAsync` 누락 → Application 클래스에 추가
2. 같은 클래스 내부 호출 → 다른 클래스로 분리
3. 메서드가 public이 아님 → public으로 변경
4. final 메서드 → final 제거

### 문제 2: 테스트가 예상보다 느림

**증상:** 3개 작업 병렬 실행인데 6초 소요

**원인:** `CorePoolSize`가 작업 개수보다 작음
```java
CorePoolSize = 2  // ← 2개만 동시 실행 가능
작업 개수 = 3     // ← 1개는 대기
```

**해결:** `CorePoolSize`를 작업 개수 이상으로 증가
```java
executor.setCorePoolSize(5);  // 3개 이상으로 설정
```

### 문제 3: RejectedExecutionException 발생

**원인:** 스레드 풀이 포화 상태

**해결:**
1. `CorePoolSize` 또는 `MaxPoolSize` 증가
2. `QueueCapacity` 증가
3. 거부 정책 변경
```java
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
```

### 문제 4: 메모리 부족

**원인:** 너무 많은 스레드 또는 큐에 작업이 쌓임

**해결:**
1. 스레드 수 감소
2. 큐 크기 제한
3. 타임아웃 설정으로 오래된 작업 정리

---

## 학습 포인트

✅ Spring의 `@Async`를 활용한 비동기 처리
✅ `CompletableFuture`를 이용한 비동기 결과 처리
✅ 커스텀 `ThreadPoolTaskExecutor` 설정 및 튜닝
✅ 동기 vs 비동기 성능 비교 (최대 90% 개선)
✅ 병렬 처리와 체이닝 패턴
✅ 비동기 환경에서의 예외 처리
✅ 스레드 풀 동작 원리 (`CorePoolSize`, `MaxPoolSize`, `QueueCapacity`)
✅ 운영 환경 고려사항 (모니터링, 타임아웃, 리소스 관리)

---

## 참고 자료

- [Spring Framework 공식 문서 - @Async](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async)
- [Java CompletableFuture 가이드](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)
- [ThreadPoolExecutor 파라미터 가이드](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadPoolExecutor.html)

---

## 라이센스

MIT License
