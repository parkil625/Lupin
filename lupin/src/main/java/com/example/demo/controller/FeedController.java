package com.example.demo.controller;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.FeedRequest;
import com.example.demo.dto.response.FeedResponse;
import com.example.demo.security.CurrentUser;
import com.example.demo.service.FeedLikeService;
import com.example.demo.service.FeedReportService;
import com.example.demo.service.FeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final FeedLikeService feedLikeService;
    private final FeedReportService feedReportService;

    @PostMapping
    public ResponseEntity<FeedResponse> createFeed(
            @CurrentUser User user,
            @Valid @RequestBody FeedRequest request
    ) {
        // 타입별 필드 우선 사용, 없으면 기존 images 배열 사용 (하위 호환)
        Feed feed = feedService.createFeed(
                user,
                request.getActivity(),
                request.getContent(),
                request.getStartImageKey(),
                request.getEndImageKey(),
                request.getOtherImageKeys()
        );
        return ResponseEntity.ok(toFeedResponse(feed));
    }

    @PutMapping("/{feedId}")
    public ResponseEntity<FeedResponse> updateFeed(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @Valid @RequestBody FeedRequest request
    ) {
        Feed feed;
        String startKey = request.getStartImageKey();
        String endKey = request.getEndImageKey();

        if (startKey != null && endKey != null) {
            // 타입별 필드 사용 (권장)
            feed = feedService.updateFeed(
                    user,
                    feedId,
                    request.getContent(),
                    request.getActivity(),
                    startKey,
                    endKey,
                    request.getOtherImageKeys()
            );
        } else {
            // 이미지가 없으면 내용만 수정
            feed = feedService.updateFeed(user, feedId, request.getContent(), request.getActivity());
        }
        return ResponseEntity.ok(toFeedResponse(feed));
    }

    @DeleteMapping("/{feedId}")
    public ResponseEntity<Void> deleteFeed(
            @CurrentUser User user,
            @PathVariable Long feedId
    ) {
        feedService.deleteFeed(user, feedId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{feedId}")
    public ResponseEntity<FeedResponse> getFeedDetail(@PathVariable Long feedId) {
        Feed feed = feedService.getFeedDetail(feedId);
        return ResponseEntity.ok(toFeedResponse(feed));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHomeFeeds(
            @CurrentUser User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Slice<Feed> feeds = feedService.getHomeFeeds(user, page, size);
        Map<Long, Integer> activeDaysMap = feedService.getActiveDaysMap(feeds.getContent());
        return ResponseEntity.ok(Map.of(
                "content", feeds.getContent().stream()
                        .map(feed -> toFeedResponseWithActiveDays(feed, user, activeDaysMap))
                        .toList(),
                "hasNext", feeds.hasNext()
        ));
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyFeeds(
            @CurrentUser User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Slice<Feed> feeds = feedService.getMyFeeds(user, page, size);
        Map<Long, Integer> activeDaysMap = feedService.getActiveDaysMap(feeds.getContent());
        return ResponseEntity.ok(Map.of(
                "content", feeds.getContent().stream()
                        .map(feed -> toFeedResponseWithActiveDays(feed, user, activeDaysMap))
                        .toList(),
                "hasNext", feeds.hasNext()
        ));
    }

    @GetMapping("/can-post-today")
    public ResponseEntity<Boolean> canPostToday(
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(feedService.canPostToday(user));
    }

    @PostMapping("/{feedId}/like")
    public ResponseEntity<Void> likeFeed(
            @CurrentUser User user,
            @PathVariable Long feedId
    ) {
        feedLikeService.likeFeed(user, feedId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{feedId}/like")
    public ResponseEntity<Void> unlikeFeed(
            @CurrentUser User user,
            @PathVariable Long feedId
    ) {
        feedLikeService.unlikeFeed(user, feedId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{feedId}/report")
    public ResponseEntity<Void> reportFeed(
            @CurrentUser User user,
            @PathVariable Long feedId
    ) {
        feedReportService.toggleReport(user, feedId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/likes/{feedLikeId}")
    public ResponseEntity<Map<String, Long>> getFeedLike(@PathVariable Long feedLikeId) {
        Long feedId = feedLikeService.getFeedIdByFeedLikeId(feedLikeId);
        if (feedId == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("feedId", feedId));
    }

    // Feed 엔티티의 반정규화 필드 사용 - DB 조회 없음
    private FeedResponse toFeedResponse(Feed feed) {
        return FeedResponse.from(feed);
    }

    // Feed 엔티티의 반정규화 필드 사용 - isLiked만 DB 조회
    private FeedResponse toFeedResponse(Feed feed, User currentUser) {
        boolean isLiked = currentUser != null && feedLikeService.isLiked(currentUser.getId(), feed.getId());
        return FeedResponse.from(feed, isLiked);
    }

    // activeDays 포함 응답 생성
    private FeedResponse toFeedResponseWithActiveDays(Feed feed, User currentUser, Map<Long, Integer> activeDaysMap) {
        boolean isLiked = currentUser != null && feedLikeService.isLiked(currentUser.getId(), feed.getId());
        Integer activeDays = activeDaysMap.getOrDefault(feed.getWriter().getId(), 0);
        return FeedResponse.from(feed, isLiked, activeDays);
    }
}
