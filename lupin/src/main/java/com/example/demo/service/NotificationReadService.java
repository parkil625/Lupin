package com.example.demo.service;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 조회 서비스 - CQRS 패턴
 * 읽기 작업을 분리하여 조회 성능 최적화 및 책임 분리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationReadService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getNotifications(User user) {
        // user.getId()만 사용하여 detached entity (@Version null) 문제 방지
        return notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId());
    }

    public boolean hasUnreadNotifications(User user) {
        // user.getId()만 사용하여 detached entity (@Version null) 문제 방지
        return notificationRepository.existsByUserIdAndIsReadFalse(user.getId());
    }
}
