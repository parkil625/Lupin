package com.example.demo.util;

import com.example.demo.domain.enums.AppointmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentTimeUtilsTest {

    @Test
    @DisplayName("IN_PROGRESS 상태일 때 채팅 가능해야 함")
    void shouldAllowChatWhenStatusIsInProgress() {
        // given
        AppointmentStatus status = AppointmentStatus.IN_PROGRESS;

        // when
        boolean result = AppointmentTimeUtils.isChatAvailable(status);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("SCHEDULED 상태일 때 채팅 불가능해야 함")
    void shouldNotAllowChatWhenStatusIsScheduled() {
        // given
        AppointmentStatus status = AppointmentStatus.SCHEDULED;

        // when
        boolean result = AppointmentTimeUtils.isChatAvailable(status);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("CANCELLED 상태일 때 채팅 불가능해야 함")
    void shouldNotAllowChatWhenStatusIsCancelled() {
        // given
        AppointmentStatus status = AppointmentStatus.CANCELLED;

        // when
        boolean result = AppointmentTimeUtils.isChatAvailable(status);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("COMPLETED 상태일 때 채팅 불가능해야 함")
    void shouldNotAllowChatWhenStatusIsCompleted() {
        // given
        AppointmentStatus status = AppointmentStatus.COMPLETED;

        // when
        boolean result = AppointmentTimeUtils.isChatAvailable(status);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("IN_PROGRESS 상태일 때 잠금 메시지가 null이어야 함")
    void shouldReturnNullLockMessageWhenStatusIsInProgress() {
        // given
        LocalDateTime appointmentTime = LocalDateTime.now().plusMinutes(10);
        AppointmentStatus status = AppointmentStatus.IN_PROGRESS;

        // when
        String message = AppointmentTimeUtils.getChatLockMessage(appointmentTime, status);

        // then
        assertThat(message).isNull();
    }

    @Test
    @DisplayName("COMPLETED 상태일 때 완료 메시지를 반환해야 함")
    void shouldReturnCompletedMessageWhenStatusIsCompleted() {
        // given
        LocalDateTime appointmentTime = LocalDateTime.now().plusMinutes(10);
        AppointmentStatus status = AppointmentStatus.COMPLETED;

        // when
        String message = AppointmentTimeUtils.getChatLockMessage(appointmentTime, status);

        // then
        assertThat(message).isEqualTo("진료가 완료되었습니다.");
    }

    @Test
    @DisplayName("CANCELLED 상태일 때 취소 메시지를 반환해야 함")
    void shouldReturnCancelledMessageWhenStatusIsCancelled() {
        // given
        LocalDateTime appointmentTime = LocalDateTime.now().plusMinutes(10);
        AppointmentStatus status = AppointmentStatus.CANCELLED;

        // when
        String message = AppointmentTimeUtils.getChatLockMessage(appointmentTime, status);

        // then
        assertThat(message).isEqualTo("취소된 진료입니다.");
    }

    @Test
    @DisplayName("예약 시간이 6시간 이상 남았을 때 기본 메시지를 반환해야 함")
    void shouldReturnDefaultMessageWhenMoreThan6HoursRemain() {
        // given
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(7);
        AppointmentStatus status = AppointmentStatus.SCHEDULED;

        // when
        String message = AppointmentTimeUtils.getChatLockMessage(appointmentTime, status);

        // then
        assertThat(message).isEqualTo("진료 시간이 아닙니다. 진료 시간 5분 전부터 채팅이 가능합니다.");
    }

    @Test
    @DisplayName("예약 시간이 1시간 30분 남았을 때 시간과 분을 표시해야 함")
    void shouldShowHoursAndMinutesWhenRemaining() {
        // given
        LocalDateTime appointmentTime = LocalDateTime.now().plusHours(1).plusMinutes(30);
        AppointmentStatus status = AppointmentStatus.SCHEDULED;

        // when
        String message = AppointmentTimeUtils.getChatLockMessage(appointmentTime, status);

        // then
        assertThat(message).contains("1시간");
        assertThat(message).contains("분 남았습니다");
    }

    @Test
    @DisplayName("예약 시간이 30분 남았을 때 분만 표시해야 함")
    void shouldShowOnlyMinutesWhenLessThanOneHour() {
        // given
        LocalDateTime appointmentTime = LocalDateTime.now().plusMinutes(30);
        AppointmentStatus status = AppointmentStatus.SCHEDULED;

        // when
        String message = AppointmentTimeUtils.getChatLockMessage(appointmentTime, status);

        // then
        assertThat(message).contains("분 남았습니다");
        // "N시간 M분" 형태가 아닌지 확인 (시간 단위는 나타나지 않아야 함)
        assertThat(message).doesNotContainPattern("\\d+시간");
    }

    @Test
    @DisplayName("예약 시간까지 남은 시간을 정확히 계산해야 함")
    void shouldCalculateMinutesUntilAppointment() {
        // given
        LocalDateTime appointmentTime = LocalDateTime.now().plusMinutes(45);

        // when
        long minutes = AppointmentTimeUtils.getMinutesUntilAppointment(appointmentTime);

        // then
        assertThat(minutes).isBetween(44L, 46L); // 실행 시간 오차 고려
    }
}
