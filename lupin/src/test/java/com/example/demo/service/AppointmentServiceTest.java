package com.example.demo.service;
import com.example.demo.domain.enums.Role;
import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.ChatMessage;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.dto.request.AppointmentRequest;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

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

    @Mock
    private ChatService chatService;

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
                .role(Role.MEMBER)
                .build();

        doctor = User.builder()
                .id(21L)
                .userId("doctor01")
                .name("의사1")
                .role(Role.DOCTOR)
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
    @DisplayName("예약 생성 시 채팅방이 자동으로 생성됨")
    void createAppointment_ShouldCreateChatRoomAutomatically() {
        // Given
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(21L)
                .date(LocalDateTime.of(2025, 12, 10, 15, 0))
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(21L)).willReturn(Optional.of(doctor));
        given(appointmentRepository.existsByDoctorIdAndDate(anyLong(), any())).willReturn(false);
        given(appointmentRepository.existsByPatientIdAndDate(anyLong(), any())).willReturn(false);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(appointment);
        given(chatService.createChatRoomForAppointment(anyLong())).willReturn("appointment_1");

        // When
        Long appointmentId = appointmentService.createAppointment(request);

        // Then
        assertThat(appointmentId).isNotNull();
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
        verify(chatService, times(1)).createChatRoomForAppointment(anyLong());
        // 환영 메시지 전송 기능 제거로 인해 saveMessage 검증 제거
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

    @Test
    @DisplayName("환자 ID로 예약 목록 조회")
    void getPatientAppointments_ShouldReturnAppointmentList() {
        // Given
        Long patientId = 1L;
        Appointment appointment2 = Appointment.builder()
                .id(2L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 5, 10, 0))
                .status(AppointmentStatus.COMPLETED)
                .build();

        List<Appointment> expectedAppointments = List.of(appointment2, appointment);
        given(appointmentRepository.findByPatientIdOrderByDateDesc(patientId))
                .willReturn(expectedAppointments);

        // When
        List<Appointment> result = appointmentService.getPatientAppointments(patientId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2L);
        assertThat(result.get(1).getId()).isEqualTo(1L);
        verify(appointmentRepository, times(1)).findByPatientIdOrderByDateDesc(patientId);
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
}
