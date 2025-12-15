package com.example.demo.controller;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.command.FeedCreateCommand;
import com.example.demo.dto.command.FeedUpdateCommand;
import com.example.demo.dto.request.FeedRequest;
import com.example.demo.dto.response.FeedResponse;
import com.example.demo.dto.response.SliceResponse;
import com.example.demo.security.CurrentUser;
import com.example.demo.service.FeedFacade;
import com.example.demo.service.FeedLikeService;
import com.example.demo.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedFacade feedFacade;
    private final FeedLikeService feedLikeService;
    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<FeedResponse> createFeed(
            @CurrentUser User user,
            @Valid @RequestBody FeedRequest request
    ) {
        FeedCreateCommand command = FeedCreateCommand.of(user, request);
        return ResponseEntity.ok(feedFacade.createFeed(command));
    }

    @PutMapping("/{feedId}")
    public ResponseEntity<FeedResponse> updateFeed(
            @CurrentUser User user,
            @PathVariable Long feedId,
            @Valid @RequestBody FeedRequest request
    ) {
        FeedUpdateCommand command = FeedUpdateCommand.of(user, feedId, request);
        return ResponseEntity.ok(feedFacade.updateFeed(command));
    }

    @DeleteMapping("/{feedId}")
    public ResponseEntity<Void> deleteFeed(
            @CurrentUser User user,
            @PathVariable Long feedId
    ) {
        feedFacade.deleteFeed(user, feedId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{feedId}")
    public ResponseEntity<FeedResponse> getFeedDetail(@PathVariable Long feedId) {
        return ResponseEntity.ok(feedFacade.getFeedDetail(feedId));
    }

    @GetMapping
    public ResponseEntity<SliceResponse<FeedResponse>> getHomeFeeds(
            @CurrentUser User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(feedFacade.getHomeFeeds(user, page, size));
    }

    @GetMapping("/my")
    public ResponseEntity<SliceResponse<FeedResponse>> getMyFeeds(
            @CurrentUser User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(feedFacade.getMyFeeds(user, page, size));
    }

    @GetMapping("/can-post-today")
    public ResponseEntity<Boolean> canPostToday(
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(feedFacade.canPostToday(user));
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
        reportService.toggleFeedReport(user, feedId);
        return ResponseEntity.ok().build();
    }
}
