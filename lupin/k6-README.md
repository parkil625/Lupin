# K6 부하 테스트 가이드

## 📋 테스트 파일 목록

### 1. `k6-test.js` - 기본 API 부하 테스트
일반적인 API 엔드포인트에 대한 부하 테스트

### 2. `k6-all-apis.js` - 전체 API 부하 테스트
모든 주요 API 엔드포인트 테스트

### 3. `k6-appointment-concurrency-test.js` - 예약 중복 동시성 테스트
**1000명이 동시에 같은 시간대 예약을 시도하는 중복 방지 테스트**

## 🚀 예약 중복 테스트 실행 방법

### 사전 준비

1. **k6 설치**
```bash
# Windows (Chocolatey)
choco install k6

# macOS (Homebrew)
brew install k6

# Linux
sudo apt-get install k6
```

2. **인증 토큰 준비**

테스트를 위해 유효한 JWT 토큰이 필요합니다. 두 가지 방법이 있습니다:

#### 방법 1: 환경 변수로 단일 토큰 사용 (간단한 테스트)
```bash
# 토큰 발급 받기
curl -X POST https://api.lupin-care.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId":"user01","password":"your_password"}'

# 환경 변수 설정
export AUTH_TOKEN="your_jwt_token_here"
```

#### 방법 2: 여러 사용자 토큰 배열 사용 (더 현실적인 테스트)
```bash
# tokens.json 파일 생성
echo '["token1", "token2", "token3", ...]' > tokens.json

# 환경 변수로 설정
export AUTH_TOKENS=$(cat tokens.json)
```

### 기본 실행

```bash
# 기본 설정으로 실행 (1000 VUs)
k6 run k6-appointment-concurrency-test.js

# 환경 변수와 함께 실행
k6 run --env BASE_URL=https://api.lupin-care.com \
       --env AUTH_TOKENS='["token1","token2",...]' \
       k6-appointment-concurrency-test.js
```

### 테스트 설정 커스터마이징

스크립트 내부의 설정을 수정하여 테스트 조건을 변경할 수 있습니다:

```javascript
// k6-appointment-concurrency-test.js 내부

// 1. 동시 접속자 수 변경
export const options = {
  scenarios: {
    concurrent_booking: {
      vus: 500,              // 500명으로 변경
      iterations: 500,
    },
  },
};

// 2. 목표 예약 시간 변경
const TARGET_APPOINTMENT = {
  patientId: 1,
  doctorId: 21,
  date: '2026-01-20T14:00:00',  // 원하는 시간으로 변경
};
```

### 실행 옵션

```bash
# VU 수를 500으로 줄여서 실행
k6 run --vus 500 --iterations 500 k6-appointment-concurrency-test.js

# 결과를 파일로 저장
k6 run k6-appointment-concurrency-test.js --out json=results.json

# 결과를 InfluxDB로 전송 (모니터링 시스템 연동)
k6 run --out influxdb=http://localhost:8086/k6 k6-appointment-concurrency-test.js

# 상세 로그 출력
k6 run --verbose k6-appointment-concurrency-test.js
```

## 📊 테스트 결과 해석

### 성공 기준

테스트가 성공하려면:

1. **정확히 1개의 예약만 성공** (successful_bookings = 1)
2. **나머지 999개는 중복 에러로 거부** (duplicate_errors > 0)
3. **95%의 응답이 2초 이내** (p95 < 2000ms)

### 출력 예시

```
🏥 예약 중복 동시성 테스트 결과
================================================================================

📊 예약 결과:
  ✅ 성공한 예약: 1개
  ❌ 실패한 예약: 999개
  🔁 중복 에러: 999개
  📈 총 요청 수: 1000개

⏱️  성능 지표:
  평균 응답 시간: 523.45ms
  최소 응답 시간: 102.34ms
  최대 응답 시간: 1856.78ms
  P90 응답 시간: 891.23ms
  P95 응답 시간: 1245.67ms
  P99 응답 시간: 1689.45ms

🎯 테스트 결과 판정:
  ✅ PASS: 정확히 1개의 예약만 성공했습니다. (중복 방지 성공)
  ✅ PASS: 중복 시도가 적절히 거부되었습니다.
  ✅ PASS: 응답 시간이 기준 내에 있습니다.
```

