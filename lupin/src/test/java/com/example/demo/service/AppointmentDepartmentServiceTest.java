package com.example.demo.service;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.domain.enums.Role;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * AppointmentService - 진료과 관련 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService - 진료과 테스트")
class AppointmentDepartmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private AppointmentService appointmentService;

    private User patient;

    @BeforeEach
    void setUp() {
        patient = User.builder()
                .id(1L)
                .userId("patient01")
                .name("환자1")
                .role(Role.MEMBER)
                .build();
    }

    @Test
    @DisplayName("예약 생성 시 진료과명이 의사의 부서명으로 설정되어야 한다")
    void createAppointment_ShouldSetDepartmentNameFromDoctor() {
        // Given
        User doctorWithDepartment = User.builder()
                .id(21L)
                .userId("doctor01")
                .name("의사1")
                .role(Role.DOCTOR)
                .departement("내과")
                .build();

        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(21L)
                .date(LocalDateTime.of(2025, 12, 10, 15, 0))
                .build();

        Appointment expectedAppointment = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctorWithDepartment)
                .date(request.getDate())
                .status(AppointmentStatus.SCHEDULED)
                .departmentName("내과")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(21L)).willReturn(Optional.of(doctorWithDepartment));
        given(appointmentRepository.existsByDoctorIdAndDate(anyLong(), any())).willReturn(false);
        given(appointmentRepository.existsByPatientIdAndDate(anyLong(), any())).willReturn(false);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(expectedAppointment);
        given(chatService.createChatRoomForAppointment(anyLong())).willReturn("appointment_1");

        // When
        Long appointmentId = appointmentService.createAppointment(request);

        // Then
        assertThat(appointmentId).isNotNull();
        verify(appointmentRepository, times(1)).save(argThat(apt ->
            apt.getDepartmentName() != null &&
            apt.getDepartmentName().equals("내과")
        ));
    }

    @Test
    @DisplayName("외과 의사로 예약 생성 시 진료과명이 '외과'로 설정되어야 한다")
    void createAppointment_ShouldSetDepartmentNameAsSurgery() {
        // Given
        User surgeonDoctor = User.builder()
                .id(22L)
                .userId("doctor02")
                .name("외과의사")
                .role(Role.DOCTOR)
                .departement("외과")
                .build();

        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(22L)
                .date(LocalDateTime.of(2025, 12, 11, 10, 0))
                .build();

        Appointment expectedAppointment = Appointment.builder()
                .id(2L)
                .patient(patient)
                .doctor(surgeonDoctor)
                .date(request.getDate())
                .status(AppointmentStatus.SCHEDULED)
                .departmentName("외과")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(22L)).willReturn(Optional.of(surgeonDoctor));
        given(appointmentRepository.existsByDoctorIdAndDate(anyLong(), any())).willReturn(false);
        given(appointmentRepository.existsByPatientIdAndDate(anyLong(), any())).willReturn(false);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(expectedAppointment);
        given(chatService.createChatRoomForAppointment(anyLong())).willReturn("appointment_2");

        // When
        Long appointmentId = appointmentService.createAppointment(request);

        // Then
        assertThat(appointmentId).isNotNull();
        verify(appointmentRepository, times(1)).save(argThat(apt ->
            apt.getDepartmentName() != null &&
            apt.getDepartmentName().equals("외과")
        ));
    }

    @Test
    @DisplayName("신경정신과 의사로 예약 생성 시 진료과명이 '신경정신과'로 설정되어야 한다")
    void createAppointment_ShouldSetDepartmentNameAsPsychiatry() {
        // Given
        User psychiatryDoctor = User.builder()
                .id(23L)
                .userId("doctor03")
                .name("정신과의사")
                .role(Role.DOCTOR)
                .departement("신경정신과")
                .build();

        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(23L)
                .date(LocalDateTime.of(2025, 12, 12, 14, 0))
                .build();

        Appointment expectedAppointment = Appointment.builder()
                .id(3L)
                .patient(patient)
                .doctor(psychiatryDoctor)
                .date(request.getDate())
                .status(AppointmentStatus.SCHEDULED)
                .departmentName("신경정신과")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(23L)).willReturn(Optional.of(psychiatryDoctor));
        given(appointmentRepository.existsByDoctorIdAndDate(anyLong(), any())).willReturn(false);
        given(appointmentRepository.existsByPatientIdAndDate(anyLong(), any())).willReturn(false);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(expectedAppointment);
        given(chatService.createChatRoomForAppointment(anyLong())).willReturn("appointment_3");

        // When
        Long appointmentId = appointmentService.createAppointment(request);

        // Then
        assertThat(appointmentId).isNotNull();
        verify(appointmentRepository, times(1)).save(argThat(apt ->
            apt.getDepartmentName() != null &&
            apt.getDepartmentName().equals("신경정신과")
        ));
    }

    @Test
    @DisplayName("피부과 의사로 예약 생성 시 진료과명이 '피부과'로 설정되어야 한다")
    void createAppointment_ShouldSetDepartmentNameAsDermatology() {
        // Given
        User dermatologyDoctor = User.builder()
                .id(24L)
                .userId("doctor04")
                .name("피부과의사")
                .role(Role.DOCTOR)
                .departement("피부과")
                .build();

        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(24L)
                .date(LocalDateTime.of(2025, 12, 13, 16, 0))
                .build();

        Appointment expectedAppointment = Appointment.builder()
                .id(4L)
                .patient(patient)
                .doctor(dermatologyDoctor)
                .date(request.getDate())
                .status(AppointmentStatus.SCHEDULED)
                .departmentName("피부과")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(24L)).willReturn(Optional.of(dermatologyDoctor));
        given(appointmentRepository.existsByDoctorIdAndDate(anyLong(), any())).willReturn(false);
        given(appointmentRepository.existsByPatientIdAndDate(anyLong(), any())).willReturn(false);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(expectedAppointment);
        given(chatService.createChatRoomForAppointment(anyLong())).willReturn("appointment_4");

        // When
        Long appointmentId = appointmentService.createAppointment(request);

        // Then
        assertThat(appointmentId).isNotNull();
        verify(appointmentRepository, times(1)).save(argThat(apt ->
            apt.getDepartmentName() != null &&
            apt.getDepartmentName().equals("피부과")
        ));
    }
}
