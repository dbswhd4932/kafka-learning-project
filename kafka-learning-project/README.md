# Kafka Learning Project

Apache Kafka를 활용한 이벤트 기반 아키텍처 학습 프로젝트입니다.

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

### 3. 인프라 종료

```bash
docker-compose down
```

데이터 볼륨까지 삭제하려면:

```bash
docker-compose down -v
```

## 프로젝트 구조

```
kafka-learning-project/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/example/kafka/
│   │   │       ├── KafkaLearningApplication.kt
│   │   │       ├── producer/          # Producer 구현
│   │   │       ├── consumer/          # Consumer 구현
│   │   │       ├── streams/           # Kafka Streams 구현
│   │   │       └── config/            # Kafka 설정
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── kotlin/
├── docker-compose.yml
├── build.gradle.kts
└── README.md
```

## 학습 주제

1. **Producer & Consumer**: Kafka 데이터 발행과 구독
2. **Topic & Partition**: 데이터 분산 저장과 병렬 처리
3. **Avro & Schema Registry**: 안정적인 데이터 스키마 관리
4. **Kafka Connect & Debezium**: DB CDC 구현
5. **Kafka Streams API**: 실시간 스트림 처리
6. **Stateful Stream Processing**: 상태 기반 데이터 집계
7. **Interactive Queries**: 실시간 데이터 조회 API
8. **Consumer Group & Rebalancing**: Consumer 확장과 장애 대응
9. **Event-Driven Architecture**: 이벤트 기반 시스템 설계

## 기술 스택

- **Language**: Kotlin
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Gradle 8.8
- **Java**: 17
- **Kafka**: Confluent Platform 7.5.1
- **Database**: PostgreSQL 15
- **Container**: Docker

## 유용한 명령어

### Gradle
```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 클린 빌드
./gradlew clean build
```

### Docker
```bash
# 로그 확인
docker-compose logs -f kafka

# 특정 서비스만 재시작
docker-compose restart kafka

# 실행 중인 컨테이너 확인
docker-compose ps
```

### Kafka CLI (컨테이너 내부)
```bash
# Kafka 컨테이너 접속
docker exec -it kafka bash

# 토픽 생성
kafka-topics --create --topic test-topic --bootstrap-server localhost:29092 --partitions 3 --replication-factor 1

# 토픽 목록 조회
kafka-topics --list --bootstrap-server localhost:29092

# Producer 실행
kafka-console-producer --topic test-topic --bootstrap-server localhost:29092

# Consumer 실행
kafka-console-consumer --topic test-topic --bootstrap-server localhost:29092 --from-beginning
```

## 참고 자료

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring for Apache Kafka](https://spring.io/projects/spring-kafka)
- [Confluent Platform](https://docs.confluent.io/platform/current/overview.html)
- [Debezium](https://debezium.io/)
