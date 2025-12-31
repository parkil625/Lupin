package com.example.demo.util;

import com.example.demo.domain.enums.AppointmentStatus;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 예약 시간 관련 유틸리티 클래스
 */
public class AppointmentTimeUtils {

    private static final long SHOW_TIME_THRESHOLD_MINUTES = 360;


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
    public static boolean isChatAvailable(LocalDateTime appointmentTime, AppointmentStatus status) {
        // 이미 진료 중이거나 완료된 경우
        if (status == AppointmentStatus.IN_PROGRESS) {
            return true;
        }

        // 예약 예정 상태이면서 5분 전부터 입장 가능
        if (status == AppointmentStatus.SCHEDULED) {
            long minutesUntil = getMinutesUntilAppointment(appointmentTime);
            return minutesUntil <= 5 && minutesUntil >= 0;
        }

        return false;
    }

    /**
     * 채팅 잠금 메시지 생성
     */
    public static String getChatLockMessage(LocalDateTime appointmentTime, AppointmentStatus status) {

        if (isChatAvailable(appointmentTime, status)) {
            return null;
        }

        if (status == AppointmentStatus.COMPLETED){
            return "진료가 완료되었습니다.";
        }

        if (status == AppointmentStatus.CANCELLED){
            return "취소된 진료입니다.";
        }

        long minutesUntil = getMinutesUntilAppointment(appointmentTime);

        if (minutesUntil > SHOW_TIME_THRESHOLD_MINUTES) {
            return "진료 시간이 아닙니다. 진료 시간 5분 전부터 채팅이 가능합니다.";
        }


        if (minutesUntil > 0) {
            long hours = minutesUntil / 60;
            long minutes = minutesUntil % 60;

            if(hours > 0) {
                return String.format("진료 시간이 %d시간 %d분 남았습니다. 진료 5분 전부터 채팅이 가능합니다.", hours, minutes);
            }else{
                return String.format("진료 시간이 %d분 남았습니다. 진료 5분 전부터 채팅이 가능합니다.", minutes);
            }
        }

        return "진료 대기 중입니다. 잠시만 기다려주세요.";
    }
}
