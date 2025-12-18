package com.example.demo.scheduler;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.domain.enums.NotificationType;
import com.example.demo.event.NotificationEvent;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 리마인더 스케줄러
 * - 예약 시간 5분 전에 환자와 의사에게 알림 발송
 * - 매 1분마다 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 예약 리마인더 알림 발송
     * - 5분 전에 알림 발송
     * - 이미 알림을 보낸 예약은 제외
     */
    @Scheduled(cron = "0 * * * * *") // 매 1분마다 실행
    @Transactional
    public void sendAppointmentReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesLater = now.plusMinutes(5);

        // 5분 후에 시작되는 SCHEDULED 상태의 예약 조회
        LocalDateTime startTime = fiveMinutesLater.minusSeconds(30); // 5분 ± 30초
        LocalDateTime endTime = fiveMinutesLater.plusSeconds(30);

        List<Appointment> upcomingAppointments = appointmentRepository
                .findByStatusAndDateBetween(AppointmentStatus.SCHEDULED, startTime, endTime);

        log.info("예약 리마인더 체크: {} ~ {}, 발견된 예약: {}개",
                startTime, endTime, upcomingAppointments.size());

        for (Appointment appointment : upcomingAppointments) {
            // 이미 리마인더 알림을 보냈는지 확인
            boolean alreadySent = notificationRepository.existsByUserIdAndTypeAndRefId(
                    appointment.getPatient().getId(),
                    NotificationType.APPOINTMENT_REMINDER,
                    String.valueOf(appointment.getId())
            );

            if (!alreadySent) {
                sendReminderNotifications(appointment);
            }
        }
    }

    /**
     * 환자와 의사에게 리마인더 알림 발송
     */
    private void sendReminderNotifications(Appointment appointment) {
        String refId = String.valueOf(appointment.getId());

        // 환자에게 알림
        Notification patientNotification = Notification.builder()
                .user(appointment.getPatient())
                .type(NotificationType.APPOINTMENT_REMINDER)
                .title("진료 예약 알림")
                .content(String.format("%s 진료 예약이 5분 후 시작됩니다. 채팅으로 대기해주세요.",
                        appointment.getDepartmentName()))
                .refId(refId)
                .targetId(null)
                .isRead(false)
                .build();

        notificationRepository.save(patientNotification);

        // 의사에게 알림
        Notification doctorNotification = Notification.builder()
                .user(appointment.getDoctor())
                .type(NotificationType.APPOINTMENT_REMINDER)
                .title("진료 예약 알림")
                .content(String.format("%s 환자의 진료 예약이 5분 후 시작됩니다.",
                        appointment.getPatient().getName()))
                .refId(refId)
                .targetId(null)
                .isRead(false)
                .build();

        notificationRepository.save(doctorNotification);

        log.info("예약 리마인더 발송 완료 - 예약 ID: {}, 환자: {}, 의사: {}",
                appointment.getId(),
                appointment.getPatient().getName(),
                appointment.getDoctor().getName());
    }
}
