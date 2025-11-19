package com.example.demo.controller;

import com.example.demo.dto.request.NotificationCreateRequest;
import com.example.demo.dto.response.NotificationResponse;
import com.example.demo.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림 관련 API
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 생성 (관리자용 또는 시스템 알림)
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody NotificationCreateRequest request) {
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 사용자의 알림 목록 조회 (페이징)
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationResponse> notifications = notificationService.getNotificationsByUserId(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 특정 사용자의 알림 목록 조회 (전체)
     */
    @GetMapping("/users/{userId}/all")
    public ResponseEntity<List<NotificationResponse>> getAllNotificationsByUserId(@PathVariable Long userId) {
        List<NotificationResponse> notifications = notificationService.getAllNotificationsByUserId(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 특정 사용자의 읽지 않은 알림 조회
     */
    @GetMapping("/users/{userId}/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotificationsByUserId(@PathVariable Long userId) {
        List<NotificationResponse> notifications = notificationService.getUnreadNotificationsByUserId(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 특정 사용자의 읽지 않은 알림 수 조회
     */
    @GetMapping("/users/{userId}/unread/count")
    public ResponseEntity<Long> getUnreadCountByUserId(@PathVariable Long userId) {
        Long count = notificationService.getUnreadCountByUserId(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * 알림 상세 조회
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponse> getNotificationDetail(@PathVariable Long notificationId) {
        NotificationResponse notification = notificationService.getNotificationDetail(notificationId);
        return ResponseEntity.ok(notification);
    }

    /**
     * 알림 읽음 처리
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long notificationId,
            @RequestParam Long userId) {
        NotificationResponse response = notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 사용자의 알림 전체 읽음 처리
     */
    @PatchMapping("/users/{userId}/read-all")
    public ResponseEntity<Void> markAllAsReadByUserId(@PathVariable Long userId) {
        notificationService.markAllAsReadByUserId(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 알림 삭제
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long notificationId,
            @RequestParam Long userId) {
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 특정 타입의 알림 조회
     */
    @GetMapping("/users/{userId}/type/{type}")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsByType(
            @PathVariable Long userId,
            @PathVariable String type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationResponse> notifications = notificationService.getNotificationsByType(userId, type, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 오래된 읽은 알림 삭제 (30일 이상)
     */
    @DeleteMapping("/users/{userId}/cleanup")
    public ResponseEntity<Void> deleteOldReadNotifications(@PathVariable Long userId) {
        notificationService.deleteOldReadNotifications(userId);
        return ResponseEntity.ok().build();
    }
}
