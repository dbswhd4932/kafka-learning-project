# Kafka Learning Project

## 프로젝트 개요
Apache Kafka를 활용한 이벤트 기반 아키텍처 학습 프로젝트

## 작업 중 기억해야할 사항
- 트랜잭션의 범위는 최소한으로 잡되, 불필요하게 서비스를 나누지말고 필요한 경우에는 예외로 한다.
- 주석은 명확하고 짧게 작성한다.
- 불필요한 메서드는 만들지 않는다.
- Map은 지양하고, Dto를 지향한다.
- 상태값은 Enum을 지향한다.
- 설정값은 yml을 지향한다.

## 학습 목표

### 1. Apache Kafka Producer/Consumer
- 대용량 이벤트 발행 및 소비하는 비동기 메시징 시스템

### 2. Kafka Streams
- 실시간 데이터 필터링, 윈도우 집계, 상태 저장소 관리하는 스트림 처리 시스템

### 3. Debezium CDC
- PostgreSQL 데이터베이스 변경사항을 실시간으로 캡처하는 변경 데이터 캡처 시스템

### 4. Apache Avro Schema
- 타입 안전한 메시지 직렬화 및 스키마 진화를 지원하는 데이터 교환 시스템

### 5. Docker Compose 인프라
- Kafka 클러스터, Zookeeper, PostgreSQL, Debezium Connect를 통합 관리하는 컨테이너 인프라

### 6. Event-Driven Architecture
- 실시간 사기 탐지, 고액 주문 필터링, 매출 통계 집계하는 이벤트 기반 비즈니스 로직

## 프로젝트 특징

### 기술 스택
- **Language**: Java 17
- **Framework**: Spring Boot + Spring Data JPA
- **Infrastructure**: Docker Compose (원클릭 실습 환경 구축)

### 핵심 기능
1. **실무적인 데이터 파이프라인 설계**
   - Avro와 Schema Registry를 활용한 데이터 스키마 관리
   - 안정적인 데이터 전송 및 스키마 진화 지원

2. **실시간 데이터 처리 및 분석**
   - Kafka Streams API를 활용한 실시간 데이터 변환 및 집계
   - 실시간 매출 통계, 사용자 활동 분석

3. **CDC 패턴 구현**
   - Debezium을 이용한 데이터베이스 변경 실시간 감지
   - 데이터베이스와 Kafka 연동

## 학습 커리큘럼

### 1. Producer & Consumer
- Kafka 데이터 발행과 구독

### 2. Topic & Partition
- 데이터 분산 저장과 병렬 처리

### 3. Avro & Schema Registry
- 안정적인 데이터 스키마 관리와 직렬화

### 4. Kafka Connect & Debezium
- DB 변경 데이터 실시간 캡처 (CDC)

### 5. Kafka Streams API
- 실시간 데이터 스트림 처리 및 분석

### 6. Stateful Stream Processing
- 상태를 이용한 데이터 집계

### 7. Interactive Queries
- 처리 중인 실시간 데이터 조회 API

### 8. Consumer Group & Rebalancing
- Consumer 확장과 장애 대응

### 9. Event-Driven Architecture
- 이벤트 기반 시스템 설계

## 기술 스펙

### Development Environment
- **Java**: 17.0.12 2024-07-16 LTS
- **Build Tool**: Gradle
- **IDE**: IntelliJ IDEA
- **Container**: Docker version 28.0.0, build f9ced58158

### Kafka Ecosystem
- Apache Kafka
- Zookeeper
- Schema Registry
- Kafka Connect
- Debezium

### Database
- PostgreSQL

## 현재 학습 진행 상황

**현재 커리큘럼**: Producer & Consumer (Kafka 데이터 발행과 구독)
**상태**: 프로젝트 초기 설정 완료