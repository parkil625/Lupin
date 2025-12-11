package com.example.demo.controller;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.FeedRequest;
import com.example.demo.dto.response.FeedResponse;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.service.FeedLikeService;
import com.example.demo.service.FeedReportService;
import com.example.demo.service.FeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController extends BaseController {

    private final FeedService feedService;
    private final FeedLikeService feedLikeService;
    private final FeedReportService feedReportService;
    private final FeedLikeRepository feedLikeRepository; // [최적화] isLiked 체크용으로만 사용

    @PostMapping
    public ResponseEntity<FeedResponse> createFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FeedRequest request
    ) {
        User user = getCurrentUser(userDetails);
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
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long feedId,
            @Valid @RequestBody FeedRequest request
    ) {
        User user = getCurrentUser(userDetails);
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
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long feedId
    ) {
        User user = getCurrentUser(userDetails);
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
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = getCurrentUser(userDetails);
        Slice<Feed> feeds = feedService.getHomeFeeds(user, page, size);
        return ResponseEntity.ok(Map.of(
                "content", feeds.getContent().stream().map(feed -> toFeedResponse(feed, user)).toList(),
                "hasNext", feeds.hasNext()
        ));
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyFeeds(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = getCurrentUser(userDetails);
        Slice<Feed> feeds = feedService.getMyFeeds(user, page, size);
        return ResponseEntity.ok(Map.of(
                "content", feeds.getContent().stream().map(feed -> toFeedResponse(feed, user)).toList(),
                "hasNext", feeds.hasNext()
        ));
    }

    @GetMapping("/can-post-today")
    public ResponseEntity<Boolean> canPostToday(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(feedService.canPostToday(user));
    }

    @PostMapping("/{feedId}/like")
    public ResponseEntity<Void> likeFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long feedId
    ) {
        User user = getCurrentUser(userDetails);
        feedLikeService.likeFeed(user, feedId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{feedId}/like")
    public ResponseEntity<Void> unlikeFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long feedId
    ) {
        User user = getCurrentUser(userDetails);
        feedLikeService.unlikeFeed(user, feedId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{feedId}/report")
    public ResponseEntity<Void> reportFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long feedId
    ) {
        User user = getCurrentUser(userDetails);
        feedReportService.toggleReport(user, feedId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/likes/{feedLikeId}")
    public ResponseEntity<Map<String, Long>> getFeedLike(@PathVariable Long feedLikeId) {
        return feedLikeRepository.findById(feedLikeId)
                .map(feedLike -> ResponseEntity.ok(Map.of("feedId", feedLike.getFeed().getId())))
                .orElse(ResponseEntity.notFound().build());
    }

    // [최적화] Feed 엔티티의 반정규화 필드 사용 - DB 조회 없음
    private FeedResponse toFeedResponse(Feed feed) {
        return FeedResponse.from(feed);
    }

    // [최적화] Feed 엔티티의 반정규화 필드 사용 - isLiked만 DB 조회
    private FeedResponse toFeedResponse(Feed feed, User currentUser) {
        boolean isLiked = currentUser != null && feedLikeRepository.existsByUserIdAndFeedId(currentUser.getId(), feed.getId());
        return FeedResponse.from(feed, isLiked);
    }
}
