package com.example.demo.domain.entity;

import com.example.demo.domain.enums.AppointmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Appointment 엔티티 테스트")
class AppointmentTest {

    @Test
    @DisplayName("기본 상태는 SCHEDULED다")
    void defaultStatusIsScheduledTest() {
        // given & when
        Appointment appointment = Appointment.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDateTime.now())
                .build();

        // then
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("진료 시작 시 상태가 IN_PROGRESS로 변경된다")
    void startConsultationTest() {
        // given
        Appointment appointment = Appointment.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDateTime.now())
                .status(AppointmentStatus.SCHEDULED)
                .build();

        // when
        appointment.startConsultation();

        // then
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("진료 완료 시 상태가 COMPLETED로 변경된다")
    void completeTest() {
        // given
        Appointment appointment = Appointment.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDateTime.now())
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        // when
        appointment.complete();

        // then
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("예약 취소 시 상태가 CANCELLED로 변경된다")
    void cancelTest() {
        // given
        Appointment appointment = Appointment.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDateTime.now())
                .status(AppointmentStatus.SCHEDULED)
                .build();

        // when
        appointment.cancel();

        // then
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("취소된 예약은 시작할 수 없다")
    void cannotStartCancelledAppointmentTest() {
        // given
        Appointment appointment = Appointment.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDateTime.now())
                .status(AppointmentStatus.CANCELLED)
                .build();

        // when & then
        assertThatThrownBy(appointment::startConsultation)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("취소된 예약은 시작할 수 없습니다.");
    }

    @Test
    @DisplayName("이미 진행 중인 예약은 다시 시작할 수 없다")
    void cannotRestartInProgressAppointmentTest() {
        // given
        Appointment appointment = Appointment.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDateTime.now())
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        // when & then
        assertThatThrownBy(appointment::startConsultation)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 진행 중인 예약입니다.");
    }

    @Test
    @DisplayName("이미 완료된 예약은 시작할 수 없다")
    void cannotStartCompletedAppointmentTest() {
        // given
        Appointment appointment = Appointment.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDateTime.now())
                .status(AppointmentStatus.COMPLETED)
                .build();

        // when & then
        assertThatThrownBy(appointment::startConsultation)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 완료된 예약입니다.");
    }

    @Test
    @DisplayName("취소된 예약은 완료할 수 없다")
    void cannotCompleteCancelledAppointmentTest() {
        // given
        Appointment appointment = Appointment.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDateTime.now())
                .status(AppointmentStatus.CANCELLED)
                .build();

        // when & then
        assertThatThrownBy(appointment::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("취소된 예약은 완료할 수 없습니다.");
    }

    @Test
    @DisplayName("이미 완료된 예약은 다시 완료할 수 없다")
    void cannotRecompleteAppointmentTest() {
        // given
        Appointment appointment = Appointment.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDateTime.now())
                .status(AppointmentStatus.COMPLETED)
                .build();

        // when & then
        assertThatThrownBy(appointment::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 완료된 예약입니다.");
    }

    @Test
    @DisplayName("완료된 예약은 취소할 수 없다")
    void cannotCancelCompletedAppointmentTest() {
        // given
        Appointment appointment = Appointment.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDateTime.now())
                .status(AppointmentStatus.COMPLETED)
                .build();

        // when & then
        assertThatThrownBy(appointment::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("완료된 예약은 취소할 수 없습니다.");
    }

    @Test
    @DisplayName("이미 취소된 예약은 다시 취소할 수 없다")
    void cannotRecancelAppointmentTest() {
        // given
        Appointment appointment = Appointment.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDateTime.now())
                .status(AppointmentStatus.CANCELLED)
                .build();

        // when & then
        assertThatThrownBy(appointment::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 취소된 예약입니다.");
    }

    @Test
    @DisplayName("SCHEDULED에서 COMPLETED로 바로 변경 가능하다")
    void canCompleteFromScheduledTest() {
        // given
        Appointment appointment = Appointment.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDateTime.now())
                .status(AppointmentStatus.SCHEDULED)
                .build();

        // when
        appointment.complete();

        // then
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }
}
