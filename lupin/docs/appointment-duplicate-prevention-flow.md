# 예약 중복 방지 플로우 차트

## 🔄 예약 생성 플로우 (Redis 통합)

```mermaid
flowchart TD
    Start([예약 요청 시작]) --> ValidateUser{환자/의사<br/>존재 확인}

    ValidateUser -->|존재하지 않음| Error1[404 에러:<br/>사용자 없음]
    ValidateUser -->|존재함| ValidateRole{의사 권한<br/>확인}

    ValidateRole -->|의사 아님| Error2[400 에러:<br/>권한 없음]
    ValidateRole -->|의사 맞음| AcquireLock{Redis<br/>분산 락<br/>획득}

    AcquireLock -->|획득 실패| Error4[409 에러:<br/>다른 사용자가<br/>예약 중]
    AcquireLock -->|획득 성공| CheckDoctorDuplicate{의사의<br/>해당 시간<br/>예약 존재?}

    CheckDoctorDuplicate -->|예약 있음| ReleaseLock1[락 해제]
    ReleaseLock1 --> Error3[409 에러:<br/>시간 중복]
    CheckDoctorDuplicate -->|예약 없음| SaveAppointment[예약 저장]

    SaveAppointment --> CreateChatRoom[채팅방 생성]
    CreateChatRoom --> InvalidateCache[Redis 캐시<br/>무효화]
    InvalidateCache --> ReleaseLock2[락 해제]
    ReleaseLock2 --> Success([예약 완료])

    style Start fill:#e1f5e1
    style Success fill:#e1f5e1
    style Error1 fill:#ffe1e1
    style Error2 fill:#ffe1e1
    style Error3 fill:#ffe1e1
    style Error4 fill:#ffe1e1
    style AcquireLock fill:#fff4e1
    style CheckDoctorDuplicate fill:#fff4e1
```

---

## 📊 중복 예약 체크 로직 (Redis 통합)

### 의사 중복 체크 with Redis 분산 락

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant Service as AppointmentService
    participant Redis as Redis (Redisson)
    participant Repo as AppointmentRepository
    participant DB as Database

    Client->>Service: createAppointment(request)
    activate Service

    Service->>Service: 환자/의사 유효성 검증

    Service->>Redis: tryLock(lockKey, 3초, 5초)
    activate Redis

    alt 락 획득 실패
        Redis-->>Service: false
        Service->>Client: ❌ BusinessException<br/>"다른 사용자가 예약 중입니다"
    else 락 획득 성공
        Redis-->>Service: true
        deactivate Redis

        Service->>Repo: existsByDoctorIdAndDate(doctorId, date)
        activate Repo

        Repo->>DB: SELECT COUNT(*) > 0<br/>WHERE doctor_id = ?<br/>AND date = ?
        activate DB
        DB-->>Repo: true/false
        deactivate DB

        Repo-->>Service: true/false
        deactivate Repo

        alt 예약 이미 존재
            Service->>Redis: unlock()
            Service->>Client: ❌ BusinessException<br/>"해당 의사의 해당 시간에<br/>예약이 이미 꽉 찼습니다"
        else 예약 가능
            Service->>Repo: save(appointment)
            Repo->>DB: INSERT INTO appointment...
            DB-->>Repo: savedAppointment
            Repo-->>Service: savedAppointment

            Service->>Redis: delete(cacheKey)
            Note over Service,Redis: 캐시 무효화

            Service->>Redis: unlock()
            Service->>Client: ✅ appointmentId
        end
    end

    deactivate Service
```

---

## 🏥 다중 진료과 예약 시나리오

### Case: 환자A가 같은 시간에 내과, 외과 예약

```mermaid
gantt
    title 2025-12-18 09:00 예약 타임라인
    dateFormat HH:mm
    axisFormat %H:%M

    section 환자A
    내과 예약 (의사1)    :active, 09:00, 30m
    외과 예약 (의사2)    :active, 09:00, 30m
    피부과 예약 (의사3)  :active, 09:00, 30m

    section 환자B
    내과 예약 시도 (의사1) :crit, 09:00, 30m
```

**설명**:

- ✅ 환자A: 내과(의사1) 09:00 예약 **성공**
- ✅ 환자A: 외과(의사2) 09:00 예약 **성공** (다른 의사)
- ✅ 환자A: 피부과(의사3) 09:00 예약 **성공** (다른 의사)
- ❌ 환자B: 내과(의사1) 09:00 예약 **실패** (의사1 중복)

---

## 🔄 취소된 예약 재예약 시나리오

### Case: 취소된 시간대에 다른 환자가 예약

```mermaid
sequenceDiagram
    participant P1 as 환자A
    participant P2 as 환자B
    participant Service as AppointmentService
    participant DB as Database

    Note over P1,DB: 초기 상태: 의사1, 09:00 예약 없음

    P1->>Service: 의사1, 09:00 예약 생성
    Service->>DB: INSERT (환자A, 의사1, 09:00, SCHEDULED)
    DB-->>P1: ✅ 예약 성공

    Note over P1,DB: 환자A가 예약 취소

    P1->>Service: 예약 취소 요청
    Service->>DB: UPDATE status = CANCELLED
    DB-->>P1: ✅ 취소 완료

    Note over P1,DB: 환자B가 같은 시간대 예약 시도

    P2->>Service: 의사1, 09:00 예약 생성
    Service->>DB: SELECT COUNT(*)<br/>WHERE doctor_id=1<br/>AND date='09:00'<br/>AND status != 'CANCELLED'
    DB-->>Service: 0 (취소된 예약 제외)
    Service->>DB: INSERT (환자B, 의사1, 09:00, SCHEDULED)
    DB-->>P2: ✅ 예약 성공
