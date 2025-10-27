# Kafka Learning Project

Apache Kafka를 활용한 이벤트 기반 아키텍처 학습 프로젝트입니다.

## 프로젝트 개요

**트랜잭션 분리 패턴**을 적용한 주문 처리 시스템으로, Kafka를 직접 사용하여 성공한 주문만 메시지로 발행합니다.

### 주요 특징

- **트랜잭션 분리**: 각 비즈니스 단위별로 독립적인 트랜잭션 실행
- **직접 Kafka 발행**: Spring Event 없이 Kafka Producer를 직접 사용하는 단순한 아키텍처
- **결제 시뮬레이션**: 30% 확률로 실패하는 결제 프로세스
- **성공한 주문만 발행**: 결제가 성공한 주문만 Kafka 토픽으로 발행

## 시작하기

### 1. Docker 환경 구성

Kafka 클러스터 및 관련 인프라를 Docker Compose로 실행합니다:

```bash
docker-compose up -d
```

다음 서비스가 실행됩니다:
- **Kafka** (포트 9092): 메시지 브로커
- **Zookeeper** (포트 2181): Kafka 클러스터 관리
- **Schema Registry** (포트 8081): Avro 스키마 관리
- **PostgreSQL** (포트 5432): 데이터베이스
- **Kafka Connect** (포트 8083): Debezium CDC 커넥터
- **Kafka UI** (포트 8080): Kafka 관리 UI

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

애플리케이션은 포트 8090에서 실행됩니다.

### 3. API 테스트

#### 단건 주문 생성
```bash
curl -X POST http://localhost:8090/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "productName": "노트북",
    "quantity": 1,
    "price": 1500000
  }'
```

#### 대량 주문 생성
```bash
curl -X POST "http://localhost:8090/api/orders/bulk?count=10"
```

#### 헬스체크
```bash
curl http://localhost:8090/api/orders/health
```

### 4. 인프라 종료

```bash
docker-compose down
```

데이터 볼륨까지 삭제하려면:

```bash
docker-compose down -v
```

## 아키텍처

### 주문 처리 흐름

```
[HTTP POST] → OrderController
    ↓
OrderService.createOrder()
    ↓
1. 주문 정보 초기화
    ↓
2. [TX-1] createPendingOrder() → PENDING 상태로 저장
    ↓
3. 결제 처리 시뮬레이션 (30% 실패)
    ↓
    ├─ 성공 → [TX-2] markOrderAsSuccess() → SUCCESS
    │           ↓
    │       publishToKafka() → Kafka 메시지 발행
    │           ↓
    │       ├─ sales-orders 토픽
    │       └─ order-success 토픽
    │
    └─ 실패 → [TX-3] markOrderAsFailed() → FAILED
```

### 트랜잭션 분리 전략

각 비즈니스 단위는 독립적인 트랜잭션으로 실행:

- **TX-1**: PENDING 상태로 주문 생성 (결제 성공/실패와 무관하게 이력 보존)
- **TX-2**: 결제 성공 시 주문 상태를 SUCCESS로 변경
- **TX-3**: 결제 실패 시 주문 상태를 FAILED로 변경

## 프로젝트 구조

```
kafka-learning-project/
├── src/
│   ├── main/
│   │   ├── java/com/example/kafka/
│   │   │   ├── KafkaLearningApplication.java
│   │   │   ├── common/              # Kafka Producer 공통 모듈
│   │   │   │   └── KafkaProducerCluster.java
│   │   │   ├── config/              # Kafka, JPA, Async 설정
│   │   │   │   ├── KafkaProducerConfig.java
│   │   │   │   ├── KafkaConsumerConfig.java
│   │   │   │   ├── KafkaTopicConfig.java
│   │   │   │   ├── JpaAuditingConfig.java
│   │   │   │   └── AsyncConfig.java
│   │   │   ├── controller/          # REST API
│   │   │   │   └── OrderController.java
│   │   │   ├── service/             # 비즈니스 로직
│   │   │   │   ├── OrderService.java           # 주문 처리 메인 로직
│   │   │   │   └── OrderTransactionService.java # 트랜잭션 분리
│   │   │   ├── consumer/            # Kafka Consumer
│   │   │   │   └── OrderConsumer.java
│   │   │   ├── producer/            # Kafka Producer
│   │   │   │   └── OrderProducer.java
│   │   │   ├── domain/              # 도메인 모델
│   │   │   │   └── Order.java
│   │   │   ├── entity/              # JPA 엔티티
│   │   │   │   ├── OrderEntity.java
│   │   │   │   ├── ApplicationEventFailureEntity.java
│   │   │   │   └── base/            # Base 엔티티
│   │   │   ├── repository/          # JPA Repository
│   │   │   │   ├── OrderRepository.java
│   │   │   │   └── ApplicationEventFailureRepository.java
│   │   │   ├── message/             # Kafka 메시지 DTO
│   │   │   │   └── SalesOrderMessage.java
│   │   │   ├── enums/               # Enum 타입
│   │   │   │   ├── OrderStatus.java
│   │   │   │   ├── MessageCategory.java
│   │   │   │   ├── ApplicationEventType.java
│   │   │   │   └── UserType.java
│   │   │   ├── converter/           # JPA Converter
│   │   │   │   └── BooleanToYNConverter.java
│   │   │   ├── properties/          # Properties
│   │   │   │   └── KafkaTopicProperties.java
│   │   │   └── security/            # 보안 관련
│   │   │       ├── AccessUser.java
│   │   │       └── AccessUserManager.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
├── docker-compose.yml
├── build.gradle
└── README.md
```

