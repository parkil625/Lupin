package com.example.demo.controller;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.command.FeedCreateCommand;
import com.example.demo.dto.command.FeedUpdateCommand;
import com.example.demo.dto.request.FeedRequest;
import com.example.demo.dto.response.FeedResponse;
import com.example.demo.dto.response.SliceResponse;
import com.example.demo.security.CurrentUser;
import com.example.demo.service.FeedLikeService;
import com.example.demo.service.FeedQueryFacade;
import com.example.demo.service.FeedReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedQueryFacade feedQueryFacade;
    private final FeedLikeService feedLikeService;
    private final FeedReportService feedReportService;

    @PostMapping
    public ResponseEntity<FeedResponse> createFeed(
            @CurrentUser User user,
            @Valid @RequestBody FeedRequest request
    ) {
        FeedCreateCommand command = FeedCreateCommand.of(user, request);
        return ResponseEntity.ok(feedQueryFacade.createFeed(command));
    }

    @PutMapping("/{feedId}")
    public ResponseEntity<FeedResponse> updateFeed(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @Valid @RequestBody FeedRequest request
    ) {
        FeedUpdateCommand command = FeedUpdateCommand.of(user, feedId, request);
        return ResponseEntity.ok(feedQueryFacade.updateFeed(command));
    }

    @DeleteMapping("/{feedId}")
    public ResponseEntity<Void> deleteFeed(
            @CurrentUser User user,
            @PathVariable Long feedId
    ) {
        feedQueryFacade.deleteFeed(user, feedId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{feedId}")
    public ResponseEntity<FeedResponse> getFeedDetail(
            @CurrentUser User user,
            @PathVariable Long feedId
    ) {
        return ResponseEntity.ok(feedQueryFacade.getFeedDetail(user, feedId));
    }

    @GetMapping
    public ResponseEntity<SliceResponse<FeedResponse>> getHomeFeeds(
            @CurrentUser User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search // [추가] 검색어 파라미터
    ) {
        return ResponseEntity.ok(feedQueryFacade.getHomeFeeds(user, page, size, search));
    }

    @GetMapping("/my")
    public ResponseEntity<SliceResponse<FeedResponse>> getMyFeeds(
            @CurrentUser User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(feedQueryFacade.getMyFeeds(user, page, size));
    }

    @GetMapping("/can-post-today")
    public ResponseEntity<Boolean> canPostToday(
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(feedQueryFacade.canPostToday(user));
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
    public ResponseEntity<Boolean> reportFeed(
            @CurrentUser User user,
            @PathVariable Long feedId
    ) {
        boolean isReported = feedReportService.toggleReport(user, feedId);
        return ResponseEntity.ok(isReported);
    }

    @GetMapping("/likes/{feedLikeId}")
    public ResponseEntity<Map<String, Long>> getFeedLike(@PathVariable Long feedLikeId) {
        Long feedId = feedLikeService.getFeedIdByFeedLikeId(feedLikeId);
        if (feedId == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("feedId", feedId));
    }
}
