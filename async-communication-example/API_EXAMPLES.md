# API 사용 예제

## 1. 동기 vs 비동기 성능 비교

### 동기 방식 사용자 생성 (약 3초 소요)
```bash
curl -X POST http://localhost:8080/api/users/sync \
  -H "Content-Type: application/json" \
  -d '{
    "name": "홍길동",
    "email": "hong@example.com"
  }'
```

**결과**: 사용자 생성 + 이메일 전송 완료 후 응답 (약 3초)

### 비동기 방식 사용자 생성 (즉시 반환)
```bash
curl -X POST http://localhost:8080/api/users/async \
  -H "Content-Type: application/json" \
  -d '{
    "name": "김철수",
    "email": "kim@example.com"
  }'
```

**결과**: 사용자 생성 후 즉시 응답, 이메일은 백그라운드 처리

---

## 2. 주문 처리 예제

### 단일 주문 생성
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "노트북",
    "amount": 1500000,
    "customerEmail": "customer@example.com"
  }'
```

**응답 예시**:
```json
{
  "id": 1,
  "productName": "노트북",
  "amount": 1500000,
  "customerEmail": "customer@example.com",
  "status": "PENDING",
  "createdAt": "2025-10-15T10:30:00",
  "processedAt": null
}
```

### 주문 상태 확인
```bash
curl http://localhost:8080/api/orders/1
```

**주문 상태 변화**:
1. `PENDING` - 주문 생성 직후
2. `PROCESSING` - 처리 중 (비동기 작업 진행 중)
3. `COMPLETED` - 처리 완료 (약 5초 후)
4. `FAILED` - 처리 실패

### 배치 주문 생성 (병렬 처리)
```bash
curl -X POST http://localhost:8080/api/orders/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "productName": "노트북",
      "amount": 1500000,
      "customerEmail": "customer1@example.com"
    },
    {
      "productName": "마우스",
      "amount": 50000,
      "customerEmail": "customer2@example.com"
    },
    {
      "productName": "키보드",
      "amount": 120000,
      "customerEmail": "customer3@example.com"
    }
  ]'
```

**결과**: 3개 주문이 병렬로 처리되어 약 3초에 모두 완료

---

## 3. 데모 API - 성능 비교

### 동기 방식 (순차 실행 - 약 9초)
```bash
curl "http://localhost:8080/api/demo/sync?email=test@example.com"
```

**응답**:
```json
{
  "method": "synchronous",
  "email": "test@example.com",
  "tasks": 3,
  "duration_ms": 9012
}
```

### 비동기 방식 (병렬 실행 - 약 3초)
```bash
curl "http://localhost:8080/api/demo/async?email=test@example.com"
```

**응답**:
```json
{
  "method": "asynchronous",
  "email": "test@example.com",
  "tasks": 3,
  "duration_ms": 3005,
  "results": {
    "welcome": true,
    "verification": true,
    "promotion": true
  }
}
```

**성능 개선**: 약 66% 시간 단축 (9초 → 3초)

### 대량 이메일 전송
```bash
curl "http://localhost:8080/api/demo/bulk?count=10"
```

**응답**:
```json
{
  "method": "bulk_async",
  "total_recipients": 10,
  "success_count": 10,
  "duration_ms": 10025
}
```

### 비동기 체이닝
```bash
curl "http://localhost:8080/api/demo/chain?email=test@example.com"
```

**응답**:
```json
{
  "method": "async_chaining",
  "email": "test@example.com",
  "steps": 3,
  "duration_ms": 9018,
  "final_result": true
}
```

---

## 4. 조회 API

### 모든 사용자 조회 (동기)
```bash
curl http://localhost:8080/api/users
```

### 모든 사용자 조회 (비동기)
```bash
curl http://localhost:8080/api/users/async
```

### 특정 사용자 조회 (비동기)
```bash
curl http://localhost:8080/api/users/1/async
```

### 상태별 주문 조회
```bash
# PENDING 주문 조회
curl http://localhost:8080/api/orders/status/PENDING

# COMPLETED 주문 조회
curl http://localhost:8080/api/orders/status/COMPLETED

# PROCESSING 주문 조회
curl http://localhost:8080/api/orders/status/PROCESSING