## 토픽 구조

### 1. sales-orders (판매 주문)
- **Partitions**: 3
- **Replication Factor**: 1
- **용도**: 성공한 주문 메시지를 발행하는 메인 토픽
- **Consumer Group**:
  - `kafka-learning-group`: 기본 주문 처리
  - `order-analytics-group`: 분석용 주문 데이터

### 2. order-success (주문 성공)
- **Partitions**: 1
- **Replication Factor**: 1
- **용도**: 성공한 주문을 모니터링하기 위한 토픽

### 3. order-failure (주문 실패)
- **Partitions**: 1
- **Replication Factor**: 1
- **용도**: 실패한 주문을 추적하기 위한 토픽 (향후 확장용)

## 데이터베이스 스키마

### orders 테이블
```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(50) UNIQUE NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    order_status VARCHAR(20) NOT NULL,  -- PENDING, SUCCESS, FAILED
    order_success_yn CHAR(1) NOT NULL,  -- Y, N
    fail_reason VARCHAR(500),
    order_datetime DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);
```

### application_event_failure 테이블
```sql
CREATE TABLE application_event_failure (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    error_message TEXT,
    created_at DATETIME NOT NULL
);
```

## 학습 주제

### ✅ 완료
1. **Producer & Consumer**: Kafka 데이터 발행과 구독
2. **Topic & Partition**: 데이터 분산 저장과 병렬 처리
3. **트랜잭션 분리 패턴**: 각 비즈니스 단위별 독립적인 트랜잭션 관리
4. **직접 Kafka 발행**: Spring Event 없이 Kafka Producer 직접 사용

### 📋 예정
5. **Avro & Schema Registry**: 안정적인 데이터 스키마 관리
6. **Kafka Connect & Debezium**: DB CDC 구현
7. **Kafka Streams API**: 실시간 스트림 처리
8. **Stateful Stream Processing**: 상태 기반 데이터 집계
9. **Interactive Queries**: 실시간 데이터 조회 API
10. **Consumer Group & Rebalancing**: Consumer 확장과 장애 대응

## 기술 스택

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Gradle 8.8
- **Database**: MySQL 8.0
- **Kafka**: Confluent Platform 7.5.1
- **Container**: Docker

## 로그 확인

### 주문 생성 흐름 로그
```
📦 주문 생성 시작: {상품명}
💾 [TX-1 START] 주문 생성 트랜잭션 시작
💾 [TX-1 COMMIT] 주문 생성 완료 - ID: {주문ID}, 상태: PENDING
💳 결제 처리 중... (주문 ID: {주문ID}, 금액: {금액}원)

# 성공 케이스
💳 ✅ 결제 성공: {주문ID} (승인번호: {승인번호})
💾 [TX-2 START] 주문 성공 처리 트랜잭션 시작
💾 [TX-2 COMMIT] 주문 성공 처리 완료
📤 Kafka 메시지 발행 시작: {주문ID}
✅ Kafka 메시지 발행 성공: {주문ID} -> sales-orders 토픽
✅ 성공 메시지 발행: {주문ID} -> order-success 토픽

# 실패 케이스
💳 ❌ 결제 실패: {주문ID} (사유: 카드 승인 거부)
💾 [TX-3 START] 주문 실패 처리 트랜잭션 시작
💾 [TX-3 COMMIT] 주문 실패 처리 완료
```
