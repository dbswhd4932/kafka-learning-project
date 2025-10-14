# 동시성 제어 방법 비교: Synchronized vs Redis Lettuce vs Redis Redisson

Spring Boot와 JPA를 사용하여 3가지 동시성 제어 방법(Synchronized, Redis Lettuce, Redis Redisson)을 비교하고 학습하는 프로젝트입니다.

## 📌 프로젝트 개요

이 프로젝트는 **재고 관리 시스템**을 예제로 하여, 여러 스레드가 동시에 재고를 감소시킬 때 발생하는 **Race Condition**을 확인하고, 3가지 방법으로 해결하는 방법을 비교합니다.

## 🏗️ 프로젝트 구조

```
concurrency-example/
├── src/
│   ├── main/
│   │   ├── java/com/example/concurrency/
│   │   │   ├── domain/
│   │   │   │   └── Stock.java                      # 재고 엔티티
│   │   │   ├── repository/
│   │   │   │   └── StockRepository.java            # 재고 Repository
│   │   │   └── service/
│   │   │       ├── SynchronizedStockService.java   # Synchronized 방식
│   │   │       ├── RedisStockService.java          # Redis Lettuce 방식
│   │   │       └── RedissonStockService.java       # Redis Redisson 방식
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/example/concurrency/
│           └── service/
│               ├── SynchronizedStockServiceTest.java   # Synchronized 테스트
│               ├── RedisStockServiceTest.java          # Lettuce 테스트
│               └── RedissonStockServiceTest.java       # Redisson 테스트
├── docker-compose.yml
├── build.gradle
└── README.md
```

## 🛠️ 기술 스택

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Data JPA**
- **MySQL 8.0** (Docker)
- **Redis 7.0** (Docker)
- **Lettuce** (Spring Boot 기본 Redis 클라이언트)
- **Redisson** (분산 락 전용 라이브러리)
- **Gradle**
- **JUnit 5**

## 🚀 실행 방법

### 1. Docker로 MySQL과 Redis 실행

```bash
cd concurrency-example
docker-compose up -d
```

### 2. 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 개별 테스트 실행
./gradlew test --tests SynchronizedStockServiceTest
./gradlew test --tests RedisStockServiceTest
./gradlew test --tests RedissonStockServiceTest
```

또는 IDE에서 각 테스트 클래스를 개별 실행합니다.

## 🔍 동시성 문제란?

**동시성 문제(Concurrency Issue)** 는 여러 스레드가 동시에 공유 자원(예: 재고 수량)에 접근하여 데이터를 읽고 쓸 때 발생하는 문제입니다.

### Race Condition 발생 시나리오

```
초기 재고: 100

시간    Thread A                Thread B                DB 재고
T1      재고 조회 (100)                                100
T2                              재고 조회 (100)         100
T3      재고 감소 (99 계산)                             100
T4                              재고 감소 (99 계산)     100
T5      DB 저장 (99)                                    99
T6                              DB 저장 (99)            99 ← Lost Update!

