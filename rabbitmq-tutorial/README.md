# RabbitMQ 비동기 아키텍처 튜토리얼

> RabbitMQ를 활용한 비동기 아키텍처를 기초부터 활용까지 단계별로 학습하는 튜토리얼 프로젝트입니다.

## 📚 목차
- [프로젝트 소개](#프로젝트-소개)
- [기술 스택](#기술-스택)
- [학습 내용](#학습-내용)
- [환경 설정](#환경-설정)
- [RabbitMQ 핵심 개념](#rabbitmq-핵심-개념)
- [RabbitMQ 용어 완전 정복](#rabbitmq-용어-완전-정복)
- [메시지 처리 프로세스](#메시지-처리-프로세스)
- [RabbitMQ Management UI 가이드](#rabbitmq-management-ui-가이드)
- [성능 튜닝](#성능-튜닝)
- [실전 트러블슈팅](#실전-트러블슈팅)
- [API 테스트](#api-테스트)
- [면접 대비 핵심 질문](#면접-대비-핵심-질문)

## 프로젝트 소개

이 프로젝트는 대용량 데이터 처리를 위한 가장 기본적인 방법인 **비동기 아키텍처**를 RabbitMQ를 통해 학습하는 것을 목표로 합니다.
Spring Boot 3.3 기반으로 작성되었으며, 실무에서 자주 사용되는 패턴들을 단계별 예제로 제공합니다.

## 기술 스택

- **Java 17**
- **Spring Boot 3.3.5**
- **Spring AMQP** (RabbitMQ)
- **Spring Data JPA**
- **H2 Database** (개발/테스트용)
- **Lombok**
- **Gradle**

## 학습 내용

### Step 1: 기본 메시지 전송/수신
- Queue, Exchange, Binding의 개념 이해
- Producer/Consumer 패턴 구현
- JSON 메시지 직렬화/역직렬화
- **학습 포인트**: AMQP 메시징 모델의 기초

### Step 2: DB 연동과 Transaction 처리
- JPA와 RabbitMQ 통합
- 트랜잭션 처리 방법
- 분산 트랜잭션의 한계와 해결 방안
- **학습 포인트**: DB와 메시지 큐의 트랜잭션 경계

### Step 3: Exchange와 Routing Model
- **Direct Exchange**: 정확한 Routing Key 매칭
- **Topic Exchange**: 패턴 매칭 (*, #)
- **Fanout Exchange**: 브로드캐스트
- **학습 포인트**: Exchange 타입별 라우팅 전략

### Step 4: Pub/Sub 실시간 알람 시스템
- Fanout Exchange를 활용한 Pub/Sub 패턴
- 이메일, SMS, 푸시 알림 동시 전송
- 실시간 이벤트 브로드캐스팅
- **학습 포인트**: 하나의 이벤트를 여러 시스템으로 전파

### Step 5: Routing Model을 활용한 Log 수집
- Topic Exchange를 활용한 로그 라우팅
- 로그 레벨별 필터링 (ERROR, WARN, INFO)
- 서비스별 로그 분리
- **학습 포인트**: 패턴 매칭을 활용한 유연한 라우팅

### Step 6: Dead Letter Queue와 Retry 재처리
- DLQ(Dead Letter Queue) 설정
- 실패한 메시지 재처리
- Parking Lot 패턴
- 자동 재시도 메커니즘
- **학습 포인트**: 메시지 손실 방지와 장애 복구

## 환경 설정

### 1. RabbitMQ 설치 및 실행

#### Docker Compose 사용 (권장)
```bash
# 프로젝트 루트에서 실행
docker-compose up -d

# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f rabbitmq
```

#### 직접 실행
```bash
# RabbitMQ 컨테이너 실행
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management

# RabbitMQ Management Console 접속
# URL: http://localhost:15672
# Username: guest
# Password: guest
```

### 2. 프로젝트 실행

```bash
# 프로젝트 디렉토리로 이동
cd rabbitmq-tutorial

# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun
```

### 3. 접속 정보

| 서비스 | URL | 계정 정보 |
|--------|-----|----------|
| RabbitMQ Management UI | http://localhost:15672 | guest / guest |
| H2 Database Console | http://localhost:8080/h2-console | sa / (공백) |
| Spring Boot API | http://localhost:8080 | - |

## RabbitMQ 핵심 개념

### AMQP 메시징 모델

```
Producer → Exchange → Binding → Queue → Consumer
```

**핵심 흐름**:
1. **Producer**: 메시지를 생성하여 Exchange로 전송
2. **Exchange**: Routing Key를 기반으로 적절한 Queue로 라우팅
3. **Binding**: Exchange와 Queue를 연결하는 규칙
4. **Queue**: 메시지를 저장하는 버퍼
5. **Consumer**: Queue에서 메시지를 가져와 처리

### Exchange 타입 비교

| Exchange 타입 | Routing 방식 | 사용 사례 | 예시 |
|--------------|-------------|----------|------|
| **Direct** | Routing Key 정확 일치 | 우선순위별 작업 큐 | `high`, `medium`, `low` |
| **Topic** | 패턴 매칭 (`*`, `#`) | 로그 수집, 카테고리별 분류 | `order.*`, `*.error`, `korea.#` |
| **Fanout** | 모든 Queue로 전송 | 실시간 알림, 브로드캐스트 | 이메일+SMS+푸시 동시 전송 |
| **Headers** | 헤더 속성 매칭 | 복잡한 조건 라우팅 | `x-match: all/any` |

#### Direct Exchange 상세
```
Producer → [Exchange: "direct.exchange"]
              ├─ (routing key: "high")   → [Queue: high.queue]
              ├─ (routing key: "medium") → [Queue: medium.queue]
              └─ (routing key: "low")    → [Queue: low.queue]
```

#### Topic Exchange 상세
```
Producer → [Exchange: "topic.exchange"]
              ├─ (pattern: "order.*")      → order.created, order.updated
              ├─ (pattern: "*.payment.*")  → order.payment.completed
              └─ (pattern: "#")            → 모든 메시지
```

#### Fanout Exchange 상세
```
Producer → [Exchange: "fanout.exchange"]
              ├─ (routing key 무시) → [Queue: email.queue]
              ├─ (routing key 무시) → [Queue: sms.queue]
              └─ (routing key 무시) → [Queue: push.queue]
```

## RabbitMQ 용어 완전 정복

### 핵심 용어

#### 1. Connection (연결)
**정의**: 애플리케이션과 RabbitMQ 서버 간의 TCP 네트워크 연결

**특징**:
- TCP 소켓 연결
- 비용이 큰 리소스 (생성에 시간 소요)
- 보통 애플리케이션당 1개의 Connection 사용
- 연결이 끊어지면 모든 Channel도 함께 종료

**비유**: 우체국 건물로 들어가는 **입구** (1개)

**예시**:
```
192.168.65.1:32163  ← 애플리케이션의 Connection
```

#### 2. Channel (채널)
**정의**: 하나의 Connection 내에서 만들어지는 가상의 통신 경로

**특징**:
- 가벼운 리소스 (빠르게 생성 가능)
- 각 작업(송신/수신)마다 별도의 Channel 사용
- 멀티스레딩을 위한 것
- 하나의 Connection에서 수백~수천 개의 Channel 생성 가능

**비유**: 우체국 안의 **창구** (여러 개)

**예시**:
```
Connection #1
  ├─ Channel (1) → BasicConsumer
  ├─ Channel (2) → OrderConsumer
  ├─ Channel (3) → DirectConsumer
  └─ Channel (4) → TopicConsumer
```

**왜 Channel을 사용하나?**
- Connection 생성은 비용이 크기 때문
- 1개 Connection으로 여러 작업을 병렬 처리
- 각 Channel은 독립적으로 동작 (격리)

#### 3. Queue (큐)
**정의**: 메시지를 저장하는 버퍼

**특징**:
- FIFO (First In First Out) 구조
- 메시지 영속성(durable) 설정 가능
- 메시지 TTL, 최대 길이 등 설정 가능

**속성**:
- **durable**: 서버 재시작 시 Queue 유지 여부
- **exclusive**: 하나의 Connection만 사용 가능
- **auto-delete**: Consumer가 없으면 자동 삭제

#### 4. Exchange (익스체인지)
**정의**: 메시지를 적절한 Queue로 라우팅하는 라우터

**핵심 역할**:
- Producer로부터 메시지 수신
- Routing Key와 Binding 규칙을 기반으로 라우팅
- Queue로 메시지 전달

#### 5. Binding (바인딩)
**정의**: Exchange와 Queue를 연결하는 규칙

**구성 요소**:
- Exchange 이름
- Queue 이름
- Routing Key (또는 패턴)

**예시**:
```java
// Direct Binding
bind(queue).to(exchange).with("routing.key")

// Topic Binding
bind(queue).to(exchange).with("order.*")

// Fanout Binding (Routing Key 불필요)
bind(queue).to(exchange)
```

#### 6. Routing Key (라우팅 키)
**정의**: Exchange가 메시지를 라우팅할 때 사용하는 키

**형식**:
- 점(`.`)으로 구분된 단어들
- 예: `order.created`, `user.payment.completed`

**와일드카드** (Topic Exchange에서만):
- `*`: 정확히 한 단어 (예: `order.*` → `order.created`, `order.updated`)
- `#`: 0개 이상의 단어 (예: `order.#` → `order.created`, `order.payment.completed`)

#### 7. Producer (생산자)
**정의**: 메시지를 생성하여 Exchange로 전송하는 애플리케이션

**역할**:
- 메시지 생성
- Exchange로 전송
- Routing Key 지정

**예시**:
```java
@Component
public class BasicProducer {
    private final RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(
            "exchange.name",    // Exchange
            "routing.key",      // Routing Key
            message             // Message
        );
    }
}
```

#### 8. Consumer (소비자)
**정의**: Queue에서 메시지를 수신하여 처리하는 애플리케이션

**역할**:
- Queue 구독
- 메시지 수신
- 비즈니스 로직 처리
- ACK/NACK 응답

**예시**:
```java
@Component
public class BasicConsumer {
    @RabbitListener(queues = "queue.name")
    public void receiveMessage(String message) {
        // 메시지 처리 로직
    }
}
```

#### 9. ACK/NACK (확인 응답)
**정의**: Consumer가 메시지 처리 결과를 RabbitMQ에 알리는 신호

**ACK (Acknowledgement)**:
- 메시지 처리 성공
- RabbitMQ가 메시지를 Queue에서 삭제

**NACK (Negative Acknowledgement)**:
- 메시지 처리 실패
- RabbitMQ가 메시지를 재전송 또는 DLQ로 이동

**모드**:
```yaml
acknowledge-mode:
  - auto: 예외 없으면 자동 ACK, 예외 발생 시 NACK
  - manual: 개발자가 직접 ACK/NACK 호출
  - none: 응답 없음 (메시지 손실 위험)
```

#### 10. Prefetch Count
**정의**: 각 Consumer가 한 번에 가져갈 수 있는 메시지 개수

**동작 방식**:
```yaml
prefetch: 10
```
- Consumer가 Queue에서 미리 10개의 메시지를 가져옴
- 처리하면서 계속 가져옴
- 처리 속도가 빠른 Consumer가 더 많은 메시지 처리

**설정 예시**:
```
prefetch: 1  (느린 작업, 공평한 분배)
prefetch: 10 (빠른 작업, 높은 처리량)
prefetch: 100 (대량 배치 작업)
```

#### 11. Virtual Host (가상 호스트)
**정의**: RabbitMQ 서버 내의 논리적인 격리 공간

**특징**:
- 하나의 RabbitMQ에서 여러 환경 분리
- 각 vhost는 독립적인 Exchange, Queue 보유
- 기본 vhost: `/`

**사용 사례**:
```
vhost: /dev   → 개발 환경
vhost: /test  → 테스트 환경
vhost: /prod  → 운영 환경
```

#### 12. Dead Letter Queue (DLQ)
**정의**: 처리 실패한 메시지가 이동하는 특별한 Queue

**이동 조건**:
1. Consumer에서 예외 발생 후 최대 재시도 횟수 초과
2. 메시지가 거부(NACK)되고 재큐잉되지 않을 때
3. 메시지 TTL 만료
4. Queue 최대 길이 초과

**설정**:
```java
Map<String, Object> args = new HashMap<>();
args.put("x-dead-letter-exchange", "dlq.exchange");
args.put("x-dead-letter-routing-key", "dlq.routing.key");

Queue queue = new Queue("main.queue", true, false, false, args);
```

## 메시지 처리 프로세스

### 1. 메시지 전송 흐름

```
① Producer가 메시지 생성
   ↓
② RabbitTemplate.convertAndSend() 호출
   ↓
③ MessageConverter가 Java 객체 → JSON 변환
   ↓
④ Channel을 통해 Exchange로 전송
   ↓
⑤ Exchange가 Routing Key 확인
   ↓
⑥ Binding 규칙에 따라 Queue로 라우팅
   ↓
⑦ Queue에 메시지 저장
```

### 2. 메시지 수신 흐름

```
① Consumer가 Queue 구독 (@RabbitListener)
   ↓
② Queue에 메시지 도착
   ↓
③ RabbitMQ가 prefetch 설정에 따라 메시지 전달
   ↓
④ MessageConverter가 JSON → Java 객체 변환
   ↓
⑤ Consumer 메서드 호출
   ↓
⑥ 비즈니스 로직 처리
   ↓
⑦-A. 성공: ACK 전송 → 메시지 삭제
⑦-B. 실패: NACK 전송 → 재시도 또는 DLQ 이동
```

### 3. Concurrency와 메시지 분배

#### Concurrency 설정

```yaml
listener:
  simple:
    concurrency: 1      # 각 Queue당 Consumer 수 (학습용)
    max-concurrency: 3  # 부하 증가 시 최대 Consumer 수
```

#### concurrency: 1 (학습용)

```
Queue: [메시지1, 메시지2, 메시지3]
  ↓
Channel 1 → 메시지1 처리 → 메시지2 처리 → 메시지3 처리
(순차 처리)
```

#### concurrency: 3 (운영 환경)

```
Queue: [메시지1, 메시지2, 메시지3, 메시지4, 메시지5]
  ↓
Channel 1 → 메시지1 처리 ⚙️
Channel 2 → 메시지2 처리 ⚙️
Channel 3 → 메시지3 처리 ⚙️
(병렬 처리)

Queue: [메시지4, 메시지5]
  ↓
Channel 1 완료 → 메시지4 처리 ⚙️
Channel 2 처리 중... ⚙️
Channel 3 처리 중... ⚙️
```

### 4. Round-Robin 분배 방식

**원칙**: RabbitMQ는 메시지를 여러 Consumer에게 **공평하게** 분배합니다.

```
메시지 10개, Consumer 3개

Round-Robin 방식:
Consumer 1: 메시지 1, 4, 7, 10
Consumer 2: 메시지 2, 5, 8
Consumer 3: 메시지 3, 6, 9
```

**중요**: 메시지는 **절대 중복 처리되지 않습니다**!

### 5. Prefetch의 영향

```yaml
prefetch: 10
concurrency: 3
```

```
Queue: [메시지 1~100]
  ↓
Consumer 1: 메시지 1~10 (미리 가져감)
Consumer 2: 메시지 11~20 (미리 가져감)
Consumer 3: 메시지 21~30 (미리 가져감)

→ 각자 처리하면서 계속 가져옴
→ 빠른 Consumer가 더 많이 처리
```

## RabbitMQ Management UI 가이드

### 접속 방법
```
URL: http://localhost:15672
Username: guest
Password: guest
```

### 주요 탭 설명

#### 1. Overview (개요)
**표시 내용**:
- RabbitMQ 버전 정보
- 전체 메시지 처리 속도 (Messages/sec)
- Connection, Channel, Queue 개수
- 노드 상태

**주요 지표**:
- **Ready**: 처리 대기 중인 메시지 수
- **Unacked**: Consumer가 가져갔지만 ACK 안 한 메시지 수
- **Total**: 전체 메시지 수

#### 2. Connections (연결)
**표시 내용**:
```
Name: 192.168.65.1:32163 → 127.0.0.1:5672
```
- **192.168.65.1**: Docker Desktop의 가상 네트워크 주소
- **32163**: 애플리케이션이 사용하는 랜덤 포트
- **127.0.0.1:5672**: RabbitMQ 서버 주소

**확인 사항**:
- Connection 수 (보통 1개)
- 연결 상태 (running)
- Channel 수

#### 3. Channels (채널)
**표시 내용**:
```
192.168.65.1:32163 (1)  ← Channel 1
192.168.65.1:32163 (2)  ← Channel 2
192.168.65.1:32163 (3)  ← Channel 3
...
```

**확인 사항**:
- Channel 수 = `@RabbitListener 개수 × concurrency`
- 각 Channel이 어떤 Queue를 구독하는지
- Consumer Tag (고유 식별자)

**채널 수 계산**:
```
우리 프로젝트의 @RabbitListener 수: 21개
concurrency: 1

→ 21개 × 1 = 21개의 Channel
```

#### 4. Queues (큐)
**표시 내용**:

| Queue 이름 | Ready | Unacked | Total | Message rates |
|-----------|-------|---------|-------|---------------|
| basic.queue | 0 | 0 | 0 | 0.0/s |
| transaction.queue | 5 | 0 | 5 | 1.2/s |

**컬럼 설명**:
- **Ready**: 처리 대기 중인 메시지 수
- **Unacked**: Consumer가 가져갔지만 ACK 전송 전인 메시지
- **Total**: Ready + Unacked
- **Message rates**: 초당 메시지 처리 속도

**액션**:
- **Get messages**: 수동으로 메시지 확인 (테스트용)
- **Purge messages**: 모든 메시지 삭제
- **Delete**: Queue 삭제

#### 5. Exchanges (익스체인지)
**표시 내용**:
- Exchange 이름
- Type (direct, topic, fanout, headers)
- Features (durable, auto-delete 등)
- Message rates

**기본 Exchange**:
- `(AMQP default)`: Direct Exchange, Routing Key = Queue 이름

#### 6. Admin (관리)
- 사용자 관리
- Virtual Host 관리
- Policies 설정

## 성능 튜닝

### 1. Concurrency 설정

#### 학습 및 개발 환경
```yaml
listener:
  simple:
    concurrency: 1
    max-concurrency: 2
```
**장점**: 로그 확인 쉬움, 디버깅 용이
**단점**: 처리 속도 느림

#### 테스트 환경
```yaml
listener:
  simple:
    concurrency: 2
    max-concurrency: 5
```
**장점**: 실제 운영과 유사한 환경
**단점**: 로그 추적 복잡

#### 운영 환경 (대용량 트래픽)
```yaml
listener:
  simple:
    concurrency: 5
    max-concurrency: 20
```
**장점**: 높은 처리량
**단점**: 리소스 사용량 증가

### 2. Prefetch 최적화

```yaml
prefetch: 1   # 공평한 분배 (느린 작업)
prefetch: 10  # 균형 (일반적)
prefetch: 100 # 높은 처리량 (빠른 작업)
```

**권장 설정**:
- **CPU 집약적 작업**: prefetch: 1~5
- **I/O 대기 작업**: prefetch: 10~20
- **배치 처리**: prefetch: 50~100

### 3. Connection Pool 설정

```yaml
rabbitmq:
  cache:
    connection:
      mode: CONNECTION
      size: 1  # Connection Pool 크기
    channel:
      size: 25  # Channel Cache 크기
```

### 4. 메시지 크기 최적화

```yaml
# 큰 메시지는 피하기
spring:
  rabbitmq:
    template:
      receive-timeout: 5000
      max-message-size: 1048576  # 1MB
```

**권장**:
- 메시지 크기: < 128KB
- 큰 데이터는 S3/DB 저장 후 참조 전달

## 실전 트러블슈팅

### 1. Channel이 너무 많이 생성됨

**증상**:
```
Channels 탭에 192.168.65.1:32163 (1) ~ (63) 까지 63개 표시
```

**원인**:
```yaml
concurrency: 3
```
21개 @RabbitListener × 3 = 63개 Channel

**해결**:
```yaml
# application.yml
listener:
  simple:
    concurrency: 1  # 학습용으로 1개로 변경
    max-concurrency: 3
```

**결과**: 21개 Channel로 감소

### 2. 메시지가 처리되지 않음

**확인 사항**:

1. **RabbitMQ 실행 확인**
```bash
docker-compose ps
```

2. **Queue에 메시지 확인**
- Management UI → Queues 탭
- Ready 컬럼에 숫자 확인

3. **Consumer 등록 확인**
- Queues 탭 → Queue 클릭 → Consumers 섹션
- Consumer 수가 0이면 문제!

4. **애플리케이션 로그 확인**
```
o.s.a.r.l.SimpleMessageListenerContainer : Consumer started
```

**해결**:
```java
// @RabbitListener의 queues 이름 확인
@RabbitListener(queues = "basic.queue")  // ← 오타 확인!
```

### 3. Connection refused 에러

**증상**:
```
java.net.ConnectException: Connection refused
```

**원인**:
- RabbitMQ가 실행되지 않음
- 포트 번호 틀림
- 네트워크 문제

**해결**:
```bash
# 1. RabbitMQ 상태 확인
docker-compose ps

# 2. RabbitMQ 재시작
docker-compose restart rabbitmq

# 3. 로그 확인
docker-compose logs rabbitmq

# 4. 포트 확인
lsof -i :5672
```

### 4. 메시지가 DLQ로 계속 이동

**증상**:
```
모든 메시지가 main.queue → dlq.queue로 이동
```

**원인**:
- Consumer 코드에서 예외 발생
- 재시도 횟수 초과

**해결**:
```java
@RabbitListener(queues = "main.queue")
public void process(Message msg) {
    try {
        // 처리 로직
    } catch (Exception e) {
        log.error("처리 실패", e);  // ← 로그 확인!
        throw e;  // 재시도를 위해 예외 던지기
    }
}
```

### 5. 192.168.65.1이 뭔가요?

**답변**:
- Docker Desktop의 가상 네트워크 주소
- Windows/Mac에서 Docker 컨테이너와 통신하는 주소
- 정상적인 동작입니다!

**참고**:
```
Mac: 192.168.65.1
Windows: 192.168.65.1
Linux (native): 127.0.0.1 또는 172.17.0.1
```

### 6. Unacked 메시지가 계속 증가

**증상**:
```
Queue의 Unacked 컬럼이 계속 증가
```

**원인**:
- Consumer가 처리 중 멈춤
- ACK를 보내지 않음
- 처리 시간이 너무 오래 걸림

**해결**:
```java
// 처리 시간이 긴 작업
@RabbitListener(queues = "slow.queue")
public void processSlowTask(Message msg) {
    // 타임아웃 늘리기
    // 또는 작업을 더 작은 단위로 분할
}
```

```yaml
# application.yml
rabbitmq:
  listener:
    simple:
      acknowledge-mode: auto
      default-requeue-rejected: false  # 실패 시 재큐잉 안 함
```

## 프로젝트 구조

```
rabbitmq-tutorial/
├── src/main/java/com/example/rabbitmq/
│   ├── RabbitMqTutorialApplication.java
│   ├── config/
│   │   └── RabbitMqConfig.java                 # 공통 RabbitMQ 설정
│   ├── step1_basic/                             # Step 1: 기본 메시지
│   │   ├── BasicMessage.java
│   │   ├── BasicQueueConfig.java
│   │   ├── BasicProducer.java
│   │   ├── BasicConsumer.java
│   │   └── BasicController.java
│   ├── step2_transaction/                       # Step 2: Transaction
│   │   ├── Order.java
│   │   ├── OrderRepository.java
│   │   ├── OrderMessage.java
│   │   ├── TransactionQueueConfig.java
│   │   ├── OrderService.java
│   │   ├── OrderConsumer.java
│   │   └── TransactionController.java
│   ├── step3_routing/                           # Step 3: Routing Model
│   │   ├── RoutingMessage.java
│   │   ├── DirectExchangeConfig.java
│   │   ├── TopicExchangeConfig.java
│   │   ├── FanoutExchangeConfig.java
│   │   ├── RoutingProducer.java
│   │   ├── DirectConsumer.java
│   │   ├── TopicConsumer.java
│   │   ├── FanoutConsumer.java
│   │   └── RoutingController.java
│   ├── step4_pubsub/                            # Step 4: Pub/Sub 알람
│   │   ├── NotificationMessage.java
│   │   ├── NotificationConfig.java
│   │   ├── NotificationProducer.java
│   │   ├── EmailNotificationConsumer.java
│   │   ├── SmsNotificationConsumer.java
│   │   ├── PushNotificationConsumer.java
│   │   └── NotificationController.java
│   ├── step5_logging/                           # Step 5: 로그 수집
│   │   ├── LogMessage.java
│   │   ├── LoggingConfig.java
│   │   ├── LogProducer.java
│   │   ├── LogConsumer.java
│   │   └── LogController.java
│   └── step6_dlq/                               # Step 6: DLQ와 Retry
│       ├── TaskMessage.java
│       ├── DlqConfig.java
│       ├── TaskProducer.java
│       ├── TaskConsumer.java
│       ├── DlqConsumer.java
│       ├── ParkingLotConsumer.java
│       └── DlqController.java
└── src/main/resources/
    └── application.yml                          # 애플리케이션 설정
```

## API 테스트

### 통합 테스트 파일 사용

`api-tests.http` 파일을 사용하여 모든 API를 순서대로 테스트할 수 있습니다.

**IntelliJ IDEA / VS Code**:
1. `api-tests.http` 파일 열기
2. 각 요청 옆의 실행 버튼 클릭
3. `Ctrl+Enter` 또는 `Cmd+Enter`

### Step 1: 기본 메시지 전송/수신

```bash
# 간단한 메시지 전송
curl -X POST "http://localhost:8080/api/v1/basic/send/simple?content=Hello%20RabbitMQ&sender=user1"

# JSON 메시지 전송
curl -X POST http://localhost:8080/api/v1/basic/send \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Hello RabbitMQ!",
    "sender": "user1"
  }'
```

### Step 2: DB 연동과 Transaction 처리

```bash
# 주문 생성 (DB 저장 + 메시지 전송)
curl -X POST http://localhost:8080/api/v1/transaction/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderNumber": "ORD-001",
    "customerId": "CUST-001",
    "productName": "Laptop",
    "quantity": 1,
    "price": 1500000
  }'

# 주문 조회
curl http://localhost:8080/api/v1/transaction/orders/ORD-001
```

### Step 3: Exchange와 Routing Model

```bash
# Direct Exchange - 우선순위별 라우팅
curl -X POST "http://localhost:8080/api/v1/routing/direct?routingKey=high&content=긴급작업"

# Topic Exchange - 패턴 매칭
curl -X POST "http://localhost:8080/api/v1/routing/topic?routingKey=order.created&content=주문생성"

# Fanout Exchange - 브로드캐스트
curl -X POST "http://localhost:8080/api/v1/routing/fanout?content=전체알림"
```

### Step 4: Pub/Sub 실시간 알람 시스템

```bash
# 알림 전송 (이메일, SMS, 푸시 동시 전송)
curl -X POST http://localhost:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "type": "ORDER_COMPLETED",
    "recipientId": "USER001",
    "title": "주문이 완료되었습니다",
    "content": "주문번호 ORD-001이 성공적으로 처리되었습니다."
  }'
```

### Step 5: Routing Model을 활용한 Log 수집

```bash
# Error 로그
curl -X POST "http://localhost:8080/api/v1/logs/error?serviceName=OrderService&message=주문처리중오류발생"

# Info 로그
curl -X POST "http://localhost:8080/api/v1/logs/info?serviceName=OrderService&message=주문처리완료"
```

### Step 6: Dead Letter Queue와 Retry 재처리

```bash
# 성공 작업
curl -X POST "http://localhost:8080/api/v1/dlq/task/success?content=정상작업"

# 실패 작업 (DLQ로 이동 테스트)
curl -X POST "http://localhost:8080/api/v1/dlq/task/fail?content=실패작업"
```

## 면접 대비 핵심 질문

### 1. 비동기 아키텍처가 필요한 이유는?

**답변**:
- **시스템 간 결합도 감소**: 서비스가 직접 통신하지 않고 메시지 큐를 통해 통신
- **확장성 향상**: Consumer를 독립적으로 스케일 아웃 가능
- **부하 분산**: 메시지가 큐에 쌓이고 Consumer가 처리 가능한 속도로 처리
- **장애 격리**: 한 서비스의 장애가 다른 서비스에 전파되지 않음
- **비동기 처리**: 사용자 응답 속도 향상 (백그라운드 처리)

**실제 사례**:
- 주문 → 결제 → 재고 → 배송 시스템 통합
- 이메일/SMS 대량 발송
- 대용량 배치 처리

### 2. RabbitMQ vs Kafka 차이점은?

| 구분 | RabbitMQ | Kafka |
|------|----------|-------|
| **타입** | 메시지 브로커 | 이벤트 스트리밍 플랫폼 |
| **프로토콜** | AMQP | 자체 프로토콜 |
| **메시지 보관** | 전달 후 삭제 | 설정된 기간 보관 |
| **처리 방식** | Push (Consumer에게 전달) | Pull (Consumer가 가져감) |
| **순서 보장** | Queue 단위 | Partition 단위 |
| **사용 사례** | 작업 큐, 요청-응답 | 로그 수집, 이벤트 소싱 |
| **처리량** | 중간 | 매우 높음 |
| **복잡도** | 낮음 | 높음 |

**선택 기준**:
- **RabbitMQ**: 작업 큐, 복잡한 라우팅, 낮은 지연시간
- **Kafka**: 대용량 로그, 이벤트 소싱, 재처리 필요

### 3. Connection과 Channel의 차이는?

**답변**:

**Connection (연결)**:
- TCP 네트워크 연결
- 비용이 큰 리소스
- 애플리케이션당 보통 1개

**Channel (채널)**:
- Connection 내의 가상 통신 경로
- 가벼운 리소스
- 각 작업마다 별도 Channel 사용

**비유**:
- Connection = 우체국 건물 입구 (1개)
- Channel = 우체국 안의 창구 (여러 개)

**이유**:
- Connection 생성은 비용이 크므로
- 1개 Connection으로 여러 작업을 병렬 처리

### 4. 분산 트랜잭션 문제는 어떻게 해결하나?

**문제 상황**:
```java
@Transactional
public void createOrder(Order order) {
    orderRepository.save(order);          // DB 저장
    rabbitTemplate.send(exchange, order); // 메시지 전송
    // DB 커밋 전에 메시지가 전송될 수 있음!
}
```

**해결 방법**:

1. **Outbox Pattern (권장)**
```java
@Transactional
public void createOrder(Order order) {
    orderRepository.save(order);
    outboxRepository.save(new OutboxEvent(order)); // 같은 트랜잭션
}

// 별도 스케줄러가 Outbox 테이블을 읽어서 메시지 전송
@Scheduled
public void sendOutboxEvents() {
    List<OutboxEvent> events = outboxRepository.findPending();
    events.forEach(event -> {
        rabbitTemplate.send(exchange, event);
        outboxRepository.markAsSent(event);
    });
}
```

2. **Saga Pattern**
- 각 서비스가 로컬 트랜잭션 실행
- 실패 시 보상 트랜잭션 실행

3. **2-Phase Commit**
- 복잡하고 성능 이슈
- 실무에서는 거의 사용 안 함

4. **최종 일관성 (Eventual Consistency)**
- 즉시 일관성을 보장하지 않음
- 시간이 지나면 일관성 확보

### 5. DLQ는 언제 사용하나?

**답변**:

**사용 시점**:
1. 메시지 처리 실패 시 재처리
2. 독성 메시지(Poison Message) 격리
3. 일시적 장애 대응
4. 메시지 손실 방지

**동작 방식**:
```
Main Queue → 처리 실패 → 재시도 3번 → DLQ로 이동
```

**실제 활용**:
```java
// Main Queue에서 처리
@RabbitListener(queues = "main.queue")
public void process(Message msg) {
    // 외부 API 호출 (네트워크 오류 가능)
    externalApi.call(msg);
}

// DLQ에서 재처리
@RabbitListener(queues = "dlq.queue")
public void retry(Message msg) {
    // 1. 재처리 시도
    // 2. 성공 시 완료
    // 3. 실패 시 Parking Lot으로 이동
}
```

### 6. Prefetch는 무엇이고 어떻게 설정하나?

**답변**:

**정의**: Consumer가 한 번에 가져갈 수 있는 메시지 개수

**동작**:
```yaml
prefetch: 10
```
- Consumer가 Queue에서 미리 10개 가져옴
- 처리하면서 계속 가져옴
- 빠른 Consumer가 더 많이 처리

**설정 가이드**:
```yaml
prefetch: 1    # 느린 작업, 공평한 분배
prefetch: 10   # 일반적인 작업
prefetch: 100  # 빠른 작업, 배치 처리
```

### 7. 메시지 중복 처리를 방지하려면?

**답변**:

1. **Idempotent (멱등성) 보장**
```java
@RabbitListener(queues = "order.queue")
public void process(OrderMessage msg) {
    // 이미 처리된 주문인지 확인
    if (orderRepository.existsByOrderNumber(msg.getOrderNumber())) {
        log.info("이미 처리된 주문: {}", msg.getOrderNumber());
        return;
    }

    // 주문 처리
    orderRepository.save(msg.toEntity());
}
```

2. **Unique Key 사용**
```java
@Entity
public class Order {
    @Column(unique = true)
    private String orderNumber;
    // 중복 insert 시 예외 발생
}
```

3. **Redis로 처리 이력 관리**
```java
if (redisTemplate.hasKey("processed:" + msg.getId())) {
    return; // 이미 처리됨
}
// 처리 후
redisTemplate.set("processed:" + msg.getId(), "true", 1, TimeUnit.HOURS);
```

## 참고 자료

- [RabbitMQ 공식 문서](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP 문서](https://docs.spring.io/spring-amqp/reference/)
- [AMQP 프로토콜 설명](https://www.amqp.org/)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/getstarted.html)
- [Enterprise Integration Patterns](https://www.enterpriseintegrationpatterns.com/)

## 라이선스

MIT License

## 기여

이 프로젝트는 학습 목적으로 작성되었습니다.
개선 사항이나 버그를 발견하시면 Issue나 Pull Request를 보내주세요!

---

**Happy Learning! 🐰**
