package com.example.demo.controller;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.response.NotificationResponse;
import com.example.demo.security.CurrentUser;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @CurrentUser User user
    ) {
        List<Notification> notifications = notificationService.getNotifications(user);
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread")
    public ResponseEntity<Map<String, Boolean>> hasUnreadNotifications(
            @CurrentUser User user
    ) {
        boolean hasUnread = notificationService.hasUnreadNotifications(user);
        return ResponseEntity.ok(Map.of("hasUnread", hasUnread));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Boolean>> markAllAsRead(
            @CurrentUser User user
    ) {
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok().build();
    }

}
