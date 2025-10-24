# Chapter 1: Producer & Consumer

## 학습 목표
- Kafka Producer를 사용하여 메시지를 발행하는 방법 이해
- Kafka Consumer를 사용하여 메시지를 소비하는 방법 이해
- Consumer Group의 개념과 병렬 처리 방식 학습
- 동기/비동기 메시지 전송의 차이점 파악

## 핵심 개념

### 1. Producer (생산자)
- 메시지를 Kafka 토픽으로 발행하는 클라이언트
- Key-Value 형태로 메시지 전송
- 파티션 할당 전략을 통해 부하 분산

### 2. Consumer (소비자)
- Kafka 토픽에서 메시지를 읽어오는 클라이언트
- Consumer Group을 통해 병렬 처리
- Offset을 통해 메시지 읽기 위치 관리

### 3. Topic (토픽)
- 메시지가 저장되는 논리적인 카테고리
- 여러 파티션으로 구성
- 파티션별로 순서 보장

### 4. Partition (파티션)
- 토픽을 물리적으로 나눈 단위
- 병렬 처리와 확장성 제공
- 파티션 내에서만 순서 보장

## 프로젝트 구조

```
src/main/java/com/example/kafka/
├── domain/
│   └── Order.java                    # 주문 도메인 모델
├── config/
│   ├── KafkaProducerConfig.java      # Producer 설정
│   ├── KafkaConsumerConfig.java      # Consumer 설정
│   └── KafkaTopicConfig.java         # Topic 설정
├── producer/
│   └── OrderProducer.java            # 주문 Producer 서비스
├── consumer/
│   └── OrderConsumer.java            # 주문 Consumer 서비스
└── controller/
    └── OrderController.java          # REST API 컨트롤러
```

## 주요 구현 내용

### 1. Producer 설정 (KafkaProducerConfig.java)

**핵심 설정:**
- `BOOTSTRAP_SERVERS_CONFIG`: Kafka 브로커 주소
- `KEY_SERIALIZER`: Key 직렬화 방식 (String)
- `VALUE_SERIALIZER`: Value 직렬화 방식 (JSON)
- `ACKS_CONFIG`: 메시지 전송 확인 수준
  - `0`: 확인 안 함 (빠름, 신뢰도 낮음)
  - `1`: Leader 확인 (중간)
  - `all`: 모든 Replica 확인 (느림, 신뢰도 높음)
- `RETRIES_CONFIG`: 실패 시 재시도 횟수

### 2. Consumer 설정 (KafkaConsumerConfig.java)

**핵심 설정:**
- `GROUP_ID_CONFIG`: Consumer Group ID
- `KEY_DESERIALIZER`: Key 역직렬화 방식
- `VALUE_DESERIALIZER`: Value 역직렬화 방식
- `AUTO_OFFSET_RESET_CONFIG`: Offset이 없을 때 시작 위치
  - `earliest`: 처음부터
  - `latest`: 최신부터
- `ENABLE_AUTO_COMMIT_CONFIG`: 자동 커밋 여부
- `MAX_POLL_RECORDS_CONFIG`: 한 번에 가져올 레코드 수

### 3. Topic 설정 (KafkaTopicConfig.java)

```java
@Bean
public NewTopic ordersTopic() {
    return new NewTopic("orders", 3, (short) 1);
}
```

- **토픽명**: orders
- **파티션 수**: 3개 (병렬 처리 가능)
- **복제 팩터**: 1 (단일 브로커 환경)

### 4. Producer 구현 (OrderProducer.java)

**비동기 전송:**
```java
CompletableFuture<SendResult<String, Order>> future =
    kafkaTemplate.send(TOPIC, order.getOrderId(), order);

future.whenComplete((result, ex) -> {
    if (ex == null) {
        log.info("Success");
    } else {
        log.error("Failed", ex);
    }
});
```

**동기 전송:**
```java
SendResult<String, Order> result =
    kafkaTemplate.send(TOPIC, order.getOrderId(), order).get();
```

### 5. Consumer 구현 (OrderConsumer.java)

**기본 Consumer:**
```java
@KafkaListener(topics = "orders", groupId = "kafka-learning-group")
public void consumeOrder(ConsumerRecord<String, Order> record) {
    Order order = record.value();
    processOrder(order);
}
```

**메타데이터와 함께 수신:**
```java
@KafkaListener(topics = "orders", groupId = "order-analytics-group")
public void consumeOrderWithMetadata(
    @Payload Order order,
    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
    @Header(KafkaHeaders.OFFSET) long offset
) {
    // 처리 로직
}
```

