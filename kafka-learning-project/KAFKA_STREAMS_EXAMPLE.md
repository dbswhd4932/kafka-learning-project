# Kafka Streams 예제 가이드

## Kafka Streams란?

### 정의

**Kafka Streams**는 Apache Kafka에서 제공하는 클라이언트 라이브러리로, 실시간 스트림 데이터를 처리하고 분석하는 프레임워크입니다.

- **별도 클러스터 불필요**: Kafka Broker만 있으면 동작 (Spark, Flink와 차이점)
- **Java/Scala 애플리케이션**: Spring Boot에 임베드하여 사용 가능
- **Exactly-Once 보장**: 중복 없는 메시지 처리
- **Stateful & Stateless 처리**: 상태 저장/비저장 모두 지원

### Consumer와의 차이점

| 비교 항목 | Kafka Consumer | Kafka Streams |
|----------|----------------|---------------|
| **용도** | 메시지 소비만 | 소비 + 변환 + 발행 |
| **처리 방식** | 단순 읽기/처리 | 복잡한 스트림 파이프라인 |
| **상태 관리** | 직접 구현 필요 | 내장 State Store 제공 |
| **윈도우 집계** | 직접 구현 필요 | 내장 Windowing API |
| **토픽 간 변환** | 수동 발행 | 자동 파이프라인 |

**예시**:
```
Consumer: sales-orders 읽기 → 비즈니스 로직 처리 → DB 저장
Streams:  sales-orders 읽기 → 필터링 → high-value-orders 발행 (자동)
```

### 언제 사용하면 좋은가?

#### 1. 실시간 데이터 필터링 및 라우팅
```
원본 토픽 → [조건 필터링] → 여러 타겟 토픽 분산
예: 주문 → 고액/일반 주문 분리, 지역별 주문 분리
```

#### 2. 실시간 데이터 변환 (ETL)
```
원본 데이터 → [변환/집계/조인] → 변환된 데이터
예: 클릭 로그 → 세션별 집계, JSON → Avro 변환
```

#### 3. 시계열 데이터 집계
```
이벤트 스트림 → [시간 윈도우 집계] → 통계 데이터
예: 1분 단위 매출 합계, 5분 단위 API 호출 수
```

#### 4. 복잡한 이벤트 처리 (CEP)
```
여러 토픽 → [조인/패턴 매칭] → 비즈니스 이벤트
예: 주문 + 결제 + 배송 조인, 사기 패턴 탐지
```

#### 5. 실시간 대시보드 데이터 준비
```
원본 이벤트 → [집계/변환] → 대시보드용 데이터
예: 실시간 매출 현황, 실시간 재고 현황
```

### 실무 사용 사례

#### 금융권
- **사기 탐지**: 비정상 거래 패턴 실시간 감지
- **리스크 관리**: 실시간 포트폴리오 위험도 계산

#### 이커머스
- **개인화 추천**: 실시간 사용자 행동 기반 추천
- **재고 관리**: 주문 이벤트 기반 실시간 재고 차감

#### 광고 플랫폼
- **실시간 입찰**: 광고 노출 요청 실시간 처리
- **CTR 집계**: 시간대별 클릭률 실시간 계산

#### IoT
- **센서 데이터 집계**: 디바이스별 시계열 데이터 집계
- **이상 탐지**: 센서 값 임계치 초과 감지

### 사용하지 말아야 할 때

❌ **단순 CRUD**: DB 읽기/쓰기만 필요한 경우 → Consumer 사용
❌ **배치 처리**: 실시간이 아닌 주기적 처리 → Spark/Airflow 사용
❌ **복잡한 머신러닝**: 모델 학습/추론 → Spark MLlib/TensorFlow 사용
❌ **일회성 데이터 이동**: 단발성 마이그레이션 → Kafka Connect 사용

## 개요

이 문서는 `HighValueOrderStream`을 활용한 실시간 고액 주문 필터링 예제를 설명합니다.

### 왜 Kafka Streams를 사용했는가?

**기존 방식 (Consumer만 사용)**:
```java
@KafkaListener(topics = "sales-orders")
public void consume(Order order) {
    if (order.getTotalAmount() >= 1000000) {
        // VIP 처리 로직
        notifyVIP(order);
        processSpecialDelivery(order);
    } else {
        // 일반 처리 로직
        processNormalOrder(order);
    }
}
```
❌ **문제점**:
- 하나의 Consumer에 모든 로직이 섞임
- 고액/일반 주문 처리 스레드 풀 공유 (성능 저하)
- 재사용 불가능 (다른 시스템에서 고액 주문만 구독 불가)