# FAILED 주문 조회
curl http://localhost:8080/api/orders/status/FAILED
```

---

## 5. 테스트 시나리오

### 시나리오 1: 동기 vs 비동기 성능 비교
```bash
# 1. 동기 방식 - 시간 측정
time curl -X POST http://localhost:8080/api/users/sync \
  -H "Content-Type: application/json" \
  -d '{"name": "동기테스트", "email": "sync@test.com"}'

# 2. 비동기 방식 - 시간 측정
time curl -X POST http://localhost:8080/api/users/async \
  -H "Content-Type: application/json" \
  -d '{"name": "비동기테스트", "email": "async@test.com"}'
```

### 시나리오 2: 주문 생성 및 상태 변화 관찰
```bash
# 1. 주문 생성
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "테스트상품",
    "amount": 100000,
    "customerEmail": "test@example.com"
  }'

# 2. 즉시 상태 확인 (PENDING)
curl http://localhost:8080/api/orders/1

# 3. 3초 후 상태 확인 (PROCESSING)
sleep 3 && curl http://localhost:8080/api/orders/1

# 4. 6초 후 상태 확인 (COMPLETED)
sleep 3 && curl http://localhost:8080/api/orders/1
```

### 시나리오 3: 병렬 처리 성능 테스트
```bash
# 동시에 여러 주문 생성 (배치 처리)
curl -X POST http://localhost:8080/api/orders/batch \
  -H "Content-Type: application/json" \
  -d '[
    {"productName": "상품1", "amount": 10000, "customerEmail": "user1@test.com"},
    {"productName": "상품2", "amount": 20000, "customerEmail": "user2@test.com"},
    {"productName": "상품3", "amount": 30000, "customerEmail": "user3@test.com"},
    {"productName": "상품4", "amount": 40000, "customerEmail": "user4@test.com"},
    {"productName": "상품5", "amount": 50000, "customerEmail": "user5@test.com"}
  ]'
```

---

## 6. 콘솔 로그 확인 포인트

애플리케이션 실행 후 다음 로그를 확인할 수 있습니다:

### 스레드 이름 확인
```
[http-nio-8080-exec-1] - 메인 요청 처리 스레드
[async-1], [async-2] - taskExecutor 스레드
[email-1], [email-2] - emailExecutor 스레드
```

### 비동기 작업 흐름
```
[http-nio-8080-exec-1] Request received: Create order for customer@example.com
[http-nio-8080-exec-1] Order created: 1
[http-nio-8080-exec-1] Response returned in 50ms (order processing in background)
[async-1] Processing order asynchronously: 1
[async-1] Order processed successfully: 1
[email-1] Sending email to: customer@example.com
[email-1] Email sent successfully to: customer@example.com
```

---

## 7. H2 콘솔로 데이터 확인

1. 브라우저에서 접속: http://localhost:8080/h2-console
2. 접속 정보:
   - JDBC URL: `jdbc:h2:mem:asyncdb`
   - Username: `sa`
   - Password: (비워두기)
3. SQL 쿼리 예제:
```sql
-- 모든 사용자 조회
SELECT * FROM USERS;

-- 모든 주문 조회
SELECT * FROM ORDERS;

-- 완료된 주문만 조회
SELECT * FROM ORDERS WHERE STATUS = 'COMPLETED';

-- 처리 중인 주문 조회
SELECT * FROM ORDERS WHERE STATUS = 'PROCESSING';
```

---

## 8. Postman 컬렉션

위 예제들을 Postman에서 사용하려면:

1. Postman 실행
2. Import > Raw Text 선택
3. 위의 curl 명령어를 Postman이 자동으로 변환
4. 또는 직접 요청 생성:
   - Method: POST/GET
   - URL: http://localhost:8080/api/...
   - Headers: Content-Type: application/json
   - Body: raw JSON

---

## 성능 비교 요약

| 작업 | 동기 방식 | 비동기 방식 | 개선율 |
|------|----------|------------|--------|
| 사용자 생성 + 이메일 | ~3초 | 즉시 | ~100% |
| 3개 작업 실행 | ~9초 | ~3초 | ~66% |
| 5개 주문 처리 | ~15초 | ~3초 | ~80% |
