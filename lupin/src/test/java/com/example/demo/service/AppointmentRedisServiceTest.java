package com.example.demo.service;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.AppointmentRequest;
import com.example.demo.exception.BusinessException;
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
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * AppointmentService - Redis 통합 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService - Redis 통합 테스트")
class AppointmentRedisServiceTest {

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

    @Mock
    private ListOperations<String, String> listOperations;

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
                .department("내과")
                .build();
    }

    @Test
    @DisplayName("Redis 분산 락을 사용하여 예약 생성 시 동시성 제어")
    void createAppointment_WithRedisLock_ShouldPreventConcurrentBooking() throws InterruptedException {
        // Given
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(21L)
                .date(LocalDateTime.of(2025, 12, 18, 9, 0))
                .build();

        Appointment expectedAppointment = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .date(request.getDate())
                .status(AppointmentStatus.SCHEDULED)
                .departmentName("내과")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(21L)).willReturn(Optional.of(doctor));
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        given(appointmentRepository.existsByDoctorIdAndDate(anyLong(), any())).willReturn(false);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(expectedAppointment);
        given(chatService.createChatRoomForAppointment(anyLong())).willReturn("appointment_1");

        // When
        Long appointmentId = appointmentService.createAppointment(request);

        // Then
        assertThat(appointmentId).isNotNull();
        verify(rLock, times(1)).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        verify(rLock, times(1)).unlock();
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Redis 락 획득 실패 시 예외 발생")
    void createAppointment_LockAcquisitionFailed_ShouldThrowException() throws InterruptedException {
        // Given
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(21L)
                .date(LocalDateTime.of(2025, 12, 18, 9, 0))
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(21L)).willReturn(Optional.of(doctor));
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("다른 사용자가 예약 중입니다");

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Redis 캐시에 예약 시간이 있으면 DB 조회 없이 반환")
    void getBookedTimesByDoctorAndDate_WithCache_ShouldReturnCachedData() {
        // Given
        Long doctorId = 21L;
        LocalDate date = LocalDate.of(2025, 12, 18);
        List<String> cachedTimes = List.of("09:00", "10:00", "11:00");

        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(listOperations.size(anyString())).willReturn(3L);
        given(listOperations.range(anyString(), eq(0L), eq(-1L))).willReturn(cachedTimes);

        // When
        List<String> result = appointmentService.getBookedTimesByDoctorAndDate(doctorId, date);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("09:00", "10:00", "11:00");
        verify(appointmentRepository, never()).findByDoctorIdAndDateBetween(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Redis 캐시 미스 시 DB 조회 후 캐시 저장")
    void getBookedTimesByDoctorAndDate_CacheMiss_ShouldQueryDbAndCache() {
        // Given
        Long doctorId = 21L;
        LocalDate date = LocalDate.of(2025, 12, 18);

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

        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(listOperations.size(anyString())).willReturn(0L);
        given(appointmentRepository.findByDoctorIdAndDateBetween(anyLong(), any(), any()))
                .willReturn(List.of(apt1, apt2));

        // When
        List<String> result = appointmentService.getBookedTimesByDoctorAndDate(doctorId, date);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("09:00", "10:00");
        verify(appointmentRepository, times(1)).findByDoctorIdAndDateBetween(anyLong(), any(), any());
        verify(redisTemplate, times(1)).delete(anyString());
        verify(listOperations, times(1)).rightPushAll(anyString(), anyList());
        verify(redisTemplate, times(1)).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("예약 취소된 항목은 예약 시간 목록에서 제외")
    void getBookedTimesByDoctorAndDate_ShouldExcludeCancelledAppointments() {
        // Given
        Long doctorId = 21L;
        LocalDate date = LocalDate.of(2025, 12, 18);

        Appointment scheduledApt = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 18, 9, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        Appointment cancelledApt = Appointment.builder()
                .id(2L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 18, 10, 0))
                .status(AppointmentStatus.CANCELLED)
                .build();

        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(listOperations.size(anyString())).willReturn(0L);
        given(appointmentRepository.findByDoctorIdAndDateBetween(anyLong(), any(), any()))
                .willReturn(List.of(scheduledApt, cancelledApt));

        // When
        List<String> result = appointmentService.getBookedTimesByDoctorAndDate(doctorId, date);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly("09:00");
        verify(listOperations, times(1)).rightPushAll(anyString(), (Collection<String>) argThat((Collection<String> list) ->
        list != null && list.size() == 1 && list.contains("09:00")
        ));
    }

    @Test
    @DisplayName("예약 생성 후 Redis 캐시 무효화")
    void createAppointment_ShouldInvalidateCache() throws InterruptedException {
        // Given
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(21L)
                .date(LocalDateTime.of(2025, 12, 18, 9, 0))
                .build();

        Appointment expectedAppointment = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .date(request.getDate())
                .status(AppointmentStatus.SCHEDULED)
                .departmentName("내과")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(21L)).willReturn(Optional.of(doctor));
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        given(appointmentRepository.existsByDoctorIdAndDate(anyLong(), any())).willReturn(false);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(expectedAppointment);
        given(chatService.createChatRoomForAppointment(anyLong())).willReturn("appointment_1");

        // When
        appointmentService.createAppointment(request);

        // Then
        verify(redisTemplate, times(1)).delete(contains("appointment:booked:21:2025-12-18"));
    }

    @Test
    @DisplayName("취소된 예약이 있는 시간대에 새로운 예약 가능")
    void createAppointment_WithCancelledAppointment_ShouldAllowNewBooking() throws InterruptedException {
        // Given
        LocalDateTime sameTime = LocalDateTime.of(2025, 12, 18, 9, 0);

        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(21L)
                .date(sameTime)
                .build();

        Appointment newAppointment = Appointment.builder()
                .id(2L)
                .patient(patient)
                .doctor(doctor)
                .date(sameTime)
                .status(AppointmentStatus.SCHEDULED)
                .departmentName("내과")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(21L)).willReturn(Optional.of(doctor));
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        // 취소된 예약은 existsByDoctorIdAndDate에서 false 반환 (쿼리에서 CANCELLED 제외)
        given(appointmentRepository.existsByDoctorIdAndDate(21L, sameTime)).willReturn(false);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(newAppointment);
        given(chatService.createChatRoomForAppointment(anyLong())).willReturn("appointment_2");

        // When
        Long appointmentId = appointmentService.createAppointment(request);

        // Then
        assertThat(appointmentId).isEqualTo(2L);
        verify(appointmentRepository, times(1)).existsByDoctorIdAndDate(21L, sameTime);
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
        verify(rLock, times(1)).unlock();
    }

    @Test
    @DisplayName("같은 시간대에 의사별로 독립적인 예약 상태 관리: 취소/예약됨/예약없음")
    void createAppointment_MultipleDoctors_IndependentBookingStatus() throws InterruptedException {
        // Given: 같은 시간대, 세 명의 다른 의사
        LocalDateTime sameTime = LocalDateTime.of(2025, 12, 18, 9, 0);

        // 의사1 (내과) - 기존 예약이 CANCELLED 상태
        User doctor1 = User.builder()
                .id(21L)
                .userId("doctor01")
                .name("내과의사")
                .role(Role.DOCTOR)
                .department("내과")
                .build();

        // 의사2 (외과) - 기존 예약이 SCHEDULED 상태
        User doctor2 = User.builder()
                .id(22L)
                .userId("doctor02")
                .name("외과의사")
                .role(Role.DOCTOR)
                .department("외과")
                .build();

        // 의사3 (피부과) - 예약 없음
        User doctor3 = User.builder()
                .id(23L)
                .userId("doctor03")
                .name("피부과의사")
                .role(Role.DOCTOR)
                .department("피부과")
                .build();

        // Scenario 1: 의사1(내과) - CANCELLED 예약 있음 → 새 예약 가능
        AppointmentRequest request1 = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(21L)
                .date(sameTime)
                .build();

        Appointment newAppointment1 = Appointment.builder()
                .id(101L)
                .patient(patient)
                .doctor(doctor1)
                .date(sameTime)
                .status(AppointmentStatus.SCHEDULED)
                .departmentName("내과")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(patient));
        given(userRepository.findById(21L)).willReturn(Optional.of(doctor1));
        given(redissonClient.getLock(contains("21"))).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        // 의사1: CANCELLED 예약은 제외되어 false 반환
        given(appointmentRepository.existsByDoctorIdAndDate(21L, sameTime)).willReturn(false);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(newAppointment1);
        given(chatService.createChatRoomForAppointment(101L)).willReturn("appointment_101");

        // When: 의사1에게 예약 시도
        Long appointmentId1 = appointmentService.createAppointment(request1);

        // Then: 의사1 예약 성공 (CANCELLED는 중복 체크에서 제외됨)
        assertThat(appointmentId1).isEqualTo(101L);
        verify(appointmentRepository, times(1)).existsByDoctorIdAndDate(21L, sameTime);
        verify(appointmentRepository, times(1)).save(argThat(apt ->
            apt.getDoctor().getId().equals(21L) &&
            apt.getDepartmentName().equals("내과")
        ));

        // Scenario 2: 의사2(외과) - SCHEDULED 예약 있음 → 새 예약 불가
        AppointmentRequest request2 = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(22L)
                .date(sameTime)
                .build();

        given(userRepository.findById(22L)).willReturn(Optional.of(doctor2));
        given(redissonClient.getLock(contains("22"))).willReturn(rLock);

        // 의사2: SCHEDULED 예약이 있어서 true 반환
        given(appointmentRepository.existsByDoctorIdAndDate(22L, sameTime)).willReturn(true);

        // When & Then: 의사2 예약 시도 → 실패
        assertThatThrownBy(() -> appointmentService.createAppointment(request2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("해당 의사의 해당 시간에 예약이 이미 꽉 찼습니다");

        verify(appointmentRepository, times(1)).existsByDoctorIdAndDate(22L, sameTime);
        verify(appointmentRepository, times(1)).save(any(Appointment.class)); // 의사1만 저장됨

        // Scenario 3: 의사3(피부과) - 예약 없음 → 새 예약 가능
        AppointmentRequest request3 = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(23L)
                .date(sameTime)
                .build();

        Appointment newAppointment3 = Appointment.builder()
                .id(103L)
                .patient(patient)
                .doctor(doctor3)
                .date(sameTime)
                .status(AppointmentStatus.SCHEDULED)
                .departmentName("피부과")
                .build();

        given(userRepository.findById(23L)).willReturn(Optional.of(doctor3));
        given(redissonClient.getLock(contains("23"))).willReturn(rLock);

        // 의사3: 예약 없음 (false 반환)
        given(appointmentRepository.existsByDoctorIdAndDate(23L, sameTime)).willReturn(false);
        given(appointmentRepository.save(any(Appointment.class))).willReturn(newAppointment3);
        given(chatService.createChatRoomForAppointment(103L)).willReturn("appointment_103");

        // When: 의사3에게 예약 시도
        Long appointmentId3 = appointmentService.createAppointment(request3);

        // Then: 의사3 예약 성공 (예약 없음)
        assertThat(appointmentId3).isEqualTo(103L);
        verify(appointmentRepository, times(1)).existsByDoctorIdAndDate(23L, sameTime);
        verify(appointmentRepository, times(2)).save(any(Appointment.class)); // 의사1 + 의사3

        // 최종 검증: 각 의사의 예약 상태가 독립적으로 관리됨
        verify(appointmentRepository, times(1)).existsByDoctorIdAndDate(21L, sameTime); // 내과
        verify(appointmentRepository, times(1)).existsByDoctorIdAndDate(22L, sameTime); // 외과
        verify(appointmentRepository, times(1)).existsByDoctorIdAndDate(23L, sameTime); // 피부과
    }
}
