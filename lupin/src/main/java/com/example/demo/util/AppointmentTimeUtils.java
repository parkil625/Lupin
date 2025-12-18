package com.example.demo.util;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 예약 시간 관련 유틸리티 클래스
 */
public class AppointmentTimeUtils {

    private static final long CHAT_UNLOCK_MINUTES = 5;

    /**
     * 예약 시간까지 남은 시간(분) 계산
     */
    public static long getMinutesUntilAppointment(LocalDateTime appointmentTime) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now, appointmentTime);
        return duration.toMinutes();
    }

    /**
     * 채팅 가능 여부 확인 (예약 시간 5분 전부터 가능)
     */
    public static boolean isChatAvailable(LocalDateTime appointmentTime) {
        long minutesUntil = getMinutesUntilAppointment(appointmentTime);
        return minutesUntil <= CHAT_UNLOCK_MINUTES;
    }

    /**
     * 채팅 잠금 메시지 생성
     */
    public static String getChatLockMessage(LocalDateTime appointmentTime) {
        long minutesUntil = getMinutesUntilAppointment(appointmentTime);

        if (minutesUntil < 0) {
            return "진료 시간이 지났습니다.";
        }

        return String.format("진료 시간이 아닙니다. %d분 후에 채팅이 가능합니다.", minutesUntil);
    }
}
