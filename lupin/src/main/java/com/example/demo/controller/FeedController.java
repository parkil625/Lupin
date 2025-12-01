package com.example.demo.controller;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.FeedRequest;
import com.example.demo.dto.response.FeedResponse;
import com.example.demo.repository.CommentRepository;
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
    private final FeedLikeRepository feedLikeRepository;
    private final CommentRepository commentRepository;

    @PostMapping
    public ResponseEntity<FeedResponse> createFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FeedRequest request
    ) {
        User user = getCurrentUser(userDetails);
        Feed feed = feedService.createFeed(user, request.getActivity(), request.getContent(), request.getImageUrls());
        return ResponseEntity.ok(toFeedResponse(feed));
    }

    @PutMapping("/{feedId}")
    public ResponseEntity<FeedResponse> updateFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long feedId,
            @Valid @RequestBody FeedRequest request
    ) {
        User user = getCurrentUser(userDetails);
        Feed feed = feedService.updateFeed(user, feedId, request.getContent(), request.getActivity());
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
                "content", feeds.getContent().stream().map(this::toFeedResponse).toList(),
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
                "content", feeds.getContent().stream().map(this::toFeedResponse).toList(),
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

    private FeedResponse toFeedResponse(Feed feed) {
        long likeCount = feedLikeRepository.countByFeed(feed);
        long commentCount = commentRepository.countByFeed(feed);
        return FeedResponse.from(feed, likeCount, commentCount);
    }
}
