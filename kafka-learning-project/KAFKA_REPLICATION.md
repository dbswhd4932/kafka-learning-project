# Kafka Replication 완벽 가이드

## 목차
1. [Replication이란?](#replication이란)
2. [Leader와 Replica](#leader와-replica)
3. [ISR (In-Sync Replicas)](#isr-in-sync-replicas)
4. [ACKs 설정](#acks-설정)
5. [장애 복구 (Failover)](#장애-복구-failover)
6. [Replication 설정](#replication-설정)
7. [실무 권장 사항](#실무-권장-사항)
8. [모니터링](#모니터링)

---

## Replication이란?

### 개념
**Replication**은 파티션의 데이터를 **여러 Broker에 복제**하여 저장하는 Kafka의 고가용성 메커니즘입니다.

```
단일 저장 (Replication Factor = 1):
Broker 1: Partition 0 [데이터]
          ↓ 장애 발생
          데이터 손실 & 서비스 중단

복제 저장 (Replication Factor = 3):
Broker 1: Partition 0 [데이터] ← Leader
Broker 2: Partition 0 [데이터] ← Replica
Broker 3: Partition 0 [데이터] ← Replica
          ↓ Broker 1 장애 발생
Broker 2: Partition 0 [데이터] ← 새 Leader로 승격
Broker 3: Partition 0 [데이터] ← Replica
          서비스 계속 운영 (무중단)
```

---

## Replication Factor

### 정의
**Replication Factor**: 파티션의 복제본 개수 (원본 포함)

```yaml
kafka:
  topics:
    - category: SALES_ORDER
      name: sales-orders
      partitions: 3
      replication-factor: 3  ← 총 3개의 복사본 (원본 1 + 복제 2)
```

### 옵션별 의미

```
replication-factor: 1
→ 복제 없음 (원본만 존재)
→ 1개 Broker 장애 시 데이터 손실

Broker 1: Partition 0 (원본)
```

```
replication-factor: 2
→ 원본 1개 + 복제본 1개
→ 1개 Broker 장애 대응 가능

Broker 1: Partition 0 (Leader)
Broker 2: Partition 0 (Replica)
```

```
replication-factor: 3
→ 원본 1개 + 복제본 2개
→ 2개 Broker 동시 장애 대응 가능 (권장)

Broker 1: Partition 0 (Leader)
Broker 2: Partition 0 (Replica)
Broker 3: Partition 0 (Replica)
```

### 제약 사항

```
replication-factor는 Broker 수를 초과할 수 없음:

Broker 1개 → replication-factor: 1 (최대)
Broker 2개 → replication-factor: 2 (최대)
Broker 3개 → replication-factor: 3 (최대)
```

---

## Leader와 Replica

### 역할 분담

#### Leader
```
역할:
1. 모든 읽기 요청 처리
2. 모든 쓰기 요청 처리
3. Replica에게 데이터 전달
4. Consumer/Producer와 직접 통신

특징:
- 파티션당 1개만 존재
- 활발히 동작 (Active)
```

#### Replica (Follower)
```
역할:
1. Leader로부터 데이터 복제
2. Leader 장애 시 승격 대기
3. 읽기/쓰기 처리 안 함 (Passive)

특징:
- 파티션당 N-1개 존재 (N = replication-factor)
- 대기 상태 (Standby)
```

---

### Leader 분산 배치

Kafka는 Leader를 Broker에 균등하게 분산합니다:

```
Topic: sales-orders
Partitions: 3
Replication Factor: 3

Partition 0:
  Broker 1: Leader     ← Broker 1이 Partition 0의 Leader
  Broker 2: Replica
  Broker 3: Replica

Partition 1:
  Broker 1: Replica
  Broker 2: Leader     ← Broker 2가 Partition 1의 Leader
  Broker 3: Replica

Partition 2:
  Broker 1: Replica
  Broker 2: Replica
  Broker 3: Leader     ← Broker 3이 Partition 2의 Leader
```

**핵심**: 각 Broker는 일부 파티션의 Leader, 나머지는 Replica
→ 부하가 모든 Broker에 균등 분산

---

### Producer/Consumer의 통신

```
Producer 메시지 전송:

Producer → Broker 1 (Partition 0 Leader)
        → Broker 2 (Partition 1 Leader)
        → Broker 3 (Partition 2 Leader)
```

```
Consumer 메시지 읽기:

Consumer → Broker 1 (Partition 0 Leader)
        → Broker 2 (Partition 1 Leader)
        → Broker 3 (Partition 2 Leader)
```

**중요**: Producer/Consumer는 항상 Leader와만 통신합니다.

---

## ISR (In-Sync Replicas)

### 개념
**ISR**: Leader와 동기화 상태를 유지하는 Replica 목록

```
정상 상태:
Partition 0:
  Leader: Broker 1
  Replicas: [Broker 1, Broker 2, Broker 3]
  ISR: [Broker 1, Broker 2, Broker 3]  ← 모두 동기화됨

동기화 지연 발생:
Partition 0:
  Leader: Broker 1
  Replicas: [Broker 1, Broker 2, Broker 3]
  ISR: [Broker 1, Broker 2]            ← Broker 3 제외됨
```

---

### ISR 포함 조건

Replica가 ISR에 포함되려면:

```
1. Leader와 지속적으로 통신 (Heartbeat)
2. 최근 N초 이내에 데이터 복제 완료 (기본 10초)
3. Lag(지연)이 임계값 이하
```

설정:
```properties
# Broker 설정 (server.properties)
replica.lag.time.max.ms=10000  # 10초 이상 지연 시 ISR 제외
```

---

### ISR에서 제외되는 경우

```
원인:
1. 네트워크 지연/장애
2. Broker의 디스크 성능 저하
3. Broker의 과부하
4. Broker의 일시적 장애

결과:
- ISR 목록에서 제거
- Leader 선출 후보에서 제외
- 성능 알림 필요
```

예시:
```
시간 0초:  ISR: [Broker 1, Broker 2, Broker 3]  ← 정상
시간 5초:  Broker 3 네트워크 지연 시작
시간 15초: ISR: [Broker 1, Broker 2]            ← Broker 3 제외
시간 20초: Broker 3 복구
시간 30초: ISR: [Broker 1, Broker 2, Broker 3]  ← 다시 포함
```

---

### ISR과 Leader 선출

새 Leader는 **ISR 내에서만** 선출됩니다:

```
상황:
  Broker 1 (Leader) 장애
  ISR: [Broker 1, Broker 2, Broker 3]

결과:
  Broker 2 또는 Broker 3이 새 Leader로 선출
  (둘 다 최신 데이터 보유)
```

```
상황:
  Broker 1 (Leader) 장애
  ISR: [Broker 1, Broker 2]  ← Broker 3은 ISR 아님

결과:
  Broker 2만 새 Leader 후보
  Broker 3은 후보 제외 (데이터 누락 가능성)
```

---

## ACKs 설정

### Producer ACKs

Producer가 메시지 전송 성공을 **언제** 판단할지 결정하는 설정:

```yaml
# application.yml
kafka:
  producer:
    acks: all  # 옵션: 0, 1, all
```

---

### 옵션 상세

#### acks = 0 (Fire and Forget)

```
동작:
Producer → Broker에 전송
         ↓
       즉시 성공 반환 (응답 안 기다림)

특징:
- 최고 처리량 (성능 최우선)
- 네트워크 오버헤드 최소
- 데이터 손실 위험 매우 높음

사용 사례:
- 로그 수집 (일부 손실 허용)
- 메트릭 수집 (일부 손실 허용)
- 실시간 센서 데이터 (최신 데이터만 중요)
```

시나리오:
```
1. Producer가 메시지 전송
2. 네트워크 중간에서 메시지 손실
3. Producer는 성공으로 간주 (모름)
4. 데이터 영구 손실
```

---

#### acks = 1 (Leader Acknowledgment)

```
동작:
Producer → Broker (Leader) → Leader 디스크에 저장
                            ↓
                          성공 응답

특징:
- 중간 수준 처리량
- 중간 수준 신뢰성
- Leader 장애 시 데이터 손실 가능

사용 사례:
- 일반적인 애플리케이션 로그
- 사용자 활동 추적
- 비중요 이벤트
```

시나리오 (데이터 손실):
```
1. Producer가 메시지 전송
2. Leader가 메시지 저장
3. Leader가 Producer에게 성공 응답
4. Replica 복제 전에 Leader 장애 발생  ← 타이밍 이슈
5. ISR에 없던 Replica가 Leader로 승격
6. 데이터 손실
```

---

#### acks = all (All In-Sync Replicas)

```
동작:
Producer → Broker (Leader) → Leader + 모든 ISR 복제 완료
                            ↓
                          성공 응답

특징:
- 최저 처리량 (신뢰성 최우선)
- 최고 신뢰성
- 데이터 손실 최소화

사용 사례:
- 금융 거래
- 주문 처리
- 결제 데이터
- 법적 기록
```

시나리오:
```
1. Producer가 메시지 전송
2. Leader가 메시지 저장
3. 모든 ISR Replica가 복제 완료 대기
4. 모든 ISR 복제 완료
5. Producer에게 성공 응답
6. 이제 Leader 장애 발생해도 안전 (ISR이 최신 데이터 보유)
```

---

### ACKs와 min.insync.replicas

```properties
# Broker 설정
min.insync.replicas=2  # 최소 ISR 개수
```

#### 조합 효과

```yaml
# 설정
replication-factor: 3
acks: all
min.insync.replicas: 2
```

동작:
```
정상:
  ISR: [Broker 1, Broker 2, Broker 3]  ← 3개
  → 메시지 전송 성공 (최소 2개 충족)

주의:
  ISR: [Broker 1, Broker 2]            ← 2개
  → 메시지 전송 성공 (최소 2개 충족)

차단:
  ISR: [Broker 1]                      ← 1개만
  → 메시지 전송 실패! (최소 2개 미달)
  → NotEnoughReplicasException 발생
```

**트레이드오프**:
- 높은 값: 신뢰성 ↑, 가용성 ↓ (Broker 장애 시 전송 불가)
- 낮은 값: 신뢰성 ↓, 가용성 ↑ (Broker 장애에도 전송 가능)

---

### 성능 비교

```
1,000개 메시지 전송 시간:

acks=0:   100ms  (최고 성능)
acks=1:   500ms  (중간)
acks=all: 1500ms (최저 성능, 최고 신뢰성)
```

---

## 장애 복구 (Failover)

### 자동 페일오버 프로세스

#### 1단계: 정상 상태
```
Partition 0:
  Broker 1: Leader (ISR)
  Broker 2: Replica (ISR)
  Broker 3: Replica (ISR)

Producer → Broker 1 (Leader)
Consumer → Broker 1 (Leader)
```

---

#### 2단계: Leader 장애 감지
```
Broker 1 장애 발생
  ↓
Zookeeper/KRaft가 장애 감지 (Heartbeat 중단)
  ↓
시간: 약 1-3초
```

---

#### 3단계: 새 Leader 선출
```
선출 프로세스:
1. ISR 목록 확인: [Broker 2, Broker 3]  ← Broker 1 제외
2. ISR 중에서 새 Leader 선택 (라운드로빈 또는 우선순위)
3. Broker 2가 새 Leader로 선출
4. 메타데이터 업데이트

시간: 약 1-2초
```

---

#### 4단계: 클라이언트 재연결
```
Producer/Consumer 자동 재연결:
1. Leader 변경 감지
2. Broker 2로 자동 재연결
3. 서비스 계속 운영

Producer → Broker 2 (새 Leader)
Consumer → Broker 2 (새 Leader)

시간: 즉시 (자동)
```

---

#### 5단계: 장애 복구 후
```
Broker 1 복구:
1. Broker 1이 다시 시작
2. Replica로 클러스터 재참여
3. Leader(Broker 2)로부터 누락된 데이터 복제
4. 복제 완료 후 ISR에 재가입

Partition 0:
  Broker 2: Leader (ISR)    ← 그대로 Leader 유지
  Broker 1: Replica (ISR)   ← Replica로 복귀
  Broker 3: Replica (ISR)
```

---

### 전체 타임라인

```
시간 0초:  정상 운영 (Broker 1 Leader)
시간 1초:  Broker 1 장애 발생
시간 2초:  Zookeeper가 장애 감지
시간 3초:  Broker 2가 새 Leader로 선출
시간 4초:  클라이언트 자동 재연결
시간 5초:  서비스 정상화 (사용자는 인지 못함)
         ↓
         약 4초의 짧은 중단
```

---

### 데이터 일관성 보장

```
acks=all + replication-factor=3 + min.insync.replicas=2:

시나리오:
1. Producer가 메시지 전송
2. Leader + 최소 1개 Replica 복제 완료
3. Producer에게 성공 응답
4. Leader 장애 발생
5. ISR Replica가 Leader로 승격
6. 데이터 손실 없음! (Replica가 최신 데이터 보유)
```

---

## Replication 설정

### 1. application.yml 설정

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092

kafka:
  producer:
    bootstrap-servers: localhost:9092
    acks: all                          # all, 1, 0
    retries: 3                         # 재시도 횟수

  topics:
    # 중요 데이터 (주문, 결제)
    - category: SALES_ORDER
      name: sales-orders
      partitions: 3
      replication-factor: 3            # 높은 복제

    # 일반 데이터 (로그, 이벤트)
    - category: USER_EVENT
      name: user-events
      partitions: 5
      replication-factor: 2            # 중간 복제

    # 비중요 데이터 (메트릭, 디버그 로그)
    - category: DEBUG_LOG
      name: debug-logs
      partitions: 1
      replication-factor: 1            # 복제 없음
```

---

### 2. Broker 설정

```properties
# server.properties

# 최소 ISR 개수
min.insync.replicas=2

# Replica가 Leader를 따라잡지 못하면 ISR에서 제거되는 시간
replica.lag.time.max.ms=10000

# Leader 선출 전략
unclean.leader.election.enable=false  # ISR 외 Replica의 Leader 선출 금지
```

#### unclean.leader.election.enable

```
false (권장):
- ISR 내에서만 Leader 선출
- 데이터 손실 방지
- ISR이 모두 장애 시 서비스 중단

true (위험):
- ISR 외 Replica도 Leader 가능
- 데이터 손실 위험
- 가용성 우선
```

---

### 3. Topic 생성 (명령어)

```bash
# Kafka CLI로 토픽 생성
kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic sales-orders \
  --partitions 3 \
  --replication-factor 3 \
  --config min.insync.replicas=2
```

---

### 4. 코드 설정

#### Producer
```java
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.ACKS_CONFIG, "all");              // 모든 ISR 대기
        config.put(ProducerConfig.RETRIES_CONFIG, 3);               // 3번 재시도
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1); // 순서 보장
        return new DefaultKafkaProducerFactory<>(config);
    }
}
```

#### Consumer
```java
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 수동 커밋
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"); // 커밋된 데이터만
        return new DefaultKafkaConsumerFactory<>(config);
    }
}
```

---

## 실무 권장 사항

### 환경별 설정

#### 개발 환경
```yaml
목적: 빠른 개발/테스트

설정:
  replication-factor: 1
  acks: 1
  min.insync.replicas: 1

특징:
- Broker 1개로도 동작
- 빠른 응답 속도
- 비용 절감
- 데이터 손실 허용
```

---

#### 스테이징 환경
```yaml
목적: 운영 환경 시뮬레이션

설정:
  replication-factor: 2
  acks: 1
  min.insync.replicas: 1

특징:
- 운영과 유사한 환경
- 적당한 비용
- 기본적인 가용성
```

---

#### 운영 환경 (일반 데이터)
```yaml
목적: 로그, 이벤트, 메트릭

설정:
  replication-factor: 2
  acks: 1
  min.insync.replicas: 1

특징:
- 1개 Broker 장애 대응
- 적당한 성능
- 대부분의 사용 사례
```

---

#### 운영 환경 (중요 데이터)
```yaml
목적: 주문, 결제, 금융 거래

설정:
  replication-factor: 3
  acks: all
  min.insync.replicas: 2

특징:
- 2개 Broker 동시 장애 대응
- 데이터 손실 최소화
- 법적 요구사항 충족
```

---

### 데이터 중요도별 설정

#### Tier 1: 극도로 중요 (금융, 결제)
```yaml
replication-factor: 3
acks: all
min.insync.replicas: 2
retries: Integer.MAX_VALUE
enable.idempotence: true  # 중복 방지
```

#### Tier 2: 중요 (주문, 사용자 데이터)
```yaml
replication-factor: 3
acks: all
min.insync.replicas: 2
retries: 3
```

#### Tier 3: 일반 (로그, 이벤트)
```yaml
replication-factor: 2
acks: 1
min.insync.replicas: 1
retries: 1
```

#### Tier 4: 비중요 (디버그 로그, 메트릭)
```yaml
replication-factor: 1
acks: 0
min.insync.replicas: 1
retries: 0
```

---

## 모니터링

### 1. 토픽 상태 확인

```bash
kafka-topics.sh --describe \
  --bootstrap-server localhost:9092 \
  --topic sales-orders
```

출력:
```
Topic: sales-orders     PartitionCount: 3       ReplicationFactor: 3

Partition: 0    Leader: 1       Replicas: 1,2,3 ISR: 1,2,3
Partition: 1    Leader: 2       Replicas: 2,3,1 ISR: 2,3,1
Partition: 2    Leader: 3       Replicas: 3,1,2 ISR: 3,1,2
```

해석:
```
Leader: 1        → Broker 1이 Leader
Replicas: 1,2,3  → 복제본이 Broker 1,2,3에 위치
ISR: 1,2,3       → 모두 동기화 상태 (정상)
```

---

### 2. Under-Replicated Partitions

```bash
# Under-Replicated 파티션 확인
kafka-topics.sh --describe \
  --bootstrap-server localhost:9092 \
  --under-replicated-partitions
```

출력 예시 (문제 상황):
```
Partition: 0    Leader: 1       Replicas: 1,2,3 ISR: 1,2
                                                     ↑
                                        Broker 3이 ISR 아님!
```

**조치**:
1. Broker 3의 상태 확인
2. 네트워크/디스크/CPU 확인
3. 로그 분석

---

### 3. Broker별 Partition 분포

```bash
# 모든 토픽의 파티션 분포 확인
kafka-topics.sh --describe \
  --bootstrap-server localhost:9092
```

이상적인 분포:
```
Broker 1: Leader 10개, Replica 20개
Broker 2: Leader 10개, Replica 20개
Broker 3: Leader 10개, Replica 20개
         ↑ 균등 분산
```

불균형 분포:
```
Broker 1: Leader 20개, Replica 25개  ← 과부하
Broker 2: Leader 5개,  Replica 15개
Broker 3: Leader 5개,  Replica 15개
         ↑ 재분배 필요
```

---

### 4. ISR 변경 모니터링

```bash
# Kafka Manager, Kafdrop 등 GUI 도구 사용
# 또는 JMX 메트릭 수집

# JMX 메트릭:
kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions
kafka.server:type=ReplicaManager,name=IsrShrinksPerSec
kafka.server:type=ReplicaManager,name=IsrExpandsPerSec
```

알림 설정:
```
UnderReplicatedPartitions > 0     → 경고
IsrShrinksPerSec > 임계값          → 경고
Leader 선출 빈도 > 임계값          → 심각
```

---

### 5. 로그 레벨 설정

```yaml
# application.yml
logging:
  level:
    org.apache.kafka: INFO
    org.springframework.kafka: INFO
```

주요 로그:
```
INFO: Leader가 Broker 2로 변경됨
WARN: ISR이 [1,2]로 축소됨 (Broker 3 제외)
ERROR: NotEnoughReplicasException (min.insync.replicas 미달)
```

---

## 비용 및 성능 고려사항

### 저장 공간

```
데이터: 100GB

replication-factor: 1 → 100GB 필요
replication-factor: 2 → 200GB 필요 (2배)
replication-factor: 3 → 300GB 필요 (3배)
```

---

### 네트워크 대역폭

```
초당 1GB 데이터 전송 시:

replication-factor: 1
  → Leader: 1GB 쓰기
  → 네트워크: 1GB

replication-factor: 2
  → Leader: 1GB 쓰기
  → Replica: 1GB 복제
  → 네트워크: 2GB (2배)

replication-factor: 3
  → Leader: 1GB 쓰기
  → Replica 1: 1GB 복제
  → Replica 2: 1GB 복제
  → 네트워크: 3GB (3배)
```

---

### 처리량 (Throughput)

```
acks=0:   10,000 msg/sec  (최고)
acks=1:   5,000 msg/sec   (중간)
acks=all: 2,000 msg/sec   (최저)

replication-factor 증가 → 복제 오버헤드 증가 → 처리량 감소
```

---

### 지연 시간 (Latency)

```
acks=0:   1ms    (즉시)
acks=1:   5ms    (Leader 대기)
acks=all: 20ms   (모든 ISR 대기)

replication-factor 증가 → 복제 대기 시간 증가 → 지연 시간 증가
```

---

## 트러블슈팅

### 문제 1: NotEnoughReplicasException

#### 증상
```
org.apache.kafka.common.errors.NotEnoughReplicasException:
  Messages are rejected since there are fewer in-sync replicas than required.
```

#### 원인
```
설정:
  replication-factor: 3
  min.insync.replicas: 2

현재 상태:
  ISR: [Broker 1]  ← 1개만 (2개 미달)
```

#### 해결
```
1. Broker 2, 3의 상태 확인 및 복구
2. 일시적으로 min.insync.replicas 낮추기 (권장 안 함)
3. Producer 재시도 로직 확인
```

---

### 문제 2: Under-Replicated Partitions

#### 증상
```
Partition: 0    Leader: 1       Replicas: 1,2,3 ISR: 1,2
```

#### 원인
```
1. Broker 3의 네트워크 지연
2. Broker 3의 디스크 성능 저하
3. Broker 3의 과부하
```

#### 해결
```
1. Broker 3의 리소스 확인 (CPU, 메모리, 디스크, 네트워크)
2. Broker 로그 확인
3. 필요 시 Broker 재시작
4. 지속되면 Broker 교체 고려
```

---

### 문제 3: Leader Imbalance

#### 증상
```
Broker 1: Leader 20개
Broker 2: Leader 5개
Broker 3: Leader 5개
```

#### 원인
```
Broker 장애 복구 후 Leader 재분배 안 됨
```

#### 해결
```bash
# Leader 재분배
kafka-leader-election.sh --bootstrap-server localhost:9092 \
  --topic sales-orders \
  --partition 0 \
  --election-type preferred
```

---

## 요약

### 핵심 개념
```
1. Replication = 여러 Broker에 데이터 복제
2. Leader = 읽기/쓰기 처리
3. Replica = Leader 장애 시 승격 대기
4. ISR = 동기화된 Replica 목록
5. ACKs = Producer의 성공 판단 기준
```

---

### 설정 가이드
```
개발:
  replication-factor: 1
  acks: 1

운영 (일반):
  replication-factor: 2
  acks: 1
  min.insync.replicas: 1

운영 (중요):
  replication-factor: 3
  acks: all
  min.insync.replicas: 2
```

---

### 트레이드오프
```
높은 Replication:
  장점: 가용성 ↑, 내구성 ↑, 안정성 ↑
  단점: 비용 ↑, 처리량 ↓, 지연 ↑

낮은 Replication:
  장점: 비용 ↓, 처리량 ↑, 지연 ↓
  단점: 가용성 ↓, 내구성 ↓, 안정성 ↓
```

---

### 실무 체크리스트

```
설계 단계:
□ 데이터 중요도 분석
□ 장애 허용 범위 정의
□ 예산 및 리소스 계획
□ Replication Factor 결정
□ ACKs 설정 결정

운영 단계:
□ ISR 모니터링 설정
□ Under-Replicated Partitions 알림
□ Leader 분산 확인
□ 정기적인 Broker 상태 점검
□ 장애 복구 프로세스 문서화
```
