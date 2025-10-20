# High Traffic Handling Example

대용량 트래픽 처리를 위한 학습 프로젝트입니다.

## 학습 목표

- 대규모 시스템 디자인
- Microservice Architecture
- Event-Driven Architecture
- 분산 시스템에 대한 이해
- 동시성 문제를 다루는 방법
- MySQL, Redis, Kafka에 대한 이해 및 실전 활용 전략
- 데이터베이스 인덱스를 활용한 대규모 데이터 쿼리 최적화
- 복잡한 계층형 테이블 설계 및 최적화
- 높은 쓰기 트래픽에서도 데이터 일관성을 보장하는 방법
- 이벤트 스트림 처리 및 비동기 애플리케이션 구축
- 이벤트 유실 방지를 위한 시스템 구축
- 대규모 트래픽 및 복잡한 아키텍처에서 활용할 수 있는 방법론
- 다양한 요구사항에 적용할 수 있는 캐시 최적화 전략

## 기술 스택

- **Language**: Java 21
- **Framework**: Spring Boot 3.3.2
- **Build Tool**: Gradle
- **Database**: MySQL 8.0.38
- **Cache**: Redis 7.4
- **Message Queue**: Kafka 3.8.0
- **ORM**: Spring Data JPA

## 프로젝트 구조

```
high-traffic-example/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/hightraffic/
│   │   │       ├── config/           # 설정 클래스
│   │   │       │   ├── AsyncConfig.java
│   │   │       │   ├── KafkaConfig.java
│   │   │       │   └── RedisConfig.java
│   │   │       ├── controller/       # REST API 컨트롤러
│   │   │       ├── service/          # 비즈니스 로직
│   │   │       ├── repository/       # 데이터 접근 계층
│   │   │       ├── domain/           # 엔티티
│   │   │       ├── dto/              # DTO
│   │   │       ├── event/            # 이벤트 객체
│   │   │       ├── consumer/         # Kafka Consumer
│   │   │       ├── producer/         # Kafka Producer
│   │   │       ├── exception/        # 예외 처리
│   │   │       ├── util/             # 유틸리티
│   │   │       └── HighTrafficApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
├── build.gradle
├── settings.gradle
└── docker-compose.yml
```

## 시작하기

### 사전 요구사항

- Java 21
- Docker & Docker Compose
- IntelliJ IDEA (권장)

### 인프라 실행

Docker Compose를 사용하여 MySQL, Redis, Kafka를 실행합니다:

```bash
docker-compose up -d
```

서비스 확인:
- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Kafka UI: `http://localhost:8989`

### 애플리케이션 실행

```bash
./gradlew bootRun
```

또는 IntelliJ IDEA에서 `HighTrafficApplication` 클래스를 실행합니다.

### 애플리케이션 중지

```bash
./gradlew bootStop
```

### 인프라 중지

```bash
docker-compose down
```

데이터 볼륨까지 삭제하려면:

```bash
docker-compose down -v
```

## 주요 설정

### MySQL 설정

- 최대 연결 수: 1000
- InnoDB 버퍼 풀 크기: 1GB
- 문자 인코딩: UTF8MB4

### Redis 설정

- 최대 메모리: 512MB
- 메모리 정책: allkeys-lru
- AOF 영속성 활성화

### Kafka 설정

- 파티션 수: 3
- 복제 계수: 1
- 기본 토픽: order-created, order-updated, payment-processed, inventory-updated

### HikariCP 설정

- 최대 풀 크기: 20
- 최소 유휴 연결: 10
- 연결 타임아웃: 30초

## 모니터링

### Actuator 엔드포인트

애플리케이션 실행 후 다음 엔드포인트에서 상태를 확인할 수 있습니다:

- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Info: `http://localhost:8080/actuator/info`

### Kafka UI

Kafka 토픽 및 메시지를 확인할 수 있습니다:

- URL: `http://localhost:8989`

## 다음 단계

프로젝트 세팅이 완료되었습니다. 이제 다음 섹션의 학습 목표를 구현할 준비가 되었습니다.

## 라이선스

이 프로젝트는 학습 목적으로 작성되었습니다.
