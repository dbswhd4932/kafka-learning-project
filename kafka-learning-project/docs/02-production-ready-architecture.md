# 프로덕션 수준의 Kafka 아키텍처 개선

## 개선 개요

실제 서비스 환경에서 사용할 수 있는 수준으로 Kafka 설정을 개선했습니다.

## 주요 개선 사항

### 1. ConfigurationProperties 도입

#### Before (기존 코드)
```java
@Value("${spring.kafka.bootstrap-servers}")
private String bootstrapServers;
```

#### After (개선 코드)
```java
@Component
@ConfigurationProperties(prefix = "kafka.producer")
public class KafkaProducerProperties {
    private String bootstrapServers;
    private String acks = "all";
    private Integer retries = 3;
    // ...
}
```

**장점:**
- ✅ 타입 안전성 보장
- ✅ IDE 자동완성 지원
- ✅ 설정 그룹화 및 체계적 관리
- ✅ `@Validated`를 통한 설정 검증 가능

---

### 2. Enum 기반 토픽 관리

#### Before (기존 코드)
```java
private static final String TOPIC = "orders";
kafkaTemplate.send(TOPIC, order);
```

#### After (개선 코드)
```java
public enum MessageCategory {
    SALES_ORDER("판매 주문"),
    RETURN_ORDER("반품 주문"),
    CANCEL_ORDER("취소 주문");
}

kafkaProducerCluster.sendMessage(order, MessageCategory.SALES_ORDER);
```

**장점:**
- ✅ 하드코딩 제거
- ✅ 컴파일 타임 타입 체크
- ✅ 토픽명 오타 방지
- ✅ 환경별 토픽명 관리 용이 (dev, stage, prod)

---

### 3. 범용 KafkaTemplate

#### Before (기존 코드)
```java
private final KafkaTemplate<String, Order> kafkaTemplate;  // Order 전용
```

#### After (개선 코드)
```java
private final KafkaTemplate<String, Object> kafkaTemplate;  // 모든 타입 지원
```

**장점:**
- ✅ 재사용성 향상
- ✅ 여러 도메인 객체 전송 가능
- ✅ 도메인마다 별도 KafkaTemplate 불필요

---

### 4. MessageBuilder 패턴

#### Before (기존 코드)
```java
kafkaTemplate.send(TOPIC, key, value);
```

#### After (개선 코드)
```java
Message<Object> message = MessageBuilder
    .withPayload(data)
    .setHeader(KafkaHeaders.TOPIC, topic)
    .setHeader(KafkaHeaders.KEY, key)
    .build();

kafkaTemplate.send(message);
```

**장점:**
- ✅ Spring Messaging 추상화 활용
- ✅ 헤더 커스터마이징 용이
- ✅ 동적 토픽 지정 가능
- ✅ 메타데이터 추가 간편

---

### 5. 범용 Producer 클러스터

#### Before (기존 코드)
```java
@Service
public class OrderProducer {
    public void sendOrder(Order order) {
        kafkaTemplate.send("orders", order.getOrderId(), order);
    }
}
```

#### After (개선 코드)
```java
@Component
public class KafkaProducerCluster {
    public void sendMessage(Object data, MessageCategory category) {
        String topicName = topicProperties.getName(category);
        // ... 범용 전송 로직
    }
}

@Service
public class OrderProducer {
    private final KafkaProducerCluster kafkaProducerCluster;

    public void sendOrder(Order order) {
        kafkaProducerCluster.sendMessage(order, MessageCategory.SALES_ORDER);
    }
}
```

**장점:**
- ✅ 단일 책임 원칙 (SRP) 준수
- ✅ Kafka 로직 중앙 집중화
- ✅ 모든 도메인에서 재사용
- ✅ 유지보수 용이

---

### 6. SSL/SASL 보안 설정

#### 새로 추가된 기능
```yaml
kafka:
  ssl:
    enabled: false
    security-protocol: PLAINTEXT  # SSL, SASL_SSL 등
    sasl-mechanism: SCRAM-SHA-256
    sasl-jaas-config: ...
    truststore-location: /path/to/truststore.jks
    keystore-location: /path/to/keystore.jks
```

**장점:**
- ✅ 프로덕션 보안 요구사항 충족
- ✅ SASL 인증 지원
- ✅ SSL/TLS 암호화 지원
- ✅ 환경별 활성화/비활성화 가능

---

### 7. YAML 기반 토픽 설정

#### Before (기존 코드)
```java
@Bean
public NewTopic ordersTopic() {
    return new NewTopic("orders", 3, (short) 1);
}
```

#### After (개선 코드)
```yaml
kafka:
  topics:
    - category: SALES_ORDER
      name: sales-orders
      partitions: 3
      replication-factor: 1
    - category: RETURN_ORDER
      name: return-orders
      partitions: 3
      replication-factor: 1
```

**장점:**
- ✅ 코드 수정 없이 토픽 추가 가능
- ✅ 환경별 설정 분리 (application-dev.yml, application-prod.yml)
- ✅ 파티션 수, 복제 팩터 관리 용이
- ✅ 동적 토픽 생성

---

## 프로젝트 구조 (개선 후)

