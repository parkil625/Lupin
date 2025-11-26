# Project Implementation Plan

## 현재 목표
대시보드 - 사용자 피드 목록 조회 (무한 스크롤)

---

## Phase 1: 사용자 피드 목록 조회

### Repository
- [ ] FeedRepository.findByWriterIdOrderByIdDesc (Slice, Pageable)

### Service
- [ ] FeedService.getMyFeeds

### Controller
- [ ] GET /api/feeds/my

---

## 다음 작업
→ FeedRepository.findByWriterIdOrderByIdDesc 테스트 작성
