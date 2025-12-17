package com.example.demo.service;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.domain.enums.Role;
import com.example.demo.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * AppointmentService - 예약 시간 조회 관련 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService - 예약 시간 조회 테스트")
class AppointmentBookingServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private User patient;
    private User doctor;

    @BeforeEach
    void setUp() {
        patient = User.builder()
                .id(1L)
                .userId("patient01")
                .name("환자1")
                .role(Role.MEMBER)
                .build();

        doctor = User.builder()
                .id(21L)
                .userId("doctor01")
                .name("의사1")
                .role(Role.DOCTOR)
                .build();
    }

    @Test
    @DisplayName("특정 의사와 날짜의 예약된 시간 목록 조회")
    void getBookedTimesByDoctorAndDate_ShouldReturnBookedTimes() {
        // Given
        Long doctorId = 21L;
        java.time.LocalDate date = java.time.LocalDate.of(2025, 12, 16);

        // 해당 날짜의 예약들
        Appointment apt1 = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 16, 9, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment apt2 = Appointment.builder()
                .id(2L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 16, 14, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment apt3 = Appointment.builder()
                .id(3L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 16, 16, 0))
                .status(AppointmentStatus.CANCELLED) // 취소된 예약은 제외
                .build();

        List<Appointment> appointments = List.of(apt1, apt2, apt3);
        given(appointmentRepository.findByDoctorIdAndDateBetween(
                eq(doctorId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(appointments);

        // When
        List<String> bookedTimes = appointmentService.getBookedTimesByDoctorAndDate(doctorId, date);

        // Then
        assertThat(bookedTimes).hasSize(2);
        assertThat(bookedTimes).containsExactlyInAnyOrder("09:00", "14:00");
        verify(appointmentRepository, times(1)).findByDoctorIdAndDateBetween(
                eq(doctorId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("취소된 예약은 예약된 시간 목록에서 제외되어야 한다")
    void getBookedTimesByDoctorAndDate_ShouldExcludeCancelledAppointments() {
        // Given
        Long doctorId = 21L;
        java.time.LocalDate date = java.time.LocalDate.of(2025, 12, 18);

        Appointment cancelledApt = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 18, 9, 0))
                .status(AppointmentStatus.CANCELLED)
                .build();

        List<Appointment> appointments = List.of(cancelledApt);
        given(appointmentRepository.findByDoctorIdAndDateBetween(
                eq(doctorId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(appointments);

        // When
        List<String> bookedTimes = appointmentService.getBookedTimesByDoctorAndDate(doctorId, date);

        // Then
        assertThat(bookedTimes).isEmpty();
    }

    @Test
    @DisplayName("같은 날짜에 여러 예약이 있을 때 모든 시간이 반환되어야 한다")
    void getBookedTimesByDoctorAndDate_ShouldReturnAllBookedTimesForSameDate() {
        // Given
        Long doctorId = 21L;
        java.time.LocalDate date = java.time.LocalDate.of(2025, 12, 18);

        Appointment apt1 = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 18, 9, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment apt2 = Appointment.builder()
                .id(2L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 18, 10, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment apt3 = Appointment.builder()
                .id(3L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 18, 14, 0))
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        List<Appointment> appointments = List.of(apt1, apt2, apt3);
        given(appointmentRepository.findByDoctorIdAndDateBetween(
                eq(doctorId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(appointments);

        // When
        List<String> bookedTimes = appointmentService.getBookedTimesByDoctorAndDate(doctorId, date);

        // Then
        assertThat(bookedTimes).hasSize(3);
        assertThat(bookedTimes).containsExactlyInAnyOrder("09:00", "10:00", "14:00");
    }

    @Test
    @DisplayName("해당 날짜에 예약이 없으면 빈 목록이 반환되어야 한다")
    void getBookedTimesByDoctorAndDate_ShouldReturnEmptyListWhenNoAppointments() {
        // Given
        Long doctorId = 21L;
        java.time.LocalDate date = java.time.LocalDate.of(2025, 12, 20);

        given(appointmentRepository.findByDoctorIdAndDateBetween(
                eq(doctorId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(List.of());

        // When
        List<String> bookedTimes = appointmentService.getBookedTimesByDoctorAndDate(doctorId, date);

        // Then
        assertThat(bookedTimes).isEmpty();
    }
}
