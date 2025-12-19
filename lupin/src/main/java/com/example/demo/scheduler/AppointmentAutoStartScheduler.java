package com.example.demo.scheduler;

import com.example.demo.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentAutoStartScheduler {

    private final AppointmentRepository appointmentRepository;

    /**
     * 1분마다 실행되어 진료 시작 5분 전인 예약들의 상태를 자동으로 '진료 중(IN_PROGRESS)'으로 변경
     */
    @Scheduled(cron = "0 * * * * *") 
    @Transactional
    public void autoStartAppointments() {
        // 현재 시간 + 5분 (이 시간보다 이전인 예약은 모두 시작 처리)
        LocalDateTime threshold = LocalDateTime.now().plusMinutes(5);

        int updatedCount = appointmentRepository.bulkUpdateStatusToInProgress(threshold);
        
        if (updatedCount > 0) {
            log.info("진료 자동 시작 처리 완료: {}건 (기준 시각: {})", updatedCount, threshold);
        }
    }
}