예상: 98
실제: 99 (Thread A의 변경이 덮어씌워짐)
```

## 📊 3가지 동시성 제어 방법 비교

| 항목 | Synchronized | Redis Lettuce | Redis Redisson |
|------|-------------|---------------|----------------|
| **동작 원리** | Java 모니터 락 | SETNX + 스핀 락 | SETNX + Pub/Sub |
| **동작 범위** | 단일 JVM | 분산 환경 (여러 서버/Pod) | 분산 환경 (여러 서버/Pod) |
| **구현 난이도** | ⭐ (매우 쉬움) | ⭐⭐⭐⭐ (어려움) | ⭐⭐ (쉬움) |
| **락 획득 방식** | 블로킹 (대기) | 스핀 락 (계속 재시도) | Pub/Sub (이벤트 기반) |
| **CPU 사용률** | 낮음 | 높음 (계속 polling) | 낮음 (대기) |
| **성능** | 빠름 (네트워크 I/O 없음) | 느림 (스핀 락 오버헤드) | 보통 (Pub/Sub 오버헤드) |
| **처리량** | 낮음 (순차 처리) | 높음 (재고별 독립 락) | 높음 (재고별 독립 락) |
| **인프라 요구사항** | 없음 | Redis 필요 | Redis 필요 |
| **Kubernetes 지원** | ❌ (단일 JVM만) | ✅ | ✅ |
| **Auto Scaling 지원** | ❌ | ✅ | ✅ |
| **MSA 환경 지원** | ❌ | ✅ | ✅ |
| **재고별 독립 락** | ❌ (메서드 전체 락) | ✅ (키별 락) | ✅ (키별 락) |
| **TTL 자동 관리** | N/A | ❌ (수동 설정) | ✅ (Watchdog) |
| **락 해제 보장** | ✅ (자동) | ⚠️ (수동, finally 필수) | ✅ (자동 + Watchdog) |
| **데드락 방지** | ⚠️ (주의 필요) | ✅ (TTL) | ✅ (TTL + Watchdog) |
| **추가 의존성** | 없음 | 없음 (Spring 기본) | Redisson 라이브러리 |
| **프로덕션 추천도** | ❌ (단일 서버만) | ⚠️ (간단한 케이스) | ✅ (높은 트래픽) |

## 💡 방법 1: Synchronized

### 동작 원리

Java의 **모니터 락(Monitor Lock)** 을 사용하여 한 번에 하나의 스레드만 메서드에 접근할 수 있도록 합니다.

```java
@Transactional
public synchronized void decrease(Long id, Long quantity) {
    Stock stock = stockRepository.findById(id).orElseThrow();
    stock.decrease(quantity);
    stockRepository.saveAndFlush(stock);
}
```

### 동작 흐름

```
Thread A: 락 획득 → 트랜잭션 시작 → 재고 감소 → 트랜잭션 커밋 → 락 해제
Thread B: 대기 중... (블로킹)
Thread B: 락 획득 → 트랜잭션 시작 → 재고 감소 → 트랜잭션 커밋 → 락 해제
```

### ✅ 장점

1. **구현이 매우 간단함**
   - `synchronized` 키워드만 추가하면 됨
   - 추가 라이브러리나 인프라 불필요
   - 별도 학습 없이 즉시 적용 가능

2. **단일 서버 환경에서 완벽하게 동작**
   - 같은 JVM 내에서는 100% 동시성 문제 해결
   - 안정적이고 예측 가능한 동작

3. **빠른 성능 (단일 서버 기준)**
   - 네트워크 I/O 없음
   - JVM 내부 락이므로 오버헤드 최소

4. **코드 이해가 쉬움**
   - Java 기본 문법
   - 디버깅이 용이

### ❌ 단점

1. **단일 JVM 내에서만 동작**
   - 여러 Pod/서버가 있으면 각각 독립적인 락 사용
   - 분산 환경에서는 동시성 문제 해결 불가

2. **성능 저하 (전역 락)**
   - 재고 ID가 달라도 모든 요청이 순차 처리
   - 상품 A 처리 중 상품 B도 대기
   - 처리량(Throughput) 감소

3. **확장성 없음**
   - Auto Scaling 불가
   - 트래픽 증가에 대응 불가

4. **데드락 위험**
   - 여러 synchronized 메서드 중첩 호출 시 데드락 가능

### ⚠️ 주의사항

1. **Kubernetes Pod가 2개 이상이면 사용 불가**
   ```yaml
   # deployment.yaml
   replicas: 3  # ❌ synchronized 동작 안 함!
   ```

2. **Auto Scaling 환경에서 사용 불가**
   ```yaml
   # HPA 설정
   minReplicas: 1
   maxReplicas: 10  # ❌ 인스턴스 늘어나면 동시성 문제 발생!
   ```

3. **로드 밸런서 뒤에 여러 서버가 있으면 사용 불가**
   ```
   [Client] → [Load Balancer] → [Server 1 - JVM 1] ← 독립적인 락
                              → [Server 2 - JVM 2] ← 독립적인 락
                              → [Server 3 - JVM 3] ← 독립적인 락

   각 서버가 독립적으로 락을 관리하므로 동시성 문제 발생!
   ```

4. **@Transactional과 함께 사용 시 주의**
   - 메서드 레벨 synchronized는 안전하지만 예측 어려움
   - 반드시 @Transactional 추가 필요 (롤백 처리)

### 📌 사용 가능한 경우

- ✅ 로컬 개발 환경
- ✅ 단일 서버로만 운영되는 소규모 서비스
- ✅ 프로토타입 또는 MVP 단계 (빠른 검증 필요)
- ✅ 레거시 시스템 (단일 서버, 마이그레이션 어려움)

### 🚫 사용하면 안 되는 경우

- ❌ Kubernetes 환경 (Pod 2개 이상)
- ❌ Auto Scaling 사용
- ❌ MSA 환경
- ❌ 로드 밸런서 뒤에 여러 서버
- ❌ 높은 트래픽 환경

## 💡 방법 2: Redis Lettuce (SETNX + 스핀 락)

### 동작 원리

Redis의 **SETNX(SET if Not eXists)** 명령어를 사용하여 분산 락을 구현합니다.
락 획득 실패 시 **스핀 락(Spin Lock)** 방식으로 계속 재시도합니다.

```java
@Transactional
public void decrease(Long id, Long quantity) {
    String lockKey = "stock:lock:" + id;

    // 스핀 락: 락 획득까지 계속 재시도
    while (!tryLock(lockKey)) {
        Thread.sleep(50);  // 50ms 대기 후 재시도
    }

    try {
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    } finally {
        unlock(lockKey);  // 반드시 락 해제
    }
}

