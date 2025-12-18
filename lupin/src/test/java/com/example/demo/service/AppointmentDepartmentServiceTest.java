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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.junit.jupiter.api.AfterEach;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RLock rLock;

    @InjectMocks
    private AppointmentService appointmentService;

    private User patient;

    @BeforeEach
    void setUp() throws InterruptedException {
        // 트랜잭션 동기화 활성화
        TransactionSynchronizationManager.initSynchronization();

        patient = User.builder()
                .id(1L)
                .userId("patient01")
                .name("환자1")
                .role(Role.MEMBER)
                .build();

        // Redis mock 공통 설정
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
    }

    @AfterEach
    void tearDown() {
        // 트랜잭션 동기화 정리
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
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
                .department("내과")
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
                .department("외과")
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
                .department("신경정신과")
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
                .department("피부과")
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

    @Test
    @DisplayName("같은 시간에 다른 진료과 예약이 가능해야 한다")
    void createAppointment_ShouldAllowMultipleDepartmentsAtSameTime() {
        // Given
        User internalDoctor = User.builder()
                .id(21L)
                .userId("doctor01")
                .name("내과의사")
                .role(Role.DOCTOR)
                .department("내과")
                .build();

        User surgeonDoctor = User.builder()
                .id(22L)
                .userId("doctor02")
                .name("외과의사")
                .role(Role.DOCTOR)
                .department("외과")
                .build();

        LocalDateTime sameTime = LocalDateTime.of(2025, 12, 15, 10, 0);

        // 내과 예약 요청
        AppointmentRequest internalRequest = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(21L)
                .date(sameTime)
                .build();

        // 외과 예약 요청 (같은 시간)
        AppointmentRequest surgeryRequest = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(22L)
                .date(sameTime)
                .build();

        Appointment internalAppointment = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(internalDoctor)
                .date(sameTime)
                .status(AppointmentStatus.SCHEDULED)
                .departmentName("내과")
                .build();

        Appointment surgeryAppointment = Appointment.builder()
                .id(2L)
                .patient(patient)
                .doctor(surgeonDoctor)
                .date(sameTime)
                .status(AppointmentStatus.SCHEDULED)
                .departmentName("외과")
                .build();

        // 내과 예약 mock
        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(21L)).willReturn(Optional.of(internalDoctor));
        given(appointmentRepository.existsByDoctorIdAndDate(21L, sameTime)).willReturn(false);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(internalAppointment);
        given(chatService.createChatRoomForAppointment(anyLong())).willReturn("appointment_1");

        // When - 내과 예약
        Long internalAppointmentId = appointmentService.createAppointment(internalRequest);

        // 외과 예약 mock
        given(userRepository.findById(22L)).willReturn(Optional.of(surgeonDoctor));
        given(appointmentRepository.existsByDoctorIdAndDate(22L, sameTime)).willReturn(false);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(surgeryAppointment);
        given(chatService.createChatRoomForAppointment(anyLong())).willReturn("appointment_2");

        // When - 외과 예약 (같은 시간, 다른 진료과)
        Long surgeryAppointmentId = appointmentService.createAppointment(surgeryRequest);

        // Then - 둘 다 성공해야 함
        assertThat(internalAppointmentId).isNotNull();
        assertThat(surgeryAppointmentId).isNotNull();
        verify(appointmentRepository, times(2)).save(any(Appointment.class));
    }
}
