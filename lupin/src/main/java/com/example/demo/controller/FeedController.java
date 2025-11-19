package com.example.demo.controller;

import com.example.demo.dto.request.FeedCreateRequest;
import com.example.demo.dto.request.FeedUpdateRequest;
import com.example.demo.dto.response.FeedDetailResponse;
import com.example.demo.dto.response.FeedListResponse;
import com.example.demo.service.FeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 피드 관련 API
 */
@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * 피드 생성
     */
    @PostMapping
    public ResponseEntity<FeedDetailResponse> createFeed(
            @RequestParam Long userId,
            @Valid @RequestBody FeedCreateRequest request) {
        FeedDetailResponse response = feedService.createFeed(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 피드 목록 조회 (검색, 페이징)
     */
    @GetMapping
    public ResponseEntity<Page<FeedListResponse>> getFeeds(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String activityType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FeedListResponse> feeds = feedService.getFeeds(keyword, activityType, pageable);
        return ResponseEntity.ok(feeds);
    }

    /**
     * 피드 상세 조회
     */
    @GetMapping("/{feedId}")
    public ResponseEntity<FeedDetailResponse> getFeedDetail(@PathVariable Long feedId) {
        FeedDetailResponse feed = feedService.getFeedDetail(feedId);
        return ResponseEntity.ok(feed);
    }

    /**
     * 피드 수정
     */
    @PutMapping("/{feedId}")
    public ResponseEntity<FeedDetailResponse> updateFeed(
            @PathVariable Long feedId,
            @RequestParam Long userId,
            @Valid @RequestBody FeedUpdateRequest request) {
        FeedDetailResponse response = feedService.updateFeed(feedId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 피드 삭제
     */
    @DeleteMapping("/{feedId}")
    public ResponseEntity<Void> deleteFeed(
            @PathVariable Long feedId,
            @RequestParam Long userId) {
        feedService.deleteFeed(feedId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 피드 좋아요
     */
    @PostMapping("/{feedId}/like")
    public ResponseEntity<Void> likeFeed(
            @PathVariable Long feedId,
            @RequestParam Long userId) {
        feedService.likeFeed(feedId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 피드 좋아요 취소
     */
    @DeleteMapping("/{feedId}/like")
    public ResponseEntity<Void> unlikeFeed(
            @PathVariable Long feedId,
            @RequestParam Long userId) {
        feedService.unlikeFeed(feedId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 오늘 피드 작성 가능 여부 확인
     */
    @GetMapping("/can-post")
    public ResponseEntity<Boolean> canPostToday(@RequestParam Long userId) {
        boolean canPost = feedService.canPostToday(userId);
        return ResponseEntity.ok(canPost);
    }
}