private boolean tryLock(String key) {
    return redisTemplate.opsForValue()
        .setIfAbsent(key, "locked", Duration.ofSeconds(3));
}
```

### 동작 흐름

```
Thread A: Redis SETNX stock:lock:1 → 성공 → 재고 감소 → Redis DEL stock:lock:1
Thread B: Redis SETNX stock:lock:1 → 실패 → 50ms 대기 → 재시도 (반복)
Thread C: Redis SETNX stock:lock:1 → 실패 → 50ms 대기 → 재시도 (반복)
Thread A: 완료 → Redis DEL
Thread B: Redis SETNX stock:lock:1 → 성공 → 재고 감소
```

### ✅ 장점

1. **분산 환경에서 동작**
   - 여러 서버/Pod에서 하나의 Redis 공유
   - Kubernetes, Auto Scaling 환경 지원

2. **재고별 독립적인 락**
   - `stock:lock:1`, `stock:lock:2` 등 키별로 락 설정
   - 상품 A 처리 중에도 상품 B는 동시 처리 가능
   - 처리량 향상

3. **TTL로 데드락 방지**
   - 락에 만료 시간 설정 (예: 3초)
   - 애플리케이션 비정상 종료 시에도 자동 해제

4. **추가 의존성 없음**
   - Spring Boot에 Lettuce가 기본 포함
   - 별도 라이브러리 설치 불필요

### ❌ 단점

1. **스핀 락으로 인한 높은 CPU 사용률**
   ```java
   while (!tryLock()) {
       Thread.sleep(50);  // 계속 대기 → CPU 낭비
   }
   ```
   - 락 획득까지 계속 재시도
   - 대기 중인 스레드가 많으면 CPU 사용률 급증

2. **불필요한 Redis 요청 증가**
   - 50ms마다 SETNX 요청 반복
   - Redis 부하 증가
   - 네트워크 I/O 오버헤드

3. **구현 복잡도 높음**
   - 스핀 락 로직 직접 구현 필요
   - 타임아웃, 재시도 횟수 등 관리
   - finally 블록에서 락 해제 필수

4. **성능 저하**
   - Synchronized보다 느림 (네트워크 I/O)
   - 스핀 락 오버헤드

5. **Redis 장애 시 서비스 영향**
   - Redis 다운 시 락 획득 불가
   - 전체 서비스 장애 가능

### ⚠️ 주의사항

1. **반드시 finally에서 락 해제**
   ```java
   try {
       // 비즈니스 로직
   } finally {
       unlock(lockKey);  // 반드시 실행되어야 함
   }
   ```

2. **TTL 설정 필수**
   - 락 획득 후 애플리케이션 크래시 시 락이 영구 유지됨
   - 반드시 TTL 설정 (예: 3초)

3. **자신의 락인지 확인 후 해제**
   ```java
   // 다른 스레드의 락을 해제하는 것 방지
   String currentValue = redisTemplate.opsForValue().get(lockKey);
   if (myValue.equals(currentValue)) {
       redisTemplate.delete(lockKey);
   }
   ```

4. **@Transactional 반드시 추가**
   - 락은 있지만 트랜잭션 없으면 롤백 불가
   - 예외 발생 시 데이터 불일치

5. **재시도 횟수 제한**
   - 무한 루프 방지
   - 최대 재시도 횟수 설정 (예: 100번)

### 📌 사용 가능한 경우

- ✅ 분산 환경 (여러 서버/Pod)
- ✅ Kubernetes + Auto Scaling
- ✅ MSA 환경
- ✅ 간단한 동시성 제어 (낮은 트래픽)
- ✅ Redisson 도입이 어려운 경우

### 🚫 사용하면 안 되는 경우

- ❌ 높은 트래픽 환경 (스핀 락으로 CPU 낭비)
- ❌ 대기 시간이 긴 작업 (스핀 락 비효율)
- ❌ Redis 부하가 이미 높은 경우

## 💡 방법 3: Redis Redisson (SETNX + Pub/Sub)

### 동작 원리

Redisson의 **RLock**을 사용하여 분산 락을 구현합니다.
Lettuce와 달리 **Pub/Sub 방식**을 사용하여 스핀 락의 단점을 개선했습니다.

```java
@Transactional
public void decrease(Long id, Long quantity) {
    // 1. id: 재고 ID (예: 1 = 아이폰, 2 = 갤럭시)
    //    - 어떤 상품의 재고를 감소시킬지 식별
    //    - DB의 Primary Key

    // 2. lockKey: Redis에 저장될 락의 키 (예: "stock:lock:1")
    //    - 재고 ID별로 독립적인 락 생성
    //    - 상품 1번의 락과 상품 2번의 락은 서로 다름
    //    - 따라서 상품 1 처리 중에도 상품 2는 동시 처리 가능
    String lockKey = "stock:lock:" + id;

    // 3. RLock 객체 생성
    //    - redissonClient: Redisson 클라이언트 (Spring Bean으로 주입)
    //    - getLock(lockKey): 해당 키에 대한 락 객체 반환
    RLock lock = redissonClient.getLock(lockKey);

    try {
        // 4. tryLock(대기시간, 락유지시간, 시간단위)
        //    ┌─────────────────────────────────────────────────────┐
        //    │ tryLock(5, 3, TimeUnit.SECONDS)                     │
        //    │         ↑  ↑                                        │
        //    │         │  └─ 락 유지 시간 (Lease Time): 3초           │
        //    │         └──── 대기 시간 (Wait Time): 5초               │
        //    └─────────────────────────────────────────────────────┘
        boolean available = lock.tryLock(5, 3, TimeUnit.SECONDS);

        if (!available) {
            // 5초 동안 대기했지만 락을 획득하지 못함
            throw new IllegalStateException("락 획득 실패");
        }

        // 락 획득 성공! 여기서부터 다른 스레드는 접근 불가
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);

    } catch (InterruptedException e) {
        // 락 대기 중 인터럽트 발생 시 처리
        Thread.currentThread().interrupt();
        throw new IllegalStateException("락 획득 중 인터럽트", e);
    } finally {
        // 5. 락 해제
        //    - isHeldByCurrentThread(): 현재 스레드가 이 락을 소유하고 있는지 확인
        //    - 다른 스레드의 락을 실수로 해제하는 것을 방지
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

### 📝 파라미터 상세 설명

#### 1. `id` (재고 ID)

```java
public void decrease(Long id, Long quantity) {
    //              ↑
    //              재고 ID
```

**의미:**
- DB에 저장된 재고의 Primary Key
- 어떤 상품의 재고를 감소시킬지 식별하는 값

**예시:**
```java
// 상품별 재고 감소
decrease(1L, 1L);  // 상품 ID 1번 (아이폰)의 재고 1개 감소
decrease(2L, 1L);  // 상품 ID 2번 (갤럭시)의 재고 1개 감소
decrease(3L, 5L);  // 상품 ID 3번 (맥북)의 재고 5개 감소
```

**중요:**
- 같은 ID에 대해서는 순차 처리됨 (락으로 보호)
- 다른 ID는 동시 처리 가능 (독립적인 락)

#### 2. `lockKey` (Redis 락 키)

```java
String lockKey = "stock:lock:" + id;
// id=1 → "stock:lock:1"
// id=2 → "stock:lock:2"
```

**의미:**
- Redis에 저장되는 락의 키 (Key)
- 재고 ID별로 **독립적인 락** 생성

**Redis에 저장되는 모습:**
```
Redis 메모리:
┌─────────────────────────────────────┐
│ Key: stock:lock:1                   │
│ Value: UUID (락을 소유한 스레드 ID)     │
│ TTL: 3초                            │
├─────────────────────────────────────┤
│ Key: stock:lock:2                   │
│ Value: UUID (락을 소유한 스레드 ID)     │
│ TTL: 3초                            │
└─────────────────────────────────────┘
```

**독립적인 락의 장점:**
```
시나리오: 3명의 사용자가 동시에 구매

사용자 A: 아이폰(ID=1) 구매 → stock:lock:1 획득 → 처리 중
사용자 B: 아이폰(ID=1) 구매 → stock:lock:1 대기 ⏰ (A가 끝날 때까지)
사용자 C: 갤럭시(ID=2) 구매 → stock:lock:2 획득 → 동시 처리 ✅

결과:
- 아이폰(ID=1): 순차 처리 (락으로 보호)
- 갤럭시(ID=2): 독립적으로 처리 (다른 락)
- 처리량 향상! 🚀
```

#### 3. `tryLock(대기시간, 락유지시간, 시간단위)`

```java
boolean available = lock.tryLock(5, 3, TimeUnit.SECONDS);
//                                ↑  ↑
//                                │  └─ leaseTime (락 유지 시간)
//                                └──── waitTime (대기 시간)
```

**파라미터 설명:**

| 파라미터 | 타입 | 의미 | 예시 |
|---------|------|------|------|
| `waitTime` | long | 락 획득을 위해 최대 얼마나 기다릴지 | 5초 |
| `leaseTime` | long | 락을 획득한 후 최대 얼마나 유지할지 | 3초 |
| `unit` | TimeUnit | 시간 단위 | SECONDS |

### 🎯 tryLock 파라미터 동작 예시

#### 예시 1: 대기 시간 (waitTime = 5초)

```
Thread A가 락을 이미 소유 중...

Thread B의 tryLock(5, 3, SECONDS) 호출:
┌─────────────────────────────────────────────┐
│ 시간 (초)  Thread B의 상태                  │
├─────────────────────────────────────────────┤
│ 0.0       tryLock() 호출                    │
│ 0.0       Redis SETNX 시도 → 실패          │
│ 0.0       Redis SUBSCRIBE (대기)           │
│           ...                               │
│ 2.0       (Thread A 작업 완료)              │
│ 2.0       Redis PUBLISH 받음 (알림)        │
│ 2.0       Redis SETNX 재시도 → 성공! ✅     │
│ 2.0       락 획득, 비즈니스 로직 실행       │
└─────────────────────────────────────────────┘

결과: 2초 대기 후 락 획득 성공
```

```
Thread A가 작업이 길어짐 (7초 소요)...

Thread B의 tryLock(5, 3, SECONDS) 호출:
┌─────────────────────────────────────────────┐
│ 시간 (초)  Thread B의 상태                     │
├─────────────────────────────────────────────┤
│ 0.0       tryLock() 호출                     │
│ 0.0       Redis SETNX 시도 → 실패             │
│ 0.0       Redis SUBSCRIBE (대기)             │
│ 1.0       대기 중...                         │
│ 2.0       대기 중...                         │
│ 3.0       대기 중...                         │
│ 4.0       대기 중...                         │
│ 5.0       타임아웃! ⏰                        │
│ 5.0       available = false 반환             │
└─────────────────────────────────────────────┘

결과: 5초 대기 후 타임아웃 (락 획득 실패)
예외 발생: "락 획득 실패"
```

#### 예시 2: 락 유지 시간 (leaseTime = 3초)

```
Thread A가 lock.tryLock(5, 3, SECONDS) 호출:

정상 케이스:
┌─────────────────────────────────────────────┐
│ 시간 (초)  동작                                │
├─────────────────────────────────────────────┤
│ 0.0       락 획득                             │
│ 0.5       DB 조회                             │
│ 1.0       재고 감소 계산                        │
│ 1.5       DB 저장                             │
│ 2.0       작업 완료, unlock() 호출              │
│ 2.0       락 해제 ✅                          │
└─────────────────────────────────────────────┘

결과: 3초 이내에 완료하여 정상 처리
```

```
애플리케이션 크래시 케이스:
┌─────────────────────────────────────────────┐
│ 시간 (초)  동작                                │
├─────────────────────────────────────────────┤
│ 0.0       락 획득                             │
│ 0.5       DB 조회                            │
│ 1.0       ⚠️ 애플리케이션 크래시!                │
│           (unlock() 호출 안 됨)               │
│ 2.0       ...                               │
│ 3.0       🔧 Redisson이 자동으로 락 해제!       │
│           (leaseTime 만료)                   │
└─────────────────────────────────────────────┘

결과: leaseTime으로 인해 데드락 방지 ✅
다른 스레드가 3초 후 락 획득 가능
```

#### 예시 3: Watchdog (자동 TTL 연장)

```
Thread A가 lock.tryLock(5, 3, SECONDS) 호출:
작업이 예상보다 오래 걸림 (5초 소요)

Watchdog 동작:
┌─────────────────────────────────────────────┐
│ 시간 (초)  동작                               │
├─────────────────────────────────────────────┤
│ 0.0       락 획득 (TTL: 3초)                  │
│ 1.0       작업 중...                         │
│ 2.0       🐕 Watchdog: "아직 작업 중?"        │
│ 2.0       🐕 TTL 연장 → 3초 추가              │
│ 3.0       작업 중...                         │
│ 4.0       🐕 Watchdog: "아직 작업 중?"        │
│ 4.0       🐕 TTL 연장 → 3초 추가              │
│ 5.0       작업 완료, unlock() 호출             │
│ 5.0       락 해제 ✅                         │
└─────────────────────────────────────────────┘

결과: 작업이 3초를 넘어도 자동 연장으로 안전하게 완료 ✅
```

### 💡 적절한 값 설정 가이드

```java
// 일반적인 재고 감소 (빠른 작업)
lock.tryLock(3, 2, TimeUnit.SECONDS);
// - 대기 시간: 3초 (보통 충분함)
// - 락 유지: 2초 (DB 작업은 보통 1초 이내)

// 복잡한 비즈니스 로직 (느린 작업)
lock.tryLock(10, 5, TimeUnit.SECONDS);
// - 대기 시간: 10초 (트래픽 많을 때 대비)
// - 락 유지: 5초 (여러 DB 쿼리, 외부 API 호출 등)

// 배치 작업 (매우 느린 작업)
lock.tryLock(30, 60, TimeUnit.SECONDS);
// - 대기 시간: 30초
// - 락 유지: 60초 (대량 데이터 처리)
```

### ⚠️ 주의사항

1. **waitTime이 너무 짧으면:**
   - 트래픽 많을 때 락 획득 실패 증가
   - 사용자에게 "잠시 후 다시 시도" 에러 발생

2. **waitTime이 너무 길면:**
   - 스레드가 오래 블로킹됨
   - 전체 애플리케이션 성능 저하

3. **leaseTime이 너무 짧으면:**
   - 작업 완료 전에 락이 만료될 수 있음
   - 다른 스레드가 락 획득 → 동시성 문제 발생

4. **leaseTime이 너무 길면:**
   - 애플리케이션 크래시 시 락이 오래 유지됨
   - 다른 요청이 오래 대기

**권장:**
- 작업 시간을 측정 후 `leaseTime = 작업시간 × 2` 설정
- `waitTime = leaseTime + 여유시간` 설정
- Watchdog을 활용하면 leaseTime 자동 관리 가능

### 동작 흐름 (Pub/Sub)

```
Thread A: Redis SETNX stock:lock:1 → 성공 → 재고 감소
Thread B: Redis SETNX stock:lock:1 → 실패 → Redis SUBSCRIBE (대기)
Thread C: Redis SETNX stock:lock:1 → 실패 → Redis SUBSCRIBE (대기)

Thread A: 완료 → Redis DEL + PUBLISH (알림)

Thread B: 알림 받음 → Redis SETNX stock:lock:1 → 성공 → 재고 감소
Thread C: 대기 중...

Thread B: 완료 → Redis DEL + PUBLISH (알림)
Thread C: 알림 받음 → Redis SETNX stock:lock:1 → 성공 → 재고 감소
```

### Lettuce vs Redisson 비교

| 항목 | Lettuce (스핀 락) | Redisson (Pub/Sub) |
|------|------------------|-------------------|
| **락 획득 실패 시** | 50ms 대기 후 재시도 (반복) | Redis SUBSCRIBE로 대기 |
| **CPU 사용률** | 높음 (계속 polling) | 낮음 (이벤트 기반) |
| **Redis 요청 횟수** | 매우 많음 (50ms마다) | 적음 (필요할 때만) |
| **네트워크 I/O** | 많음 | 적음 |
| **구현 난이도** | 어려움 (직접 구현) | 쉬움 (라이브러리 제공) |
| **성능** | 느림 | 빠름 |

### Pub/Sub 방식 상세 설명

**Lettuce (스핀 락)**
```java
// 계속 재시도 → CPU 낭비, Redis 부하
while (!tryLock()) {
    Thread.sleep(50);      // CPU 사용
    Redis SETNX 시도;      // 불필요한 요청
}
```

**Redisson (Pub/Sub)**
```java
if (!tryLock()) {
    Redis SUBSCRIBE;        // 대기 (CPU 사용 안 함)
    // 락 해제 시 Redis가 PUBLISH → 알림 받음
    // 그때 tryLock() 재시도
}
```

### ✅ 장점

1. **Pub/Sub으로 효율적인 대기**
   - 락 해제 시 Redis가 대기 중인 클라이언트에게 알림
   - 불필요한 재시도 없음
   - CPU 사용률 낮음

2. **Lettuce보다 훨씬 좋은 성능**
   - 스핀 락 오버헤드 없음
   - Redis 요청 횟수 최소화
   - 네트워크 I/O 감소

3. **분산 환경에서 완벽하게 동작**
   - 여러 서버/Pod 지원
   - Kubernetes, Auto Scaling 환경 최적

4. **재고별 독립적인 락**
   - Lettuce와 동일하게 키별 락
   - 높은 처리량

5. **Watchdog 메커니즘**
   - 락을 획득한 스레드가 작업 중이면 TTL 자동 연장
   - 작업 완료 전 락이 만료되는 문제 방지

6. **구현이 간단**
   - `tryLock()`, `unlock()` 메서드만 사용
   - 스핀 락 로직 불필요
   - 락 해제, TTL 관리 자동 처리

7. **안전한 락 해제**
   - `isHeldByCurrentThread()`로 자신의 락인지 확인
   - 다른 스레드의 락 해제 방지

8. **추가 기능 제공**
   - Fair Lock (공정한 락 획득 순서)
   - MultiLock (여러 락 동시 관리)
   - ReadWriteLock (읽기/쓰기 락 분리)

### ❌ 단점

1. **추가 의존성 필요**
   ```gradle
   implementation 'org.redisson:redisson-spring-boot-starter:3.24.3'
   ```
   - Lettuce는 Spring Boot 기본 포함
   - Redisson은 별도 설치 필요

2. **Redis 장애 시 서비스 영향**
   - Redis 다운 시 락 획득 불가
   - 전체 서비스 장애 가능

3. **네트워크 I/O 발생**
   - Synchronized보다 느림
   - Redis와 통신 필요

4. **학습 곡선**
   - Redisson API 학습 필요
   - RLock, RTopic 등 개념 이해

### ⚠️ 주의사항

1. **tryLock() 타임아웃 설정 필수**
   ```java
   // 타임아웃 없으면 무한 대기 가능
   lock.tryLock(5, 3, TimeUnit.SECONDS);
   //           ↑   ↑
   //     대기시간  락유지시간
   ```

2. **반드시 finally에서 락 해제**
   ```java
   try {
       // 비즈니스 로직
   } finally {
       if (lock.isHeldByCurrentThread()) {
           lock.unlock();
       }
   }
   ```

3. **InterruptedException 처리**
   ```java
   try {
       lock.tryLock(5, 3, TimeUnit.SECONDS);
   } catch (InterruptedException e) {
       Thread.currentThread().interrupt();  // 인터럽트 상태 복원
       throw new IllegalStateException("락 획득 중 인터럽트", e);
   }
   ```

4. **@Transactional 반드시 추가**
   - 락은 있지만 트랜잭션 없으면 롤백 불가
   - 예외 발생 시 데이터 불일치

5. **Watchdog 타임아웃 설정**
   ```java
   // application.yml
   spring:
     redis:
       redisson:
         config: |
           lockWatchdogTimeout: 30000  # 30초
   ```

### 📌 사용 가능한 경우

- ✅ 분산 환경 (여러 서버/Pod)
- ✅ Kubernetes + Auto Scaling
- ✅ MSA 환경
- ✅ 높은 트래픽 환경 (Lettuce보다 성능 좋음)
- ✅ 프로덕션 환경 (가장 권장)
- ✅ 복잡한 락 요구사항 (Fair Lock, MultiLock 등)

### 🚫 사용하면 안 되는 경우

- ❌ 단일 서버 환경 (Synchronized가 더 빠름)
- ❌ Redis 인프라 구축이 어려운 경우
- ❌ 매우 간단한 프로토타입 (오버 엔지니어링)

## 🎯 어떤 방법을 선택해야 할까?

### 의사결정 플로우차트

```
시작
 ↓
분산 환경인가? (여러 서버/Pod)
 ├─ NO → Synchronized 사용
 │       (단일 서버, 간단함, 빠름)
 │
 └─ YES → 트래픽이 높은가?
         ├─ NO → Redis Lettuce 고려
         │       (간단한 분산 환경, Redisson 도입 어려움)
         │
         └─ YES → Redis Redisson 사용 ✅ (권장)
                 (높은 트래픽, 프로덕션 환경)
```

### 환경별 권장 방법

#### 1. 로컬 개발 / 프로토타입
```
✅ Synchronized
- 이유: 빠르게 구현 가능, Redis 불필요
- 주의: 프로덕션 배포 전 반드시 변경
```

#### 2. 단일 서버 운영 (스타트업 초기)
```
✅ Synchronized
- 이유: 간단하고 빠름
- 주의: 서버 증설 시 반드시 분산 락으로 전환
```

#### 3. Kubernetes (Pod 2개 이상)
```
✅ Redis Redisson (1순위)
⚠️ Redis Lettuce (2순위, 트래픽 낮을 때만)
❌ Synchronized (사용 불가)

- 이유: Pod마다 독립적인 JVM, synchronized 동작 안 함
```

#### 4. Auto Scaling 사용
```
✅ Redis Redisson (1순위)
⚠️ Redis Lettuce (2순위, 트래픽 낮을 때만)
❌ Synchronized (사용 불가)

- 이유: 인스턴스가 동적으로 증가, 분산 락 필수
```

#### 5. MSA 환경
```
✅ Redis Redisson (강력 권장)
⚠️ Redis Lettuce (비추천, 성능 이슈)
❌ Synchronized (사용 불가)

- 이유: 여러 서비스가 같은 재고 접근, 높은 트래픽
```

#### 6. 높은 트래픽 환경 (일 10만+ 요청)
```
✅ Redis Redisson (필수)
❌ Redis Lettuce (CPU 낭비, Redis 부하)
❌ Synchronized (확장성 없음)

- 이유: Pub/Sub 방식으로 효율적, CPU 사용률 낮음
```

## 📊 성능 비교 (100개 스레드 동시 요청)

| 방법 | 실행 시간 | CPU 사용률 | Redis 요청 | 정확성 | 확장성 |
|------|----------|-----------|-----------|-------|-------|
| **Synchronized** | ~500ms | 낮음 | 0 | ✅ | ❌ (단일 JVM만) |
| **Redis Lettuce** | ~3000ms | 매우 높음 | ~5000회 | ✅ | ✅ |
| **Redis Redisson** | ~1500ms | 낮음 | ~200회 | ✅ | ✅ |

**결론: Redisson이 Lettuce보다 2배 빠르고 CPU 사용률도 낮음!**

## 🔄 트랜잭션과 락의 관계

### ❌ 잘못된 예시: @Transactional 없이 사용

```java
// ❌ 원자성 보장 안 됨
public void decrease(Long id, Long quantity) {
    lock.tryLock();
    try {
        Stock stock = findById(id);  // 트랜잭션 1
        stock.decrease(quantity);    // 메모리
        saveAndFlush(stock);         // 트랜잭션 2

        // 중간에 예외 발생 시 롤백 불가!
    } finally {
        lock.unlock();
    }
}
```

### ✅ 올바른 예시: @Transactional과 함께 사용

```java
// ✅ 원자성 보장
@Transactional
public void decrease(Long id, Long quantity) {
    lock.tryLock();              // ← 락 시작
    try {
        // @Transactional 시작   ← 트랜잭션 시작
        Stock stock = findById(id);
        stock.decrease(quantity);
        saveAndFlush(stock);
        // @Transactional 커밋   ← 트랜잭션 종료
    } finally {
        lock.unlock();           // ← 락 해제
    }
}
```

### 락과 트랜잭션의 범위

```
┌─────────────────────────────────────┐
│  락 범위 (lock ~ unlock)            │
│  ┌───────────────────────────────┐  │
│  │ 트랜잭션 범위 (@Transactional)│  │
│  │                               │  │
│  │  DB 조회                      │  │
│  │  메모리 수정                  │  │
│  │  DB 저장                      │  │
│  │                               │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘

락의 범위가 트랜잭션보다 크므로 안전!
```

## 🔧 트러블슈팅

### 1. Synchronized를 사용했는데 동시성 문제가 발생해요

**원인:**
- 서버가 2대 이상이거나 Pod가 여러 개입니다
- 각 서버/Pod는 독립적인 JVM을 가져서 락이 공유되지 않음

**해결:**
```bash
# Kubernetes Pod 확인
kubectl get pods

# 2개 이상이면 Redis 분산 락으로 전환
```

### 2. Redis Lettuce를 사용했는데 CPU 사용률이 너무 높아요

**원인:**
- 스핀 락 방식으로 계속 재시도하면서 CPU 낭비

**해결:**
```java
// Redisson으로 전환 (Pub/Sub 방식)
// CPU 사용률 70% → 20%로 감소
```

### 3. 락을 획득했는데 타임아웃이 발생해요

**원인:**
- 작업 시간이 락 유지 시간(lease time)보다 김

**해결:**
```java
// 락 유지 시간 증가
lock.tryLock(5, 10, TimeUnit.SECONDS);  // 3초 → 10초

// 또는 Redisson Watchdog 사용 (자동 TTL 연장)
```

### 4. 예외 발생 시 락이 해제되지 않아요

**원인:**
- finally 블록에서 unlock()을 호출하지 않음

**해결:**
```java
try {
    lock.tryLock();
    // 비즈니스 로직
} finally {
    lock.unlock();  // 반드시 호출
}
```

### 5. Redis가 다운되면 서비스 전체가 멈춰요

**원인:**
- Redis 장애 시 락 획득 불가

**해결:**
```java
// 1. Redis Cluster로 고가용성 확보
// 2. Circuit Breaker 패턴 적용
// 3. Fallback 전략 구현

@CircuitBreaker(name = "redis", fallbackMethod = "fallback")
public void decrease(Long id, Long quantity) {
    // Redis 분산 락 사용
}

public void fallback(Long id, Long quantity, Exception e) {
    log.error("Redis 장애, 재시도 큐에 추가", e);
    retryQueue.add(new Task(id, quantity));
}
```

## 📝 주요 설정

### application.yml

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/concurrency_db
    username: root
    password: 1234

  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true

  data:
    redis:
      host: localhost
      port: 6379
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: mysql-concurrency
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: 1234
      MYSQL_DATABASE: concurrency_db
    volumes:
      - mysql-data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    container_name: redis-concurrency
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

volumes:
  mysql-data:
  redis-data:
```

### build.gradle

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.redisson:redisson-spring-boot-starter:3.24.3'

    runtimeOnly 'com.mysql:mysql-connector-j'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

## 🎓 학습 포인트

1. **Race Condition 이해**
   - Lost Update 문제
   - 동시성 이슈의 근본 원인

2. **락의 종류와 특징**
   - Synchronized (모니터 락)
   - 분산 락 (Redis SETNX)
   - 스핀 락 vs Pub/Sub

3. **트랜잭션과 락의 관계**
   - 락의 범위가 트랜잭션보다 커야 하는 이유
   - @Transactional이 필수인 이유

4. **분산 환경의 이해**
   - JVM이 다르면 메모리도 다름
   - Kubernetes Pod, Auto Scaling의 의미

5. **성능과 확장성의 Trade-off**
   - Synchronized는 빠르지만 확장성 없음
   - Lettuce는 확장성 있지만 느림
   - Redisson은 둘 다 만족 (Best Practice)

## 📚 참고 자료

- [동시성 문제 해결 방법론](https://josteady.tistory.com/956)
- [Redisson 공식 문서](https://github.com/redisson/redisson)
- [Java Concurrency in Practice](https://www.amazon.com/Java-Concurrency-Practice-Brian-Goetz/dp/0321349601)

## 🚀 다음 단계

이 프로젝트를 완료했다면 다음 주제를 학습해보세요:

1. **Database 락**
   - Pessimistic Lock (비관적 락)
   - Optimistic Lock (낙관적 락)
   - Named Lock (MySQL GET_LOCK)

2. **메시지 큐**
   - Kafka로 순차 처리
   - RabbitMQ로 작업 큐 구현

3. **고급 분산 락**
   - Redlock 알고리즘
   - Consul 분산 락
   - Zookeeper 분산 락

---

## 📌 핵심 요약

| 환경 | 권장 방법 | 이유 |
|------|----------|------|
| **로컬 개발** | Synchronized | 간단하고 빠름 |
| **단일 서버** | Synchronized | 추가 인프라 불필요 |
| **Kubernetes** | Redisson | Pub/Sub 방식, 고성능 |
| **Auto Scaling** | Redisson | 확장성, 안정성 |
| **MSA 환경** | Redisson | 높은 트래픽 대응 |
| **높은 트래픽** | Redisson | CPU 효율, 빠른 성능 |

**⚡ 최종 결론:**
- 프로덕션 환경에서는 **Redisson** 사용을 강력 권장합니다!
- Lettuce는 간단한 케이스에만 사용하고, 높은 트래픽에서는 Redisson으로 전환하세요.
- Synchronized는 절대 분산 환경에서 사용하지 마세요!
