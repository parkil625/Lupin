# Test Coverage Improvement Plan

## 목표
- **현재 커버리지**: 56%
- **목표 커버리지**: 80%
- **SonarCloud Quality Gate**: 통과

## 원칙
- TDD 원칙 준수: Red → Green → Refactor
- 엔티티 수정 금지
- 테스트 난이도 유지 (정석으로 해결)
- 구조적 변경과 행동적 변경 분리

---

## Phase 1: 서비스 레이어 테스트 추가 (가장 큰 영향)

### ✅ 완료된 테스트
- [x] ChatMessageControllerTest.sendMessage_Success (수정 완료)
- [x] PrescriptionControllerTest.createPrescription_Success (수정 완료)
- [x] AppointmentController - 7개 테스트 추가
- [x] ChatMessageController - 8개 테스트 추가 (1개 수정)
- [x] PrescriptionController - 10개 테스트 추가 (1개 수정)

### 🔲 Service 레이어 (현재 1% → 목표 60%+)

#### High Priority - FeedService (현재 22%)
- [ ] Feed 생성 테스트
- [ ] Feed 수정 테스트
- [ ] Feed 삭제 테스트
- [ ] Feed 좋아요 추가 테스트
- [ ] Feed 좋아요 취소 테스트
- [ ] Feed 조회 권한 검증 테스트

#### High Priority - FeedQueryService (현재 24%)
- [ ] Feed 목록 조회 (필터링) 테스트
- [ ] Feed 상세 조회 테스트
- [ ] Feed 검색 테스트
- [ ] 페이징 처리 테스트

#### Medium Priority - UserQueryService (현재 32%)
- [ ] 사용자 조회 테스트
- [ ] 사용자 검색 테스트
- [ ] 사용자 통계 조회 테스트

#### Medium Priority - ChatMessageService (현재 0%)
- [ ] 메시지 전송 테스트
- [ ] 메시지 조회 테스트
- [ ] 읽음 처리 테스트
- [ ] 메시지 삭제 테스트

#### Medium Priority - PrescriptionService (현재 0%)
- [ ] 처방전 생성 테스트
- [ ] 처방전 조회 테스트
- [ ] 처방전 수정 테스트
- [ ] 처방전 삭제 테스트

---

## Phase 2: Repository 레이어 테스트 추가

### 🔲 Repository Custom (현재 0% → 목표 70%+)
- [ ] FeedRepositoryImpl QueryDSL 테스트
- [ ] 복잡한 검색 쿼리 테스트
- [ ] 페이징 및 정렬 테스트

---

## Phase 3: 엔티티 비즈니스 로직 테스트

### 🔲 Entity 레이어 (현재 19% → 목표 60%+)
- [ ] Challenge.canJoin() 테스트
- [ ] Challenge.start() 테스트
- [ ] Challenge.end() 테스트
- [ ] User.addPoints() 테스트
- [ ] User.deductPoints() 테스트
- [ ] Feed.like() / unlike() 테스트
- [ ] Comment.isReply() 테스트

---

## Phase 4: DTO 변환 로직 테스트

### 🔲 DTO Response (현재 0% → 목표 80%+)
- [ ] ChatMessageResponse.from() 테스트
- [ ] FeedListResponse 변환 테스트
- [ ] PrescriptionResponse 변환 테스트

---

## 우선순위 결정 기준
1. **영향도**: Service 레이어가 가장 큰 커버리지 향상 기대
2. **복잡도**: 비즈니스 로직이 많은 부분 우선
3. **의존성**: 의존성이 적은 단위부터 테스트

---

## 커버리지 목표 분포

| 레이어 | 현재 | 목표 | 우선순위 |
|--------|------|------|----------|
| Service | 1% | 60%+ | 🔴 High |
| Entity | 19% | 60%+ | 🟡 Medium |
| Controller | 2% | 80%+ | 🟢 Low (이미 많이 추가됨) |
| Repository Custom | 0% | 70%+ | 🟡 Medium |
| DTO Response | 0% | 80%+ | 🟢 Low |

---

## 다음 작업
**"go"** 명령 시 다음 테스트 구현:
→ **FeedService.createFeed() 테스트** (Red → Green → Refactor)
