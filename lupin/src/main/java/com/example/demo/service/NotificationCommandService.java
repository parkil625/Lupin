package com.example.demo.service;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 명령 서비스 - CQRS 패턴
 * 알림의 상태를 변경하거나 삭제하는 쓰기 작업을 담당
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;

    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
    }

    public void markAllAsRead(User user) {
        // [최적화] 벌크 업데이트 - 개별 엔티티 로딩 없이 한 번에 처리
        // user.getId()만 사용하여 detached entity (@Version null) 문제 방지
        notificationRepository.markAllAsReadByUserId(user.getId());
    }

    public void deleteNotification(Long notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notificationRepository.deleteById(notificationId);
    }
}
