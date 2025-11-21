package com.example.demo.controller;

import com.example.demo.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 피드 신고
     */
    @PostMapping("/feeds/{feedId}")
    public ResponseEntity<Map<String, String>> reportFeed(
            @PathVariable Long feedId,
            @RequestParam Long userId,
            @RequestParam(required = false) String reason) {

        reportService.reportFeed(feedId, userId, reason);
        return ResponseEntity.ok(Map.of("message", "신고가 접수되었습니다."));
    }

    /**
     * 댓글 신고
     */
    @PostMapping("/comments/{commentId}")
    public ResponseEntity<Map<String, String>> reportComment(
            @PathVariable Long commentId,
            @RequestParam Long userId,
            @RequestParam(required = false) String reason) {

        reportService.reportComment(commentId, userId, reason);
        return ResponseEntity.ok(Map.of("message", "신고가 접수되었습니다."));
    }

    /**
     * 사용자 신고 기록 확인 (3일 내)
     */
    @GetMapping("/check/{userId}")
    public ResponseEntity<Map<String, Boolean>> checkUserReportStatus(@PathVariable Long userId) {
        boolean hasRecentReport = reportService.hasRecentReport(userId);
        return ResponseEntity.ok(Map.of("hasRecentReport", hasRecentReport));
    }

    /**
     * 사용자가 특정 피드를 신고했는지 확인
     */
    @GetMapping("/feeds/{feedId}/status")
    public ResponseEntity<Map<String, Object>> getFeedReportStatus(
            @PathVariable Long feedId,
            @RequestParam Long userId) {

        boolean reported = reportService.hasUserReportedFeed(userId, feedId);
        Long reportCount = reportService.getFeedReportCount(feedId);
        return ResponseEntity.ok(Map.of(
                "reported", reported,
                "reportCount", reportCount
        ));
    }

    /**
     * 사용자가 특정 댓글을 신고했는지 확인
     */
    @GetMapping("/comments/{commentId}/status")
    public ResponseEntity<Map<String, Object>> getCommentReportStatus(
            @PathVariable Long commentId,
            @RequestParam Long userId) {

        boolean reported = reportService.hasUserReportedComment(userId, commentId);
        Long reportCount = reportService.getCommentReportCount(commentId);
        return ResponseEntity.ok(Map.of(
                "reported", reported,
                "reportCount", reportCount
        ));
    }
}
