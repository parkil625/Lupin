package com.example.demo.scheduler;

import com.example.demo.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationScheduler 테스트")
class NotificationSchedulerTest {

    @InjectMocks
    private NotificationScheduler notificationScheduler;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("오래된 알림 삭제 성공")
    void deleteOldNotifications_Success() {
        // given
        given(notificationRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).willReturn(10);

        // when
        notificationScheduler.deleteOldNotifications();

        // then
        then(notificationRepository).should().deleteByCreatedAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("오래된 알림 삭제 중 예외 발생")
    void deleteOldNotifications_Exception() {
        // given
        given(notificationRepository.deleteByCreatedAtBefore(any(LocalDateTime.class)))
                .willThrow(new RuntimeException("DB 오류"));

        // when
        notificationScheduler.deleteOldNotifications();

        // then - 예외가 발생해도 스케줄러는 정상 종료
        then(notificationRepository).should().deleteByCreatedAtBefore(any(LocalDateTime.class));
    }
}
