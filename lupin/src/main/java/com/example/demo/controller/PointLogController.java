package com.example.demo.controller;

import com.example.demo.dto.request.PointLogCreateRequest;
import com.example.demo.dto.response.PointLogResponse;
import com.example.demo.service.PointLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 포인트 로그 관련 API
 */
@RestController
@RequestMapping("/api/point-logs")
@RequiredArgsConstructor
public class PointLogController {

    private final PointLogService pointLogService;

    /**
     * 포인트 로그 생성 (관리자용)
     */
    @PostMapping
    public ResponseEntity<PointLogResponse> createPointLog(
            @Valid @RequestBody PointLogCreateRequest request) {
        PointLogResponse response = pointLogService.createPointLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 사용자의 포인트 로그 조회 (페이징)
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<PointLogResponse>> getPointLogsByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PointLogResponse> pointLogs = pointLogService.getPointLogsByUserId(userId, pageable);
        return ResponseEntity.ok(pointLogs);
    }

    /**
     * 특정 사용자의 포인트 로그 조회 (전체)
     */
    @GetMapping("/users/{userId}/all")
    public ResponseEntity<List<PointLogResponse>> getAllPointLogsByUserId(@PathVariable Long userId) {
        List<PointLogResponse> pointLogs = pointLogService.getAllPointLogsByUserId(userId);
        return ResponseEntity.ok(pointLogs);
    }

    /**
     * 포인트 로그 상세 조회
     */
    @GetMapping("/{pointLogId}")
    public ResponseEntity<PointLogResponse> getPointLogDetail(@PathVariable Long pointLogId) {
        PointLogResponse pointLog = pointLogService.getPointLogDetail(pointLogId);
        return ResponseEntity.ok(pointLog);
    }

    /**
     * 특정 사용자의 포인트 합계 조회
     */
    @GetMapping("/users/{userId}/total")
    public ResponseEntity<Long> getTotalPointsByUserId(@PathVariable Long userId) {
        Long totalPoints = pointLogService.getTotalPointsByUserId(userId);
        return ResponseEntity.ok(totalPoints);
    }

    /**
     * 특정 기간 내 포인트 로그 조회
     */
    @GetMapping("/users/{userId}/date-range")
    public ResponseEntity<List<PointLogResponse>> getPointLogsByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<PointLogResponse> pointLogs = pointLogService.getPointLogsByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(pointLogs);
    }

    /**
     * 특정 사유의 포인트 로그 조회
     */
    @GetMapping("/users/{userId}/reason")
    public ResponseEntity<Page<PointLogResponse>> getPointLogsByReason(
            @PathVariable Long userId,
            @RequestParam String reason,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PointLogResponse> pointLogs = pointLogService.getPointLogsByReason(userId, reason, pageable);
        return ResponseEntity.ok(pointLogs);
    }

    /**
     * 특정 참조 ID의 포인트 로그 조회
     */
    @GetMapping("/ref/{refId}")
    public ResponseEntity<List<PointLogResponse>> getPointLogsByRefId(@PathVariable String refId) {
        List<PointLogResponse> pointLogs = pointLogService.getPointLogsByRefId(refId);
        return ResponseEntity.ok(pointLogs);
    }

    /**
     * 특정 기간 내 포인트 합계 조회
     */
    @GetMapping("/users/{userId}/total/date-range")
    public ResponseEntity<Long> getTotalPointsByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        Long totalPoints = pointLogService.getTotalPointsByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(totalPoints);
    }

    /**
     * 최근 포인트 로그 조회
     */
    @GetMapping("/users/{userId}/recent")
    public ResponseEntity<List<PointLogResponse>> getRecentPointLogsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        List<PointLogResponse> pointLogs = pointLogService.getRecentPointLogsByUserId(userId, limit);
        return ResponseEntity.ok(pointLogs);
    }

    /**
     * 특정 사용자의 포인트 로그 수 조회
     */
    @GetMapping("/users/{userId}/count")
    public ResponseEntity<Long> getPointLogCountByUserId(@PathVariable Long userId) {
        Long count = pointLogService.getPointLogCountByUserId(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * 포인트 로그 삭제 (관리자용)
     */
    @DeleteMapping("/{pointLogId}")
    public ResponseEntity<Void> deletePointLog(@PathVariable Long pointLogId) {
        pointLogService.deletePointLog(pointLogId);
        return ResponseEntity.noContent().build();
    }
}
