package com.example.demo.service;
import com.example.demo.domain.enums.Role;
import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.ChatMessage;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.dto.request.AppointmentRequest;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private RLock rLock;

    @InjectMocks
    private AppointmentService appointmentService;

    private User patient;
    private User doctor;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        // 트랜잭션 동기화 활성화 (TransactionSynchronizationManager 사용을 위해)
        TransactionSynchronizationManager.initSynchronization();

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

    @AfterEach
    void tearDown() {
        // 트랜잭션 동기화 정리
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("예약 생성 시 채팅방이 자동으로 생성됨")
    void createAppointment_ShouldCreateChatRoomAutomatically() throws InterruptedException {
        // Given
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(21L)
                .date(LocalDateTime.of(2025, 12, 10, 15, 0))
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(21L)).willReturn(Optional.of(doctor));
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        given(appointmentRepository.existsByDoctorIdAndDate(anyLong(), any())).willReturn(false);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(appointment);
        given(chatService.createChatRoomForAppointment(anyLong())).willReturn("appointment_1");

        // When
        Long appointmentId = appointmentService.createAppointment(request);

        // Then
        assertThat(appointmentId).isNotNull();
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
        verify(chatService, times(1)).createChatRoomForAppointment(anyLong());
        verify(rLock, times(1)).unlock();
        // Redis 캐시 무효화는 트랜잭션 커밋 후 실행되므로 단위 테스트에서는 검증하지 않음
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
}
