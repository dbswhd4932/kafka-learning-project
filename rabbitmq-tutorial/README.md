# RabbitMQ 비동기 아키텍처 튜토리얼

> RabbitMQ를 활용한 비동기 아키텍처를 기초부터 활용까지 단계별로 학습하는 튜토리얼 프로젝트입니다.

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

### Step 2: DB 연동과 Transaction 처리
- JPA와 RabbitMQ 통합
- 트랜잭션 처리 방법
- 분산 트랜잭션의 한계와 해결 방안

### Step 3: Exchange와 Routing Model
- **Direct Exchange**: 정확한 Routing Key 매칭
- **Topic Exchange**: 패턴 매칭 (*, #)
- **Fanout Exchange**: 브로드캐스트

### Step 4: Pub/Sub 실시간 알람 시스템
- Fanout Exchange를 활용한 Pub/Sub 패턴
- 이메일, SMS, 푸시 알림 동시 전송
- 실시간 이벤트 브로드캐스팅

### Step 5: Routing Model을 활용한 Log 수집
- Topic Exchange를 활용한 로그 라우팅
- 로그 레벨별 필터링 (ERROR, WARN, INFO)
- 서비스별 로그 분리

### Step 6: Dead Letter Queue와 Retry 재처리
- DLQ(Dead Letter Queue) 설정
- 실패한 메시지 재처리
- Parking Lot 패턴
- 자동 재시도 메커니즘

## 환경 설정

### 1. RabbitMQ 설치 및 실행

#### Docker 사용 (권장)
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

#### 직접 설치
- [RabbitMQ 공식 다운로드](https://www.rabbitmq.com/download.html)
- 설치 후 서비스 시작

### 2. 프로젝트 실행

```bash
# 프로젝트 클론
cd rabbitmq-tutorial

# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun
```

### 3. H2 Console 접속
- URL: http://localhost:8080/h2-console
- JDBC URL: jdbc:h2:mem:testdb
- Username: sa
- Password: (공백)

## API 테스트

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
curl -X POST "http://localhost:8080/api/v1/routing/direct?routingKey=medium&content=일반작업"
curl -X POST "http://localhost:8080/api/v1/routing/direct?routingKey=low&content=낮은우선순위작업"

# Topic Exchange - 패턴 매칭
curl -X POST "http://localhost:8080/api/v1/routing/topic?routingKey=order.created&content=주문생성"
curl -X POST "http://localhost:8080/api/v1/routing/topic?routingKey=order.payment.completed&content=결제완료"

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

# 간단한 알림 전송
curl -X POST "http://localhost:8080/api/v1/notifications/simple?type=PAYMENT_SUCCESS&recipientId=USER001&title=결제성공&content=결제가완료되었습니다"
```

### Step 5: Routing Model을 활용한 Log 수집

```bash
# Error 로그
curl -X POST "http://localhost:8080/api/v1/logs/error?serviceName=OrderService&message=주문처리중오류발생"

# Warn 로그
curl -X POST "http://localhost:8080/api/v1/logs/warn?serviceName=PaymentService&message=결제지연발생"

# Info 로그
curl -X POST "http://localhost:8080/api/v1/logs/info?serviceName=OrderService&message=주문처리완료"
```

### Step 6: Dead Letter Queue와 Retry 재처리

```bash
# 성공 작업 (정상 처리)
curl -X POST "http://localhost:8080/api/v1/dlq/task/success?content=정상작업"

# 실패 작업 (DLQ로 이동 테스트)
curl -X POST "http://localhost:8080/api/v1/dlq/task/fail?content=실패작업"

# DLQ 정보 조회
curl http://localhost:8080/api/v1/dlq/info
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

## RabbitMQ 핵심 개념

### AMQP 메시징 모델
```
Producer → Exchange → Binding → Queue → Consumer
```

### Exchange 타입

#### 1. Direct Exchange
- Routing Key가 **정확히 일치**하는 Queue로 메시지 전달
- 사용 사례: 우선순위별 작업 큐, 특정 워커 지정

#### 2. Topic Exchange
- Routing Key **패턴 매칭**으로 메시지 전달
- 와일드카드:
  - `*` : 정확히 한 단어 (예: `order.*`)
  - `#` : 0개 이상의 단어 (예: `order.#`)
- 사용 사례: 로그 수집, 위치 기반 알림

#### 3. Fanout Exchange
- Routing Key를 무시하고 **모든 Queue**로 메시지 전달
- 사용 사례: 실시간 알림, 이벤트 브로드캐스팅

### Dead Letter Queue (DLQ)
- 처리 실패한 메시지가 이동하는 특별한 큐
- 재처리 기회 제공
- 메시지 손실 방지

## 면접 대비 핵심 질문

### 1. 비동기 아키텍처가 필요한 이유는?
- 시스템 간 결합도 감소
- 확장성 향상
- 부하 분산
- 장애 격리

### 2. RabbitMQ vs Kafka 차이점은?
- **RabbitMQ**: 메시지 브로커, 작업 큐, AMQP 프로토콜
- **Kafka**: 이벤트 스트리밍, 로그 수집, 고성능 처리

### 3. 분산 트랜잭션 문제는 어떻게 해결하나?
- Outbox Pattern (권장)
- Saga Pattern
- 2-Phase Commit
- 최종 일관성(Eventual Consistency)

### 4. DLQ는 언제 사용하나?
- 메시지 처리 실패 시 재처리
- 독성 메시지(Poison Message) 격리
- 시스템 안정성 향상

## 참고 자료

- [RabbitMQ 공식 문서](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP 문서](https://docs.spring.io/spring-amqp/reference/)
- [AMQP 프로토콜 설명](https://www.amqp.org/)

## 라이선스

MIT License

## 기여

이 프로젝트는 학습 목적으로 작성되었습니다.
개선 사항이나 버그를 발견하시면 Issue나 Pull Request를 보내주세요!

---

**Happy Learning! 🐰**