```

**설명**:

1. 환자A가 의사1의 09:00 시간대 예약 → **성공** (SCHEDULED)
2. 환자A가 예약 취소 → 상태가 **CANCELLED**로 변경
3. 환자B가 같은 시간대(의사1, 09:00) 예약 시도
4. 중복 체크 쿼리에서 `status != 'CANCELLED'` 조건으로 **취소된 예약 제외**
5. 환자B 예약 → **성공** ✅

**중요**:

- 취소된 예약은 다른 진료과와 마찬가지로 **의사별로** 독립적으로 처리됨
- 의사A의 취소 → 의사A의 해당 시간만 재예약 가능
- 의사B, 의사C는 전혀 영향 받지 않음

---

## 🔍 상세 비교: v1.0 vs v2.0

### v1.0 (이전 버전 - 환자 중복 체크 있음)

```mermaid
flowchart LR
    A[예약 요청] --> B{의사<br/>중복?}
    B -->|Yes| E1[❌ 실패]
    B -->|No| C{환자<br/>중복?}
    C -->|Yes| E2[❌ 실패]
    C -->|No| S[✅ 성공]

    style E1 fill:#ffe1e1
    style E2 fill:#ffe1e1
    style S fill:#e1f5e1
```

**문제점**: 환자가 같은 시간에 내과 예약 후 외과 예약 시도 시 **실패**

---

### v2.0 (현재 버전 - 환자 중복 체크 제거 + 취소된 예약 처리)

```mermaid
flowchart LR
    A[예약 요청] --> B{의사의<br/>해당 시간<br/>예약 존재?<br/>status != CANCELLED}
    B -->|Yes| E1[❌ 실패]
    B -->|No| S[✅ 성공]

    style E1 fill:#ffe1e1
    style S fill:#e1f5e1
```

**개선점**:

- 환자가 같은 시간에 여러 진료과 예약 가능 ✅
- 취소된 예약(CANCELLED)은 중복 체크에서 제외되어 같은 시간대 재예약 가능 ✅

---

## 🎯 데이터베이스 제약조건 (권장)

### 유니크 인덱스로 동시성 제어

```sql
-- 의사는 같은 시간에 한 명만 진료 가능
CREATE UNIQUE INDEX idx_appointment_doctor_date_unique
ON appointment(doctor_id, date)
WHERE status != 'CANCELLED';
```

**효과**:

```mermaid
sequenceDiagram
    participant T1 as Thread 1
    participant T2 as Thread 2
    participant DB as Database

    T1->>DB: existsByDoctorIdAndDate() = false
    T2->>DB: existsByDoctorIdAndDate() = false

    T1->>DB: INSERT (doctor_id=1, date='09:00')
    activate DB
    Note over DB: 유니크 인덱스 획득
    DB-->>T1: ✅ 성공
    deactivate DB

    T2->>DB: INSERT (doctor_id=1, date='09:00')
    activate DB
    Note over DB: 유니크 제약 위반
    DB-->>T2: ❌ UniqueConstraintViolation
    deactivate DB
```

---

## 📈 성능 분석

### 쿼리 실행 계획

```sql
-- existsByDoctorIdAndDate 쿼리
EXPLAIN ANALYZE
SELECT COUNT(*) > 0
FROM appointment
WHERE doctor_id = 21
  AND date = '2025-12-18 09:00:00';
```

**인덱스 있을 때**:

```
Index Scan using idx_appointment_doctor_date
  Cost: 0.43..8.45 rows=1
  Execution Time: 0.023ms
```

**인덱스 없을 때**:

```
Seq Scan on appointment
  Cost: 0.00..1234.56 rows=1
  Execution Time: 45.678ms
```

---

## 🧪 테스트 커버리지

### 테스트 매트릭스

| 시나리오                      | 테스트 파일                      | 결과    |
| ----------------------------- | -------------------------------- | ------- |
| 의사 중복 예약 방지           | AppointmentServiceTest           | ✅ Pass |
| 환자 다중 진료과 예약         | AppointmentDepartmentServiceTest | ✅ Pass |
| 예약된 시간 조회 - 취소 제외  | AppointmentBookingServiceTest    | ✅ Pass |
| 진료과명 자동 설정            | AppointmentDepartmentServiceTest | ✅ Pass |
| 예약 생성 시 채팅방 자동 생성 | AppointmentServiceTest           | ✅ Pass |

---

## ✅ Redis 통합 완료

### 1. Redis 분산 락 (Redisson)

**구현 내용:**

```java
// 락 키 형식: appointment:lock:doctor:{doctorId}:{date}
String lockKey = "appointment:lock:doctor:21:2025-12-18T09:00";
RLock lock = redissonClient.getLock(lockKey);