### 실패 사례

만약 중복 예약이 발생한다면:

```
📊 예약 결과:
  ✅ 성공한 예약: 3개  ⚠️ 문제 발생!
  ❌ 실패한 예약: 997개

🎯 테스트 결과 판정:
  ❌ FAIL: 3개의 중복 예약이 발생했습니다! (중복 방지 실패)
```

이는 데이터베이스 트랜잭션 격리 수준이나 락 메커니즘에 문제가 있음을 의미합니다.

## 🔧 트러블슈팅

### 1. 모든 요청이 401 Unauthorized 에러

**원인**: 인증 토큰이 유효하지 않거나 만료됨

**해결책**:
```bash
# 새로운 토큰 발급
curl -X POST https://api.lupin-care.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId":"user01","password":"your_password"}'

# 토큰 업데이트
export AUTH_TOKEN="new_token_here"
```

### 2. 모든 요청이 400 Bad Request 에러

**원인**: 요청 데이터 형식이 잘못됨 또는 존재하지 않는 의사/환자 ID

**해결책**:
- `TARGET_APPOINTMENT` 객체의 `patientId`, `doctorId` 확인
- 날짜 형식이 `YYYY-MM-DDTHH:mm:ss` 형식인지 확인

### 3. Connection Timeout

**원인**: 서버가 부하를 감당하지 못함

**해결책**:
```bash
# VU 수를 줄여서 재시도
k6 run --vus 100 --iterations 100 k6-appointment-concurrency-test.js

# 최대 지속 시간 늘리기
# 스크립트 내에서 maxDuration: '2m'으로 변경
```

### 4. 예약이 하나도 성공하지 않음

**원인**:
- 해당 시간대가 이미 예약되어 있음
- 의사가 존재하지 않음
- 과거 날짜로 예약 시도

**해결책**:
- 미래의 사용 가능한 시간대로 변경
- 유효한 의사 ID 사용
- 데이터베이스에서 해당 시간대 예약 삭제 후 재시도

## 📈 성능 최적화 검증

### 데이터베이스 인덱스 확인

중복 방지 성능을 위해 다음 인덱스가 필요합니다:

```sql
-- 복합 인덱스로 중복 예약 빠르게 확인
CREATE INDEX idx_appointment_doctor_date
ON appointment(doctor_id, date);

-- UNIQUE 제약조건으로 중복 방지
ALTER TABLE appointment
ADD CONSTRAINT uk_doctor_date UNIQUE (doctor_id, date);
```

### 트랜잭션 격리 수준 확인

```sql
-- MySQL/MariaDB
SELECT @@transaction_isolation;

-- 권장: READ_COMMITTED 또는 REPEATABLE_READ
```

## 🎯 테스트 시나리오 확장

### 다양한 시간대 동시 예약 테스트

```javascript
// 여러 시간대를 동시에 테스트
const TIME_SLOTS = [
  '2026-01-15T09:00:00',
  '2026-01-15T10:00:00',
  '2026-01-15T11:00:00',
  '2026-01-15T14:00:00',
  '2026-01-15T15:00:00',
];

export default function () {
  const randomSlot = TIME_SLOTS[Math.floor(Math.random() * TIME_SLOTS.length)];
  const payload = JSON.stringify({
    patientId: 1,
    doctorId: 21,
    date: randomSlot,
  });
  // ... rest of the code
}
```

### 다른 환자들이 동시 예약 시도

```javascript
const PATIENT_IDS = [1, 2, 3, 4, 5, ...];

export default function () {
  const randomPatient = PATIENT_IDS[__VU % PATIENT_IDS.length];
  const payload = JSON.stringify({
    patientId: randomPatient,
    doctorId: 21,
    date: '2026-01-15T15:00:00',
  });
  // ... rest of the code
}
```

## 📚 참고 자료

- [k6 공식 문서](https://k6.io/docs/)
- [k6 시나리오 가이드](https://k6.io/docs/using-k6/scenarios/)
- [k6 메트릭](https://k6.io/docs/using-k6/metrics/)
- [데이터베이스 락 메커니즘](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html)
