# 🔄 Lupin 프로젝트 리팩토링 완료 가이드

## 📅 리팩토링 일자: 2025-11-25

## 🎯 리팩토링 목표
**"백엔드 기능 구현을 처음부터 다시 시작하기 위한 클린 슬레이트(Clean Slate) 제공"**

---

## ✅ 완료된 작업

### 1. **MedicalStaff → DoctorProfile 리팩토링** ⭐
#### 변경 사유:
- 기존: MedicalStaff가 별도 인증 시스템을 가진 독립적 의사 엔티티
- 문제: Prescription/Appointment가 User를 참조 → 설계 충돌
- 해결: **"의사도 직원이다"** 철학으로 통합

#### 새로운 구조:
```
User (모든 직원)
├─ role: MEMBER, ADMIN, DOCTOR
├─ 공통: 인증, 피드, 포인트, 경매 참여
└─ department: 부서

DoctorProfile (의사 추가 정보) - Optional
├─ user_id FK -> User (1:1)
├─ specialty (전공)
├─ license_number (면허번호)
└─ medical_experience (경력)
```

#### 변경된 파일:
- ✅ **생성**: `DoctorProfile.java` (entity)
- ✅ **생성**: `DoctorProfileRepository.java`
- ✅ **삭제**: `MedicalStaff.java`
- ✅ **삭제**: `MedicalStaffRepository.java`

#### 데이터베이스 마이그레이션:
```sql
-- database/migration/V2__refactor_medical_staff_to_doctor_profile.sql 참조
```

---

### 2. **비즈니스 로직 대량 삭제** 🗑️

#### 삭제된 Controllers (11개):
```
❌ AppointmentController
❌ AuctionController
❌ ChatMessageController
❌ ChatWebSocketController
❌ CommentController
❌ FeedController
❌ ImageController
❌ NotificationController
❌ PrescriptionController
❌ ReportController
❌ UserController
```

#### 삭제된 Services (20개):
```
❌ AppointmentService
❌ AuctionService
❌ ChatMessageService
❌ CommentCommandService
❌ CommentQueryService
❌ CommentService
❌ DistributedLockService
❌ FeedCommandService
❌ FeedQueryService
❌ FeedService
❌ ImageService
❌ NotificationService
❌ OutboxService
❌ PrescriptionService
❌ RedisCounterService
❌ RedisLuaService
❌ ReportService
❌ ResilientRedisService
❌ UserQueryService
❌ UserService
```

#### 삭제된 Repositories (16개):
```
❌ AppointmentRepository
❌ AuctionBidRepository
❌ AuctionRepository
❌ ChatMessageRepository
❌ CommentLikeRepository
❌ CommentRepository
❌ FeedLikeRepository
❌ FeedRepository
❌ MedicalStaffRepository
❌ NotificationRepository
❌ OutboxRepository
❌ PrescriptionMedRepository
❌ PrescriptionRepository
❌ PrizeClaimRepository
❌ ReportRepository
❌ UserPenaltyRepository
```

#### 삭제된 Tests (50+개):
```
❌ 모든 비즈니스 로직 관련 테스트 코드
```

---

### 3. **유지된 코어 시스템** ✅

#### Controllers (3개):
```
✅ AuthController       (인증)
✅ OAuthController      (OAuth 로그인)
✅ HealthController     (헬스체크 - CI/CD용)
```

#### Services (2개):
```
✅ AuthService          (인증 서비스)
✅ OAuthService         (OAuth 서비스)
```

#### Repositories (3개):
```
✅ UserRepository       (사용자)
✅ UserOAuthRepository  (OAuth 연동)
✅ DoctorProfileRepository (의사 프로필 - 새로 생성)
```

#### Tests (유지):
```
✅ AuthServiceTest
✅ AuthControllerTest
✅ OAuthServiceTest
✅ OAuthControllerTest
✅ HealthControllerTest
✅ JwtTokenProviderTest
✅ SecurityIntegrationTest
✅ NaverOAuthAdapterTest
✅ KakaoOAuthAdapterTest
✅ OAuthProviderFactoryTest
✅ UserOAuthTest
```

---

## 📊 리팩토링 통계

| 항목 | 삭제 | 유지 | 생성 |
|------|------|------|------|
| **Controllers** | 11 | 3 | 0 |
| **Services** | 20 | 2 | 0 |
| **Repositories** | 16 | 2 | 1 |
| **Entities** | 1 (MedicalStaff) | 18 | 1 (DoctorProfile) |
| **Tests** | 50+ | 11 | 0 |

---

## 🏗️ 현재 프로젝트 구조

