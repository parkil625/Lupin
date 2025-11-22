package com.example.demo.controller;

import com.example.demo.dto.request.FeedCreateRequest;
import com.example.demo.dto.request.FeedUpdateRequest;
import com.example.demo.dto.response.FeedDetailResponse;
import com.example.demo.dto.response.FeedListResponse;
import com.example.demo.service.FeedCommandService;
import com.example.demo.service.FeedQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 피드 관련 API
 * CQRS 패턴 적용 - Command/Query 서비스 분리
 */
@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedCommandService feedCommandService;
    private final FeedQueryService feedQueryService;

    // ========== Command API ==========

    /**
     * 피드 생성
     */
    @PostMapping
    public ResponseEntity<Map<String, Long>> createFeed(
            @RequestParam Long userId,
            @Valid @RequestBody FeedCreateRequest request) {
        Long feedId = feedCommandService.createFeed(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("feedId", feedId));
    }

    /**
     * 피드 수정
     */
    @PutMapping("/{feedId}")
    public ResponseEntity<Void> updateFeed(
            @PathVariable Long feedId,
            @RequestParam Long userId,
            @Valid @RequestBody FeedUpdateRequest request) {
        feedCommandService.updateFeed(feedId, userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 피드 삭제
     */
    @DeleteMapping("/{feedId}")
    public ResponseEntity<Void> deleteFeed(
            @PathVariable Long feedId,
            @RequestParam Long userId) {
        feedCommandService.deleteFeed(feedId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 피드 좋아요
     */
    @PostMapping("/{feedId}/like")
    public ResponseEntity<Void> likeFeed(
            @PathVariable Long feedId,
            @RequestParam Long userId) {
        feedCommandService.likeFeed(feedId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 피드 좋아요 취소
     */
    @DeleteMapping("/{feedId}/like")
    public ResponseEntity<Void> unlikeFeed(
            @PathVariable Long feedId,
            @RequestParam Long userId) {
        feedCommandService.unlikeFeed(feedId, userId);
        return ResponseEntity.noContent().build();
    }

    // ========== Query API ==========

    /**
     * 피드 목록 조회 (검색, 페이징)
     */
    @GetMapping
    public ResponseEntity<Page<FeedListResponse>> getFeeds(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) Long excludeUserId,
            @RequestParam(required = false) Long excludeFeedId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FeedListResponse> feeds = feedQueryService.getFeeds(keyword, activityType, excludeUserId, excludeFeedId, pageable);
        return ResponseEntity.ok(feeds);
    }

    /**
     * 피드 상세 조회
     */
    @GetMapping("/{feedId}")
    public ResponseEntity<FeedDetailResponse> getFeedDetail(@PathVariable Long feedId) {
        FeedDetailResponse feed = feedQueryService.getFeedDetail(feedId);
        return ResponseEntity.ok(feed);
    }

    /**
     * 특정 사용자의 피드 조회
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<FeedListResponse>> getFeedsByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FeedListResponse> feeds = feedQueryService.getFeedsByUserId(userId, pageable);
        return ResponseEntity.ok(feeds);
    }

    /**
     * 오늘 피드 작성 가능 여부 확인
     */
    @GetMapping("/can-post")
    public ResponseEntity<Boolean> canPostToday(@RequestParam Long userId) {
        boolean canPost = feedQueryService.canPostToday(userId);
        return ResponseEntity.ok(canPost);
    }
}