```
src/main/java/com/example/kafka/
├── common/
│   └── KafkaProducerCluster.java      # 범용 Producer (모든 도메인 사용)
├── config/
│   ├── KafkaProducerConfig.java       # Producer 설정 (ConfigurationProperties 사용)
│   ├── KafkaConsumerConfig.java       # Consumer 설정
│   └── KafkaTopicConfig.java          # 동적 토픽 생성
├── properties/
│   ├── KafkaProducerProperties.java   # Producer 설정 Properties
│   ├── KafkaConsumerProperties.java   # Consumer 설정 Properties
│   ├── KafkaSSLProperties.java        # SSL/Security Properties
│   ├── KafkaTopicProperties.java      # Topic 관리 Properties
│   └── KafkaTopic.java                # Topic 모델
├── enums/
│   └── MessageCategory.java           # 토픽 카테고리 Enum
├── domain/
│   └── Order.java                     # 도메인 모델
├── producer/
│   └── OrderProducer.java             # 비즈니스 로직 집중
└── consumer/
    └── OrderConsumer.java             # Consumer 로직
```

---

## 설정 파일 (application.yml)

### 전체 구조

```yaml
# Kafka Producer 설정
kafka:
  producer:
    bootstrap-servers: localhost:9092
    acks: all
    retries: 3
    batch-size: 16384
    compression-type: none

  # Kafka Consumer 설정
  consumer:
    bootstrap-servers: localhost:9092
    group-id: kafka-learning-group
    auto-offset-reset: earliest
    max-poll-records: 500
    concurrency: 3

  # SSL/Security 설정
  ssl:
    enabled: false
    security-protocol: PLAINTEXT

  # Topic 설정 (Enum 매핑)
  topics:
    - category: SALES_ORDER
      name: sales-orders
      partitions: 3
      replication-factor: 1
```

---

## 사용 예제

### 1. Producer 사용

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderProducer orderProducer;

    public void createOrder(Order order) {
        // 비즈니스 로직
        order.setStatus("PENDING");

        // 판매 주문 이벤트 발행
        orderProducer.sendOrder(order);

        // 고액 주문이면 알림 발행
        orderProducer.sendHighValueOrderNotification(order);
    }

    public void returnOrder(Order order) {
        // 반품 처리 로직
        order.setStatus("RETURNED");

        // 반품 주문 이벤트 발행
        orderProducer.sendReturnOrder(order);
    }
}
```

### 2. 다른 도메인에서 KafkaProducerCluster 직접 사용

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final KafkaProducerCluster kafkaProducerCluster;

    public void createUser(User user) {
        // 비즈니스 로직

        // Kafka로 이벤트 발행 (범용 클러스터 사용)
        kafkaProducerCluster.sendMessage(
            user.getId(),
            user,
            MessageCategory.USER_CREATED  // 새로운 카테고리 추가 가능
        );
    }
}
```

---

## 환경별 설정 관리

### 로컬 환경 (application-local.yml)
```yaml
kafka:
  producer:
    bootstrap-servers: localhost:9092
  ssl:
    enabled: false
  topics:
    - category: SALES_ORDER
      name: local-sales-orders
```

### 개발 환경 (application-dev.yml)
```yaml
kafka:
  producer:
    bootstrap-servers: kafka-dev.example.com:9092
  ssl:
    enabled: false
  topics:
    - category: SALES_ORDER
      name: dev-sales-orders
```

### 프로덕션 환경 (application-prod.yml)
```yaml
kafka:
  producer:
    bootstrap-servers: kafka-prod.example.com:9093
    compression-type: gzip  # 프로덕션에서는 압축 활성화
  ssl:
    enabled: true
    security-protocol: SASL_SSL
    sasl-mechanism: SCRAM-SHA-256
    sasl-jaas-config: ...
  topics:
    - category: SALES_ORDER
      name: prod-sales-orders
      partitions: 10  # 프로덕션에서는 파티션 증가
      replication-factor: 3  # 복제 팩터 증가
```

---

## 개선 효과 요약

| 항목 | 개선 전 | 개선 후 |
|------|--------|--------|
| **설정 관리** | @Value로 분산 | ConfigurationProperties로 집중 |
| **타입 안전성** | 낮음 (String) | 높음 (Enum + Properties) |
| **토픽 관리** | 하드코딩 | Enum + YAML |
| **재사용성** | 낮음 (도메인별 Producer) | 높음 (범용 Cluster) |
| **보안** | 없음 | SSL/SASL 지원 |
| **확장성** | 낮음 | 높음 |
| **유지보수** | 어려움 | 쉬움 |
| **프로덕션 준비도** | ❌ | ✅ |

---

## 다음 단계

이제 실제 서비스 수준의 Kafka 설정이 완료되었습니다.

다음 학습 주제:
- **Topic & Partition**: 파티션 전략 및 성능 최적화
- **Avro & Schema Registry**: 스키마 관리 및 진화
- **Kafka Streams**: 실시간 스트림 처리

---

## 참고 자료

- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties)
- [Kafka Security](https://kafka.apache.org/documentation/#security)
- [Kafka Best Practices](https://kafka.apache.org/documentation/#bestpractices)
