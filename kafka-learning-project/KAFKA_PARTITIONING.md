# Kafka Partitioning 완벽 가이드

## 목차
1. [파티션이란?](#파티션이란)
2. [파티션이 필요한 이유](#파티션이-필요한-이유)
3. [파티셔닝 방식](#파티셔닝-방식)
4. [파티션 설정](#파티션-설정)
5. [실전 예제](#실전-예제)
6. [파티션 개수 결정 가이드](#파티션-개수-결정-가이드)

---

## 파티션이란?

### 개념
파티션은 Kafka 토픽을 **물리적으로 분할**한 저장 단위입니다.

```
Topic: sales-orders
├─ Partition 0: [메시지1, 메시지4, 메시지7, ...]
├─ Partition 1: [메시지2, 메시지5, 메시지8, ...]
└─ Partition 2: [메시지3, 메시지6, 메시지9, ...]
```

### 핵심 특징
- 각 파티션은 **독립적인 로그 파일**
- 파티션 내에서만 **메시지 순서 보장**
- 각 파티션은 **별도의 Consumer**가 처리 가능

---

## 파티션이 필요한 이유

### 1. 병렬 처리 (Parallelism)

#### 파티션 1개
```
Topic → [Partition 0] → Consumer 1
                         (처리 속도: 100 msg/sec)
```

#### 파티션 3개
```
Topic → [Partition 0] → Consumer 1 (100 msg/sec)
     → [Partition 1] → Consumer 2 (100 msg/sec)
     → [Partition 2] → Consumer 3 (100 msg/sec)
                       총 처리 속도: 300 msg/sec
```

### 2. 확장성 (Scalability)

```yaml
# 트래픽 증가에 따른 확장
초기: 파티션 3개 → Consumer 3개
증가: 파티션 10개 → Consumer 10개 (동적 확장)
```

### 3. 순서 보장 범위 조절

```
파티션 1개: 전역 순서 보장 (느림, 병목)
파티션 N개: 파티션 내에서만 순서 보장 (빠름, 확장 가능)
```

**예시**: 사용자별 주문 이력
- 같은 사용자의 주문은 같은 파티션 → 순서 보장
- 다른 사용자 간에는 순서 무관 → 병렬 처리

### 4. 장애 격리

```
파티션 10개 중 1개 장애 발생
→ 나머지 9개는 정상 동작 (90% 가용성 유지)
```

---

## 파티셔닝 방식

### 1. Key 기반 파티셔닝 (Hash Partitioning)

#### 원리
```java
partition = hash(key) % partitionCount
```

#### 코드 예제
```java
// OrderService.java
kafkaProducer.sendMessage(
    orderId,     // Key: 같은 orderId는 항상 같은 파티션
    message,     // Value
    MessageCategory.SALES_ORDER
);
```

#### 실제 동작
```
orderId: "ORD-ABC123" → hash → 12345 → 12345 % 3 = 0 → Partition 0
orderId: "ORD-DEF456" → hash → 67890 → 67890 % 3 = 0 → Partition 0
orderId: "ORD-GHI789" → hash → 45678 → 45678 % 3 = 1 → Partition 1
orderId: "ORD-JKL012" → hash → 89012 → 89012 % 3 = 2 → Partition 2
```

#### 특징
- **일관성**: 같은 Key는 항상 같은 파티션
- **순서 보장**: Key별로 메시지 순서 보장
- **균등 분산**: 대량 데이터에서 거의 균등하게 분배

#### 사용 시나리오
```java
// 1. 사용자별 주문 (userId를 Key로)
kafkaProducer.sendMessage(userId, orderMessage, MessageCategory.SALES_ORDER);

// 2. 디바이스별 로그 (deviceId를 Key로)
kafkaProducer.sendMessage(deviceId, logMessage, MessageCategory.DEVICE_LOG);

// 3. 세션별 이벤트 (sessionId를 Key로)
kafkaProducer.sendMessage(sessionId, eventMessage, MessageCategory.USER_EVENT);
```

---

### 2. Round-Robin 파티셔닝

#### 원리
Key 없이 전송하면 자동으로 순환 분배

#### 코드 예제
```java
// KafkaProducerCluster.java
kafkaProducer.sendMessage(
    message,                     // Key 없음
    MessageCategory.SALES_ORDER
);
```

#### 실제 동작
```
Message 1 → Partition 0
Message 2 → Partition 1
Message 3 → Partition 2
Message 4 → Partition 0
Message 5 → Partition 1
Message 6 → Partition 2
...
```

#### 특징
- **균등 분산**: 완벽하게 균등 분배
- **순서 없음**: 메시지 순서 보장 안 됨
- **최대 처리량**: 병렬 처리 극대화

#### 사용 시나리오
```java
// 1. 로그 수집 (순서 무관)
kafkaProducer.sendMessage(logData, MessageCategory.SYSTEM_LOG);

// 2. 메트릭 수집 (순서 무관)
kafkaProducer.sendMessage(metricsData, MessageCategory.METRICS);

// 3. 알림 발송 (순서 무관)
kafkaProducer.sendMessage(notification, MessageCategory.NOTIFICATION);
```

---

### 3. 명시적 파티션 지정

#### 코드 예제
```java
// KafkaProducerCluster.java
kafkaProducer.sendMessageToPartition(
    message,
    MessageCategory.SALES_ORDER,
    1  // 파티션 1번으로 강제 전송
);
```

#### 특징
- **정확한 제어**: 개발자가 직접 파티션 선택
- **테스트 용이**: 특정 파티션만 테스트 가능
- **주의 필요**: 잘못 사용 시 불균등 분배

#### 사용 시나리오
```java
// 1. 우선순위 처리 (VIP 고객은 전용 파티션)
int partition = isVipCustomer ? 0 : (hash(userId) % 2) + 1;
kafkaProducer.sendMessageToPartition(order, topic, partition);

// 2. 지역별 처리
int partition = getPartitionByRegion(region); // 서울:0, 부산:1, 대구:2
kafkaProducer.sendMessageToPartition(order, topic, partition);

// 3. 테스트 환경
kafkaProducer.sendMessageToPartition(testMessage, topic, 0);
```

---

## 파티션 설정

### 1. application.yml 설정

```yaml
kafka:
  topics:
    # 판매 주문 토픽
    - category: SALES_ORDER
      name: sales-orders
      partitions: 3              # 파티션 개수
      replication-factor: 1      # 복제 계수

    # 고액 주문 토픽
    - category: HIGH_VALUE_ORDERS
      name: high-value-orders
      partitions: 1              # 순서 보장이 중요하면 1개
      replication-factor: 1

    # 로그 수집 토픽
    - category: SYSTEM_LOG
      name: system-logs
      partitions: 10             # 대용량 처리는 많게
      replication-factor: 1
```

### 2. Producer 설정

```java
@Component
@RequiredArgsConstructor
public class KafkaProducerCluster {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Key 기반 파티셔닝
     */
    public void sendMessage(String key, Object data, String topic) {
        Message<Object> message = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, key)  // Key 지정
                .build();

        kafkaTemplate.send(message);
    }

    /**
     * Round-Robin 파티셔닝
     */
    public void sendMessage(Object data, String topic) {
        Message<Object> message = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, topic)
                // Key 없음 → Round-Robin
                .build();

        kafkaTemplate.send(message);
    }

    /**
     * 명시적 파티션 지정
     */
    public void sendMessageToPartition(Object data, String topic, int partition) {
        Message<Object> message = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.PARTITION, partition)  // 파티션 지정
                .build();

        kafkaTemplate.send(message);
    }
}
```

### 3. Consumer 설정

```yaml
kafka:
  consumer:
    concurrency: 3  # Consumer 스레드 수 (파티션 수와 동일하게 설정 권장)
```

```java
@KafkaListener(
    topics = "sales-orders",
    groupId = "order-processing-group",
    concurrency = "3"  // 3개의 Consumer 인스턴스 생성
)
public void consume(ConsumerRecord<String, String> record) {
    log.info("Partition: {}, Offset: {}, Key: {}, Value: {}",
            record.partition(),
            record.offset(),
            record.key(),
            record.value());
}
```

---

## 실전 예제

### 시나리오 1: 사용자별 주문 처리

#### 요구사항
- 같은 사용자의 주문은 순서대로 처리
- 다른 사용자는 병렬 처리

#### 구현
```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final KafkaProducerCluster kafkaProducer;

    public void createOrder(Order order) {
        // userId를 Key로 사용 → 같은 사용자는 같은 파티션
        kafkaProducer.sendMessage(
            order.getUserId(),  // Key
            order,
            MessageCategory.SALES_ORDER
        );
    }
}
```

#### 결과
```
User A의 주문:
  Order-1 (userId: A) → Partition 0
  Order-2 (userId: A) → Partition 0  ← 같은 파티션, 순서 보장
  Order-3 (userId: A) → Partition 0

User B의 주문:
  Order-4 (userId: B) → Partition 1  ← 다른 파티션, 병렬 처리
  Order-5 (userId: B) → Partition 1

User C의 주문:
  Order-6 (userId: C) → Partition 2  ← 다른 파티션, 병렬 처리
```

---

### 시나리오 2: 로그 수집 (순서 무관)

#### 요구사항
- 최대 처리량
- 순서 보장 불필요

#### 구현
```java
@Service
@RequiredArgsConstructor
public class LogService {

    private final KafkaProducerCluster kafkaProducer;

    public void sendLog(LogMessage log) {
        // Key 없이 전송 → Round-Robin
        kafkaProducer.sendMessage(
            log,
            MessageCategory.SYSTEM_LOG
        );
    }
}
```

#### 결과
```
Log 1 → Partition 0
Log 2 → Partition 1
Log 3 → Partition 2
Log 4 → Partition 3
Log 5 → Partition 4
...
완벽한 균등 분산, 최대 처리량
```

---

### 시나리오 3: 지역별 주문 처리

#### 요구사항
- 지역별로 별도 파티션
- 지역별 독립 처리

#### 구현
```java
@Service
@RequiredArgsConstructor
public class RegionalOrderService {

    private final KafkaProducerCluster kafkaProducer;

    private int getPartitionByRegion(String region) {
        switch (region) {
            case "SEOUL": return 0;
            case "BUSAN": return 1;
            case "DAEGU": return 2;
            default: return 0;
        }
    }

    public void createOrder(Order order) {
        int partition = getPartitionByRegion(order.getRegion());

        kafkaProducer.sendMessageToPartition(
            order,
            MessageCategory.REGIONAL_ORDER,
            partition
        );
    }
}
```

#### 결과
```
서울 주문 → Partition 0 → 서울 전용 Consumer
부산 주문 → Partition 1 → 부산 전용 Consumer
대구 주문 → Partition 2 → 대구 전용 Consumer
```

---

## 파티션 개수 결정 가이드

### 1. 처리량 기반 계산

```
필요 파티션 수 = max(
    목표 처리량 / Consumer 처리 속도,
    목표 처리량 / Producer 처리 속도
)
```

#### 예시
```
목표 처리량: 10,000 msg/sec
Consumer 처리 속도: 1,000 msg/sec
Producer 처리 속도: 2,000 msg/sec

필요 파티션 = max(10,000/1,000, 10,000/2,000)
            = max(10, 5)
            = 10개
```

---

### 2. 사용 사례별 권장 설정

#### 저용량 (초당 수십 건)
```yaml
partitions: 1
replication-factor: 1
```
- 순서 보장 필요
- 처리량 낮음
- 예: 관리자 알림, 시스템 설정 변경

#### 중간 용량 (초당 수백~수천 건)
```yaml
partitions: 3-5
replication-factor: 2
```
- 적당한 병렬 처리
- 일부 순서 보장
- 예: 주문 처리, 사용자 이벤트

#### 대용량 (초당 수만 건)
```yaml
partitions: 10-50
replication-factor: 3
```
- 높은 처리량
- 순서 보장 불필요
- 예: 로그 수집, 메트릭 수집, 클릭 스트림

---

### 3. 파티션 개수 변경

#### 주의사항
```
파티션 개수 증가: 가능 (단, 기존 Key 분배 변경됨)
파티션 개수 감소: 불가능
```

#### 변경 방법
```bash
# Kafka CLI 사용
kafka-topics.sh --alter \
  --bootstrap-server localhost:9092 \
  --topic sales-orders \
  --partitions 10

# 또는 application.yml 변경 후 재시작
```

#### 영향
```java
// 기존 (3개 파티션)
hash("ORD-ABC") % 3 = 0  → Partition 0

// 변경 후 (10개 파티션)
hash("ORD-ABC") % 10 = 5 → Partition 5  ← 변경됨!
```

**결론**: 초기 설계 시 충분히 고려 필요

---

## 파티션 모니터링

### 1. 로그 확인

```java
@KafkaListener(topics = "sales-orders")
public void consume(ConsumerRecord<String, String> record) {
    log.info("Partition: {}, Offset: {}, Key: {}, Value: {}",
            record.partition(),  // 파티션 번호
            record.offset(),     // Offset
            record.key(),        // Key
            record.value());     // Value
}
```

### 2. 파티션별 Lag 확인

```bash
# Consumer Group의 Lag 확인
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group order-processing-group \
  --describe

# 출력 예시:
# TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
# sales-orders    0          1000            1000            0
# sales-orders    1          950             1000            50   ← Lag 발생
# sales-orders    2          1000            1000            0
```

### 3. 파티션별 분포 확인

```java
@Service
@RequiredArgsConstructor
public class PartitionMonitorService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void checkPartitionDistribution() {
        Map<Integer, Integer> distribution = new HashMap<>();

        for (int i = 0; i < 1000; i++) {
            String key = "ORD-" + UUID.randomUUID();

            kafkaTemplate.send("sales-orders", key, "test")
                .whenComplete((result, ex) -> {
                    int partition = result.getRecordMetadata().partition();
                    distribution.merge(partition, 1, Integer::sum);
                });
        }

        // 분포 출력
        distribution.forEach((partition, count) ->
            log.info("Partition {}: {} messages ({}%)",
                partition, count, (count * 100.0 / 1000)));
    }
}

// 출력 예시:
// Partition 0: 334 messages (33.4%)
// Partition 1: 333 messages (33.3%)
// Partition 2: 333 messages (33.3%)
```

---

## 베스트 프랙티스

### 1. Key 설계
```java
// ✅ Good: 비즈니스 의미가 있는 Key
kafkaProducer.sendMessage(userId, order, topic);
kafkaProducer.sendMessage(customerId, transaction, topic);
kafkaProducer.sendMessage(sessionId, event, topic);

// ❌ Bad: 랜덤 Key (순서 보장 불가)
kafkaProducer.sendMessage(UUID.randomUUID().toString(), order, topic);
```

### 2. 파티션 개수
```yaml
# ✅ Good: Consumer 수와 일치
partitions: 3
concurrency: 3

# ❌ Bad: Consumer 수보다 적음 (일부 Consumer 유휴)
partitions: 2
concurrency: 5
```

### 3. 순서 보장
```java
// ✅ Good: Key 기반 파티셔닝
kafkaProducer.sendMessage(orderId, orderCreated, topic);
kafkaProducer.sendMessage(orderId, orderPaid, topic);
kafkaProducer.sendMessage(orderId, orderShipped, topic);
// → 같은 파티션, 순서 보장

// ❌ Bad: Key 없음 (순서 보장 안 됨)
kafkaProducer.sendMessage(orderCreated, topic);
kafkaProducer.sendMessage(orderPaid, topic);
kafkaProducer.sendMessage(orderShipped, topic);
// → 다른 파티션, 순서 보장 안 됨
```

### 4. 확장성
```yaml
# ✅ Good: 충분한 파티션 (확장 가능)
partitions: 10
replication-factor: 3

# ❌ Bad: 파티션 부족 (확장 어려움)
partitions: 1
replication-factor: 1
```

---

## 문제 해결

### 문제 1: 특정 파티션에만 메시지 쌓임

#### 원인
```java
// 항상 같은 Key 사용
kafkaProducer.sendMessage("FIXED_KEY", order, topic);
```

#### 해결
```java
// 다양한 Key 사용
kafkaProducer.sendMessage(order.getUserId(), order, topic);
```

---

### 문제 2: Consumer Lag 발생

#### 원인
```yaml
# Consumer 수 < 파티션 수
partitions: 10
concurrency: 3  # 7개 파티션이 처리 대기
```

#### 해결
```yaml
# Consumer 수 = 파티션 수
partitions: 10
concurrency: 10
```

---

### 문제 3: 순서 보장 안 됨

#### 원인
```java
// Key 없이 전송
kafkaProducer.sendMessage(order, topic);
```

#### 해결
```java
// Key 사용
kafkaProducer.sendMessage(order.getUserId(), order, topic);
```

---

## 요약

### 핵심 개념
1. **파티션**: 토픽의 물리적 분할 단위
2. **파티셔닝**: Key 기반, Round-Robin, 명시적 지정
3. **순서 보장**: 파티션 내에서만 보장
4. **병렬 처리**: 파티션 수 = 최대 Consumer 수

### 선택 가이드
```
순서 보장 필요 → Key 기반 파티셔닝 (파티션 3-5개)
순서 무관      → Round-Robin (파티션 10개 이상)
특수 요구사항  → 명시적 파티션 지정
```

### 성능 최적화
```yaml
# 처리량 우선
partitions: 많게 (10개 이상)
concurrency: 파티션 수와 동일

# 순서 보장 우선
partitions: 적게 (1-3개)
key: 비즈니스 키 사용
```