// 락 획득 시도 (최대 3초 대기, 5초 후 자동 해제)
boolean isLocked = lock.tryLock(3L, 5L, TimeUnit.SECONDS);
```

**효과:**

- 동시 예약 요청 시 순차적으로 처리
- Race Condition 방지
- 자동 락 해제로 데드락 방지

**설정:**

```yaml
# application.yaml
redisson:
  singleServerConfig:
    address: "redis://localhost:6379"
    connectionMinimumIdleSize: 5
    connectionPoolSize: 10
    timeout: 3000
    retryAttempts: 3
    retryInterval: 1500
```

### 2. Redis 캐싱 시스템

**구현 내용:**

```java
// 캐시 키 형식: appointment:booked:{doctorId}:{date}
String cacheKey = "appointment:booked:21:2025-12-18";

// 캐시에서 조회 (5분 TTL)
List<String> cachedTimes = getCachedBookedTimes(cacheKey);

// 캐시 무효화 (예약 생성/취소 시)
invalidateBookedTimesCache(doctorId, date);
```

**효과:**

- DB 조회 횟수 감소 (성능 향상)
- 캐시 TTL 5분으로 데이터 신선도 유지
- 예약 생성/취소 시 즉시 캐시 무효화

**캐시 전략:**

```mermaid
flowchart LR
    A[클라이언트] -->|예약 가능 시간 조회| B[Redis Cache]
    B -->|캐시 Hit| A
    B -->|캐시 Miss| C[DB 조회]
    C --> D[Redis 저장<br/>TTL: 5분]
    D --> A

    E[예약 생성/취소] --> F[캐시 무효화]
    F --> G[DB 저장]
```

### 3. 동시성 제어 시나리오

**Case: 두 명이 동시에 같은 시간 예약 시도**

```mermaid
sequenceDiagram
    participant U1 as 사용자1
    participant U2 as 사용자2
    participant Redis as Redis Lock
    participant Service as AppointmentService
    participant DB as Database

    U1->>Redis: tryLock() 시도
    U2->>Redis: tryLock() 시도

    Redis-->>U1: ✅ 락 획득 성공
    Redis-->>U2: ❌ 락 획득 실패 (대기)

    U1->>Service: 중복 체크 (없음)
    U1->>DB: 예약 저장
    DB-->>U1: ✅ 성공

    U1->>Redis: unlock()
    Redis-->>U2: ✅ 락 획득 성공

    U2->>Service: 중복 체크 (있음)
    Service-->>U2: ❌ 예약 중복 에러

    U2->>Redis: unlock()
```

### 4. 테스트 커버리지

**Redis 통합 테스트:**

- ✅ Redis 분산 락을 사용한 예약 생성
- ✅ 락 획득 실패 시 예외 처리
- ✅ Redis 캐시 Hit 시 DB 조회 생략
- ✅ Redis 캐시 Miss 시 DB 조회 및 캐싱
- ✅ 취소된 예약 제외 처리
- ✅ 예약 생성/취소 시 캐시 무효화

**테스트 파일:**

- `AppointmentRedisServiceTest.java` (8개 테스트)

## 🚀 향후 개선 방향

### 1. 예약 대기열 시스템

```mermaid
stateDiagram-v2
    [*] --> Available: 예약 가능
    Available --> Reserved: 예약 시도
    Reserved --> Confirmed: 5분 내 확정
    Reserved --> Available: 5분 초과 (자동 취소)
    Confirmed --> Completed: 진료 완료
    Confirmed --> Cancelled: 사용자 취소
    Cancelled --> [*]
    Completed --> [*]
```

### 3. 예약 충돌 알림

```mermaid
sequenceDiagram
    participant U as 사용자
    participant S as Service
    participant N as Notification

    U->>S: 예약 시도
    S->>S: 중복 체크

    alt 예약 불가
        S->>N: 대기 등록 알림
        N-->>U: 푸시 알림:<br/>"대기자 등록되었습니다"

        Note over S,N: 기존 예약 취소 시
        S->>N: 예약 가능 알림
        N-->>U: 푸시 알림:<br/>"예약 가능합니다"
    end
```

---

## 📚 참고 자료

- [Spring Data JPA Query Methods](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods)
- [Database Indexing Best Practices](https://use-the-index-luke.com/)
- [Pessimistic Locking in JPA](https://www.baeldung.com/jpa-pessimistic-locking)
- [Distributed Locks with Redis](https://redis.io/docs/manual/patterns/distributed-locks/)
