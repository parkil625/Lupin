# Project Implementation Plan

## 목표
- **Health Management Platform** 백엔드 기능 구현
- **TDD (Test Driven Development)** 및 **Tidy First** 원칙 준수
- **Scope**: User manages `BaseEntity`, `Comment`, `CommentLike`, `Feed`, `FeedImage`, `FeedLike`, `Notification`, `Outbox`, `Report`, `User`, `UserOAuth`, `UserPenalty`. Other entities are out of scope.

## 원칙
- **TDD Cycle**: Red (실패하는 테스트) → Green (최소한의 구현) → Refactor (리팩토링)
- **Vertical Slicing**: 기능 단위로 Repository → Service → Controller 순차 구현
- **Test Coverage**: 비즈니스 로직(Service) 80% 이상 목표

---

## Phase 1: Missing Repositories Implementation
기존 엔티티에 대한 누락된 Repository 인터페이스 생성 (Allowed Entities Only)

### ✅ Repositories
- [x] FeedLikeRepository 생성
- [x] CommentLikeRepository 생성
- [x] FeedImageRepository 생성
- [x] UserOAuthRepository 생성
- [x] UserPenaltyRepository 생성
- [x] OutboxRepository 생성
- [x] ReportRepository 생성
- [x] NotificationRepository 생성

---

## Phase 2: Feed Feature Implementation
피드 생성, 조회, 좋아요, 댓글 기능 구현

### 🔲 FeedService & Controller
- [ ] Feed 생성 (Create)
- [ ] Feed 목록 조회 (Read - Pagination)
- [ ] Feed 상세 조회 (Read)
- [ ] Feed 수정 (Update)
- [ ] Feed 삭제 (Delete)
- [ ] Feed 좋아요/취소 (Like/Unlike)

### 🔲 CommentService & Controller
- [ ] 댓글 작성
- [ ] 댓글 목록 조회
- [ ] 댓글 삭제

---

## Phase 3: User & Notification Feature
사용자 및 알림 기능 구현

### 🔲 UserService & Controller
- [ ] 사용자 정보 조회
- [ ] 사용자 정보 수정
- [ ] 회원 탈퇴 (Soft Delete)

### 🔲 NotificationService & Controller
- [ ] 알림 목록 조회
- [ ] 알림 읽음 처리

---

## Phase 4: Report Feature
신고 기능 구현

### 🔲 ReportService & Controller
- [ ] 신고 접수

---

## 다음 작업
**"go"** 명령 시 다음 작업 수행:
→ **Phase 2: Feed 생성 (Create) - FeedService 테스트 작성**