```
src/main/java/com/example/demo/
├── controller/
│   ├── AuthController.java          ✅ 인증
│   ├── OAuthController.java         ✅ OAuth
│   └── HealthController.java        ✅ 헬스체크
│
├── service/
│   ├── AuthService.java             ✅ 인증 서비스
│   └── OAuthService.java            ✅ OAuth 서비스
│
├── repository/
│   ├── UserRepository.java          ✅ 사용자
│   ├── UserOAuthRepository.java     ✅ OAuth
│   └── DoctorProfileRepository.java ✅ 의사 프로필 (신규)
│
├── domain/entity/
│   ├── User.java                    ✅ 직원 (role: MEMBER, ADMIN, DOCTOR)
│   ├── UserOAuth.java               ✅ OAuth 연동
│   ├── DoctorProfile.java           ✅ 의사 추가 정보 (신규)
│   ├── Feed.java                    ✅ 피드
│   ├── FeedImage.java               ✅ 피드 이미지
│   ├── FeedLike.java                ✅ 피드 좋아요
│   ├── Comment.java                 ✅ 댓글
│   ├── CommentLike.java             ✅ 댓글 좋아요
│   ├── Notification.java            ✅ 알림
│   ├── Report.java                  ✅ 신고
│   ├── UserPenalty.java             ✅ 패널티
│   ├── Auction.java                 ✅ 경매
│   ├── AuctionBid.java              ✅ 입찰
│   ├── Appointment.java             ✅ 예약
│   ├── Prescription.java            ✅ 처방전
│   ├── PrescriptionMed.java         ✅ 처방 약품
│   ├── ChatMessage.java             ✅ 채팅
│   └── Outbox.java                  ✅ 이벤트 아웃박스
│
├── security/                        ✅ 인증/인가 (유지)
├── oauth/                           ✅ OAuth (유지)
└── config/                          ✅ 설정 (유지)
```

---

## 🚀 다음 단계 (백엔드 재구현 가이드)

### Phase 1: Feed 도메인 구현
```
1. FeedRepository      (Repository 계층)
2. FeedService         (Service 계층)
3. FeedController      (Controller 계층)
4. FeedServiceTest     (테스트 코드)
```

### Phase 2: Comment 도메인 구현
```
1. CommentRepository
2. CommentService
3. CommentController
4. CommentServiceTest
```

### Phase 3: Auction 도메인 구현
```
1. AuctionRepository
2. AuctionService
3. AuctionController
4. AuctionServiceTest
```

### Phase 4: Medical 도메인 구현
```
1. AppointmentRepository
2. PrescriptionRepository
3. AppointmentService
4. PrescriptionService
5. AppointmentController
6. PrescriptionController
7. Tests
```

### Phase 5: 기타 도메인
- Notification
- Report
- ChatMessage
- Outbox

---

## 🔧 데이터베이스 마이그레이션

### 실행 방법:
```bash
# MySQL 접속
mysql -u root -p lupin

# 마이그레이션 실행
source database/migration/V2__refactor_medical_staff_to_doctor_profile.sql;

# 검증
SELECT COUNT(*) FROM doctor_profiles;
SELECT u.user_id, u.real_name, u.role, dp.specialty
FROM users u
LEFT JOIN doctor_profiles dp ON dp.user_id = u.id
WHERE u.role = 'DOCTOR';
```

---

## 📝 Entity 설계 원칙

### ✅ 유지된 엔티티 (18개):
1. **User** - 모든 직원 (role: MEMBER, ADMIN, DOCTOR)
2. **UserOAuth** - OAuth 연동 정보
3. **DoctorProfile** - 의사 추가 정보 (User와 1:1)
4. **Feed** - 운동 피드
5. **FeedImage** - 피드 이미지
6. **FeedLike** - 피드 좋아요
7. **Comment** - 댓글
8. **CommentLike** - 댓글 좋아요
9. **Notification** - 알림
10. **Report** - 신고
11. **UserPenalty** - 패널티
12. **Auction** - 경매 (체스 초읽기 방식)
13. **AuctionBid** - 입찰
14. **Appointment** - 예약
15. **Prescription** - 처방전
16. **PrescriptionMed** - 처방 약품
17. **ChatMessage** - 채팅
18. **Outbox** - 이벤트 아웃박스

---

## 📌 중요 참고사항

### 1. Prescription/Appointment의 doctor_id
- **현재 상태**: User 참조 (올바름)
- **이유**: 의사도 직원이므로 User.role=DOCTOR 사용

### 2. PrizeClaim 엔티티
- **상태**: 존재하지만 PrizeType enum이 없어서 컴파일 에러
- **권장**: 삭제 또는 수정 필요

### 3. BaseEntity
- **포함 필드**: createdAt, updatedAt
- **상속 엔티티**: UserOAuth, Feed, Comment, Appointment, Prescription, DoctorProfile, Outbox

---

## 🎓 도메인 모델 철학

### User 중심 통합 모델
```
"의사도 우리 직원이다"

- 의사는 User.role=DOCTOR
- DoctorProfile은 의사의 "추가 정보"일 뿐
- 의사도 피드 올리고, 포인트 받고, 경매 참여
- 단일 인증 시스템 (복잡도 ↓)
- 확장 용이 (영양사, 트레이너 추가 시 role만 확장)
```

---

## ✨ 이제 처음부터 깔끔하게 백엔드를 구현할 수 있습니다!

**남은 것:**
- ✅ 도메인 엔티티 (18개)
- ✅ 인증/인가 시스템
- ✅ OAuth 시스템
- ✅ CI/CD 인프라

**구현할 것:**
- ⚪ 비즈니스 로직 (Controller, Service, Repository)
- ⚪ 테스트 코드

**행운을 빕니다! 🚀**