**Kafka Streams 사용**:
```java
// 1. Streams: 필터링만 담당
sales-orders → [필터링] → high-value-orders

// 2. Consumer 1: 일반 주문 처리
@KafkaListener(topics = "sales-orders")

// 3. Consumer 2: 고액 주문 전용 처리 (독립 스레드 풀)
@KafkaListener(topics = "high-value-orders")
```
✅ **장점**:
- 관심사 분리 (필터링 vs 처리)
- 독립적인 스케일링 (고액 주문 Consumer만 증설 가능)
- 재사용성 (다른 시스템도 high-value-orders 구독 가능)
- 데이터 파이프라인 명확화

## 아키텍처

```
[주문 생성]
    ↓
OrderService → sales-orders 토픽
    ↓
    ├─→ [기본 Consumer] OrderConsumer (kafka-learning-group)
    │   └─ 모든 주문 처리
    │
    └─→ [Kafka Streams] HighValueOrderStream
        └─ 100만원 이상 필터링
            ↓
        high-value-orders 토픽
            ↓
        [전문 Consumer] HighValueOrderConsumer
        └─ VIP 고객 알림
        └─ 관리자 알림
        └─ 특별 배송 처리
        └─ 사기 거래 검증
```

## 코드 구조

### 1. HighValueOrderStream (Kafka Streams)

**역할**: 실시간으로 고액 주문을 필터링

**위치**: `com.example.kafka.streams.HighValueOrderStream`

**처리 로직**:
```java
sales-orders 토픽에서 읽기
→ JSON을 SalesOrderMessage로 변환
→ 100만원 이상 필터링
→ high-value-orders 토픽으로 발행
```

**주요 코드**:
```java
sourceStream
    .mapValues(this::parseOrderMessage)
    .filter((key, order) -> order != null)
    .filter((key, order) -> {
        BigDecimal threshold = new BigDecimal("1000000");
        return order.getTotalAmount().compareTo(threshold) >= 0;
    })
    .mapValues(this::toJson)
    .to("high-value-orders");
```

### 2. HighValueOrderConsumer (Consumer)

**역할**: 고액 주문을 전문적으로 처리

**위치**: `com.example.kafka.consumer.HighValueOrderConsumer`

**처리 기능**:
1. **주문 정보 로깅**: 상세 주문 정보 기록
2. **VIP 고객 알림**: SMS, 이메일, 푸시 알림 발송
3. **관리자 알림**: Slack, 관리자 대시보드 알림
4. **특별 배송 처리**: VIP 프리미엄 배송 적용
5. **사기 거래 검증**: 500만원 이상 추가 보안 검증

## 실행 방법

### 1. 애플리케이션 시작

```bash
./gradlew bootRun
```

### 2. 고액 주문 생성 (100만원 이상)

```bash
# 150만원 노트북 주문
curl -X POST http://localhost:8090/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "VIP-001",
    "productName": "맥북 프로",
    "quantity": 1,
    "price": 1500000
  }'
```

### 3. 일반 주문 생성 (100만원 미만)

```bash
# 15만원 키보드 주문 (필터링됨)
curl -X POST http://localhost:8090/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "USER-001",
    "productName": "키보드",
    "quantity": 1,
    "price": 150000
  }'
```

## 로그 확인

### 고액 주문 처리 로그

```
[Kafka Streams - HighValueOrderStream]
💰 고액 주문 감지: ORD-XXX - 맥북 프로 (1500000원)

[Consumer - HighValueOrderConsumer]
========================================
💎 고액 주문 수신: ORD-XXX
========================================
📋 주문 상세:
  - 주문 ID: ORD-XXX
  - 고객 ID: VIP-001
  - 상품명: 맥북 프로
  - 수량: 1개
  - 단가: 1500000원
  - 총 금액: 1500000원
  - 주문 시간: 2025-10-24T...

📱 VIP 고객 알림 전송:
  → 고객 ID: VIP-001
  → 메시지: '맥북 프로' 주문이 정상적으로 접수되었습니다.
  → 특별 혜택: VIP 전용 포장 + 빠른 배송

🔔 관리자 알림:
  → 고액 주문 발생!
  → 주문 ID: ORD-XXX
  → 금액: 1500000원
  → 즉시 확인 필요

🚚 특별 배송 처리:
  → 배송 등급: VIP 프리미엄
  → 예상 배송: 익일 새벽 배송
  → 포장: 고급 선물 포장
  → 배송 추적: 실시간 GPS 추적 제공

🔍 사기 거래 검증:
  → 정상 거래로 판단
  → 검증 통과 ✅

✅ 고액 주문 처리 완료: ORD-XXX
========================================
```

