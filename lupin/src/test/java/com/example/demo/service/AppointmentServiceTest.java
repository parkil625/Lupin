package com.example.demo.service;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * AppointmentService TDD 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService 테스트")
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private User patient;
    private User doctor;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        patient = User.builder()
                .id(1L)
                .userId("patient01")
                .name("환자1")
                .build();

        doctor = User.builder()
                .id(21L)
                .userId("doctor01")
                .name("의사1")
                .build();

        appointment = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("진료 시작 시 예약 상태 'IN_PROGRESS'로 변경")
    void startConsultation_ShouldChangeStatusToInProgress() {
        // Given
        Long appointmentId = 1L;
        given(appointmentRepository.findById(appointmentId))
                .willReturn(Optional.of(appointment));

        // When
        appointmentService.startConsultation(appointmentId);

        // Then
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
        verify(appointmentRepository, times(1)).findById(appointmentId);
    }

    @Test
    @DisplayName("진료 완료 시 예약 상태 'COMPLETED'로 변경")
    void completeConsultation_ShouldChangeStatusToCompleted() {
        // Given
        Long appointmentId = 1L;
        Appointment inProgressAppointment = Appointment.builder()
                .id(appointmentId)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        given(appointmentRepository.findById(appointmentId))
                .willReturn(Optional.of(inProgressAppointment));

        // When
        appointmentService.completeConsultation(appointmentId);

        // Then
        assertThat(inProgressAppointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(appointmentRepository, times(1)).findById(appointmentId);
    }
}
