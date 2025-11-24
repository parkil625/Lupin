package com.example.demo.domain.entity;

import com.example.demo.domain.enums.AppointmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Appointment 엔티티 테스트")
class AppointmentTest {

    @Test
    @DisplayName("예약 완료 성공")
    void complete_Success() {
        // given
        Appointment appointment = Appointment.builder()
                .id(1L)
                .apptDate(LocalDateTime.now())
                .status(AppointmentStatus.SCHEDULED)
                .reason("정기 검진")
                .build();

        // when
        appointment.complete();

        // then
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("예약 완료 실패 - 이미 취소됨")
    void complete_AlreadyCancelled_ThrowsException() {
        // given
        Appointment appointment = Appointment.builder()
                .id(1L)
                .apptDate(LocalDateTime.now())
                .status(AppointmentStatus.CANCELLED)
                .reason("정기 검진")
                .build();

        // when & then
        assertThatThrownBy(() -> appointment.complete())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("취소된 예약");
    }

    @Test
    @DisplayName("예약 취소 성공")
    void cancel_Success() {
        // given
        Appointment appointment = Appointment.builder()
                .id(1L)
                .apptDate(LocalDateTime.now())
                .status(AppointmentStatus.SCHEDULED)
                .reason("정기 검진")
                .build();

        // when
        appointment.cancel();

        // then
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("예약 취소 실패 - 이미 완료됨")
    void cancel_AlreadyCompleted_ThrowsException() {
        // given
        Appointment appointment = Appointment.builder()
                .id(1L)
                .apptDate(LocalDateTime.now())
                .status(AppointmentStatus.COMPLETED)
                .reason("정기 검진")
                .build();

        // when & then
        assertThatThrownBy(() -> appointment.cancel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("완료된 예약");
    }

    @Test
    @DisplayName("예약 생성 - 기본 상태")
    void create_DefaultStatus() {
        // given & when
        Appointment appointment = Appointment.builder()
                .id(1L)
                .apptDate(LocalDateTime.now())
                .reason("상담")
                .build();

        // then
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }
}
