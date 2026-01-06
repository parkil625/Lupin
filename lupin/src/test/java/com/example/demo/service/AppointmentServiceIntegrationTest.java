package com.example.demo.service;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.domain.enums.Role;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * AppointmentService의 트랜잭션 및 WebSocket 메시지 전송을 테스트하는 통합 테스트
 */
@SpringBootTest
@Import(TestRedisConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@DisplayName("AppointmentService 통합 테스트 - 트랜잭션 및 WebSocket")
class AppointmentServiceIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @SpyBean
    private SimpMessagingTemplate messagingTemplate;

    private User doctor;
    private User patient;

    @BeforeEach
    void setUp() {
        // 각 테스트마다 새로운 의사와 환자 생성
        doctor = userRepository.save(User.builder()
                .userId("doctor_" + System.currentTimeMillis())
                .name("테스트의사")
                .password("password")
                .role(Role.DOCTOR)
                .department("내과")
                .build());

        patient = userRepository.save(User.builder()
                .userId("patient_" + System.currentTimeMillis())
                .name("테스트환자")
                .password("password")
                .role(Role.MEMBER)
                .build());
    }

    @Test
    @Transactional
    @DisplayName("진료 완료 시 트랜잭션 커밋 후 WebSocket 메시지 전송 (afterCommit 실행)")
    void completeConsultation_ShouldSendWebSocketMessageAfterCommit() {
        // Given: IN_PROGRESS 상태의 예약 생성
        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 15, 14, 0))
                .status(AppointmentStatus.IN_PROGRESS)
                .departmentName(doctor.getDepartment())
                .build());

        Long appointmentId = appointment.getId();
        String expectedRoomId = "appointment_" + appointmentId;

        // When: 진료 완료 메서드 호출 (이 메서드는 @Transactional이므로 트랜잭션이 활성화됨)
        appointmentService.completeConsultation(appointmentId);

        // Then: afterCommit이 실행되어 WebSocket 메시지가 전송되어야 함
        // timeout을 사용하여 비동기 메시지 전송을 기다림 (최대 3초)
        verify(messagingTemplate, timeout(3000).times(1)).convertAndSend(
                eq("/queue/chat/" + expectedRoomId),
                any(AppointmentService.ConsultationEndNotification.class)
        );

        // 예약 상태도 COMPLETED로 변경되었는지 확인
        Appointment updatedAppointment = appointmentRepository.findById(appointmentId).orElseThrow();
        assertThat(updatedAppointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @Transactional
    @DisplayName("진료 완료 시 WebSocket 메시지 전송 확인 및 예약 상태 변경 확인")
    void completeConsultation_ShouldSendCorrectNotificationDataAndUpdateStatus() {
        // Given
        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 16, 15, 30))
                .status(AppointmentStatus.IN_PROGRESS)
                .departmentName(doctor.getDepartment())
                .build());

        Long appointmentId = appointment.getId();
        String doctorName = doctor.getName();

        // When
        appointmentService.completeConsultation(appointmentId);

        // Then: WebSocket 메시지에 appointmentId와 doctorName이 포함되어야 함
        verify(messagingTemplate, timeout(3000).times(1)).convertAndSend(
                eq("/queue/chat/appointment_" + appointmentId),
                any(AppointmentService.ConsultationEndNotification.class)
        );

        // 예약 상태 확인
        Appointment updatedAppointment = appointmentRepository.findById(appointmentId).orElseThrow();
        assertThat(updatedAppointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @Transactional
    @DisplayName("진료 완료 후 트랜잭션 커밋 확인 - DB에 상태 변경 반영")
    void completeConsultation_ShouldPersistStatusChangeInDatabase() {
        // Given
        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 17, 10, 0))
                .status(AppointmentStatus.IN_PROGRESS)
                .departmentName(doctor.getDepartment())
                .build());

        Long appointmentId = appointment.getId();

        // When
        appointmentService.completeConsultation(appointmentId);

        // Then: DB에서 다시 조회했을 때도 상태가 COMPLETED여야 함
        appointmentRepository.flush();
        Appointment persistedAppointment = appointmentRepository.findById(appointmentId).orElseThrow();
        assertThat(persistedAppointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);

        // WebSocket 메시지도 전송되었는지 확인
        verify(messagingTemplate, timeout(3000).times(1)).convertAndSend(
                eq("/queue/chat/appointment_" + appointmentId),
                any(AppointmentService.ConsultationEndNotification.class)
        );
    }
}