### 일반 주문 처리 로그

```
[Kafka Streams - HighValueOrderStream]
(필터링됨 - 로그 없음)

[Consumer - OrderConsumer]
Consumed Order from Kafka
Order: Order(orderId=ORD-YYY, customerId=USER-001, ...)
Processing order: ORD-YYY
```

## Kafka CLI로 확인

### 1. high-value-orders 토픽 확인

```bash
docker exec -it kafka bash

kafka-console-consumer \
  --topic high-value-orders \
  --bootstrap-server localhost:29092 \
  --from-beginning
```

**출력 예시**:
```json
{
  "orderId": "ORD-XXX",
  "customerId": "VIP-001",
  "productName": "맥북 프로",
  "quantity": 1,
  "price": 1500000,
  "totalAmount": 1500000,
  "status": "SUCCESS",
  "orderDateTime": "2025-10-24T19:51:37.462690"
}
```

### 2. Consumer Group 확인

```bash
kafka-consumer-groups \
  --describe \
  --group high-value-order-group \
  --bootstrap-server localhost:29092
```

## 확장 아이디어

### 1. 사기 거래 탐지 강화
```java
// ML 모델 연동
boolean isFraud = fraudDetectionService.predict(order);
if (isFraud) {
    securityService.blockOrder(order.getOrderId());
    adminService.alertSecurityTeam(order);
}
```

### 2. 실시간 VIP 등급 자동 조정
```java
// 누적 구매액 계산
BigDecimal totalPurchase = customerService.getTotalPurchase(order.getCustomerId());
if (totalPurchase.compareTo(new BigDecimal("10000000")) >= 0) {
    customerService.upgradeToVIP(order.getCustomerId());
}
```

### 3. 재고 자동 확인
```java
// 고액 주문 시 재고 우선 확보
inventoryService.reserveStock(order.getProductName(), order.getQuantity());
```

### 4. 다단계 알림 시스템
```java
// 금액별 차등 알림
if (totalAmount >= 5000000) {
    notifyService.sendToCEO(order);
} else if (totalAmount >= 3000000) {
    notifyService.sendToManager(order);
} else {
    notifyService.sendToTeam(order);
}
```

## 성능 최적화

### 1. Kafka Streams 병렬 처리
```yaml
# application.yml
spring:
  kafka:
    streams:
      replication-factor: 3  # 안정성
      num-stream-threads: 3  # 병렬 처리
```

### 2. Consumer 스케일링
```java
@KafkaListener(
    topics = "high-value-orders",
    groupId = "high-value-order-group",
    concurrency = "5"  // 5개 스레드 병렬 처리
)
```

## 트러블슈팅

### Q1: Kafka Streams가 메시지를 소비하지 않음

**해결**:
1. Kafka Streams 로그 확인: `kafka-streams-app` 상태 확인
2. 토픽 존재 확인: `kafka-topics --list`
3. Offset 리셋: StateStore 디렉토리 삭제 `/tmp/kafka-streams`

### Q2: Consumer가 중복 메시지 수신

**해결**:
- Consumer Group ID 확인: 같은 Group ID는 메시지 분산
- 다른 Group ID는 모든 메시지 수신 (의도된 동작)

### Q3: 필터링이 작동하지 않음

**해결**:
- `HighValueOrderStream` 초기화 로그 확인
- 주문 금액 확인: 100만원 이상인지 체크
- JSON 파싱 오류 확인: 로그에서 에러 메시지 확인

## 참고 자료

- [Kafka Streams Documentation](https://kafka.apache.org/documentation/streams/)
- [Spring Kafka Streams](https://docs.spring.io/spring-kafka/reference/streams.html)
- [Stateless vs Stateful Processing](https://kafka.apache.org/documentation/streams/developer-guide/dsl-api.html)