## 실습하기

### 1. Docker 환경 시작

```bash
cd kafka-learning-project
docker-compose up -d
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 주문 생성 API 테스트

**단건 주문 생성 (비동기):**
```bash
curl -X POST http://localhost:8090/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "productName": "Laptop",
    "quantity": 1,
    "price": 1500000
  }'
```

**단건 주문 생성 (동기):**
```bash
curl -X POST http://localhost:8090/api/orders/sync \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-002",
    "productName": "Mouse",
    "quantity": 2,
    "price": 30000
  }'
```

**특정 파티션으로 전송:**
```bash
curl -X POST http://localhost:8090/api/orders/partition/0 \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-003",
    "productName": "Keyboard",
    "quantity": 1,
    "price": 120000
  }'
```

**대량 주문 생성:**
```bash
curl -X POST "http://localhost:8090/api/orders/bulk?count=100"
```

### 4. Consumer 로그 확인

애플리케이션 로그에서 다음과 같은 Consumer 로그를 확인할 수 있습니다:

```
========================================
Consumed Order from Kafka
Topic: orders
Partition: 0
Offset: 12
Key: ORD-A1B2C3D4
Order: Order(orderId=ORD-A1B2C3D4, customerId=CUST-001, ...)
========================================
```

### 5. Kafka UI로 모니터링

브라우저에서 접속:
```
http://localhost:8080
```

- 토픽 목록 확인
- 메시지 내용 조회
- 파티션별 Offset 확인
- Consumer Group 상태 모니터링

## Consumer Group 실험

### 실험 1: 단일 Consumer
- Consumer Group: `kafka-learning-group`
- Consumer 개수: 1개
- 파티션: 3개
- 결과: 1개의 Consumer가 3개 파티션 모두 처리

### 실험 2: 다중 Consumer (최적)
- Consumer Group: `kafka-learning-group`
- Consumer 개수: 3개
- 파티션: 3개
- 결과: 각 Consumer가 1개 파티션씩 처리 (최적 병렬화)

### 실험 3: Consumer > Partition
- Consumer Group: `kafka-learning-group`
- Consumer 개수: 5개
- 파티션: 3개
- 결과: 3개만 활성화, 2개는 대기 (Idle)

## 핵심 포인트

### 1. 메시지 순서 보장
- **파티션 내**: 순서 보장 ✅
- **파티션 간**: 순서 보장 ❌
- 순서가 중요한 메시지는 같은 파티션으로 전송 (동일 Key 사용)

### 2. 파티션 전략
- **Key 기반**: Key의 해시값으로 파티션 결정
- **Round-Robin**: Key가 없을 때 순차적으로 분배
- **Custom**: 직접 파티션 지정 가능

### 3. Consumer Group
- 동일 Group 내에서는 메시지를 나눠서 처리
- 다른 Group은 모든 메시지를 각각 처리
- 예: `kafka-learning-group`, `order-analytics-group`, `high-value-order-group`

### 4. Offset 관리
- Offset: 각 파티션에서 Consumer가 읽은 위치
- 자동 커밋: 주기적으로 자동 저장
- 수동 커밋: Consumer가 직접 제어 (더 정확한 제어 가능)

## 성능 최적화 팁

### Producer
1. **비동기 전송 사용**: 처리량 증가
2. **배치 크기 조정**: `BATCH_SIZE_CONFIG`
3. **압축 활성화**: `COMPRESSION_TYPE_CONFIG` (gzip, snappy, lz4)
4. **적절한 ACKS 설정**: 신뢰도와 성능의 균형

### Consumer
1. **병렬 처리**: Consumer 수 = 파티션 수
2. **적절한 fetch 크기**: `MAX_POLL_RECORDS_CONFIG`
3. **수동 커밋 고려**: 정확한 처리 보장 필요 시
4. **스레드 풀 활용**: 메시지 처리를 별도 스레드에서 수행

## 다음 단계

다음 챕터에서는 **Topic & Partition**에 대해 더 깊이 학습합니다:
- 파티션 전략 상세
- Replication과 고가용성
- Partition Reassignment
- Topic 설계 Best Practice

## 참고 자료

- [Kafka Producer API](https://kafka.apache.org/documentation/#producerapi)
- [Kafka Consumer API](https://kafka.apache.org/documentation/#consumerapi)
- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/reference/html/)
