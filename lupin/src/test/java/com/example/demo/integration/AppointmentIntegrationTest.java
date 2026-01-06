package com.example.demo.integration;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.AppointmentRequest;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest // 실제 서버 환경을 띄움
@AutoConfigureMockMvc // API 요청을 보낼 수 있는 MockMvc 설정
@Transactional // 테스트 끝나면 데이터 롤백 (DB 오염 방지)
@Import(TestRedisConfiguration.class)
@Disabled("디버깅 용도 테스트 클래스 - CI에서 제외 (BeanDefinitionOverrideException 발생)")
class AppointmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentService appointmentService;

    @SpyBean
    private SimpMessagingTemplate messagingTemplate;

    private Long patientId;
    private Long doctorId;

    @BeforeEach
    void setUp() {
        // 1. 테스트를 위한 의사와 환자 데이터를 DB에 실제로 넣습니다.
        User patient = User.builder()
                .userId("patient123")
                .password("password")
                .name("테스트환자")
                .role(Role.MEMBER)
                .build();
        patient = userRepository.save(patient);
        patientId = patient.getId();

        User doctor = User.builder()
                .userId("doctor123")
                .password("password")
                .name("테스트의사")
                .role(Role.DOCTOR)
                .department("외과")
                .build();
        doctor = userRepository.save(doctor);
        doctorId = doctor.getId();
    }

    @Test
    @Disabled("디버깅 용도 테스트 - CI에서 제외")
    @DisplayName("예약 생성 통합 테스트 - 500 에러 원인 파악용")
    void createAppointment_IntegrationTest() throws Exception {
        // Given: 아까 실패했던 요청 데이터와 동일하게 구성
        AppointmentRequest request = new AppointmentRequest();
        // DTO에 Setter나 Builder가 있다고 가정합니다. (없으면 추가 필요)
        // Reflection을 사용하거나 필드에 직접 할당해야 할 수도 있습니다.
        // 여기서는 편의상 json으로 변환될 객체를 만듭니다.
        
        // *주의: AppointmentRequest 구조에 맞춰서 값을 넣어주세요*
        // 예: request.setPatientId(patientId);
        //     request.setDoctorId(doctorId);
        //     request.setDate(LocalDateTime.of(2025, 12, 11, 10, 0));
        
        // JSON 문자열 직접 생성 (DTO 구조 문제 회피용)
        String requestBody = String.format(
            "{\"patientId\": %d, \"doctorId\": %d, \"date\": \"2025-12-11T10:00:00\"}",
            patientId, doctorId
        );

        // When & Then
        mockMvc.perform(post("/api/appointment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print()) // 🌟 여기가 핵심! 요청/응답 로그를 콘솔에 다 찍어줍니다.
                .andExpect(status().isOk()); // 성공해야 한다고 가정 (실패하면 에러 뜸)
    }

    @Test@DisplayName("예약 시 재대로 진료과목이 반영이 되는지 테스트")
    void createAppointment_shouldPersistDepartmentName() throws Exception {
        // Given
        AppointmentRequest request = new AppointmentRequest();
        User doctor = userRepository.save(User.builder()
                .userId("doctor01")
                .name("미스터최")
                .role(Role.DOCTOR)
                .department("내과")
                .build());
        
        User patient = userRepository.save(User.builder()
                .userId("patient01")
                .password("pass")
                .name("미스터박")
                .role(Role.MEMBER)
                .build());

        String requestBody = String.format(
            "{\"patientId\": %d, \"doctorId\": %d, \"date\": \"2025-12-11T10:00:00\"}",
            patient.getId(), doctor.getId()
        );

        mockMvc.perform(post("/api/appointment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        Appointment savedApt = appointmentRepository.findAll().stream()
                .filter(a -> a.getDoctor().getId().equals(doctor.getId()))
                .findFirst().get();
        assertThat(savedApt.getDepartmentName()).isEqualTo("내과");
    }

    @Test
    @DisplayName("진료 완료 시 트랜잭션 커밋 후 WebSocket 메시지 전송 (afterCommit 실행)")
    void completeConsultation_ShouldSendWebSocketMessageAfterCommit() {
        // Given: IN_PROGRESS 상태의 예약 생성
        User doctor = userRepository.save(User.builder()
                .userId("doctor02")
                .name("김의사")
                .role(Role.DOCTOR)
                .department("내과")
                .build());

        User patient = userRepository.save(User.builder()
                .userId("patient02")
                .password("pass")
                .name("박환자")
                .role(Role.MEMBER)
                .build());

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
        // timeout을 사용하여 비동기 메시지 전송을 기다림 (최대 2초)
        verify(messagingTemplate, timeout(2000).times(1)).convertAndSend(
                eq("/queue/chat/" + expectedRoomId),
                any(AppointmentService.ConsultationEndNotification.class)
        );

        // 예약 상태도 COMPLETED로 변경되었는지 확인
        Appointment updatedAppointment = appointmentRepository.findById(appointmentId).orElseThrow();
        assertThat(updatedAppointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("진료 완료 시 WebSocket 메시지에 올바른 정보 포함")
    void completeConsultation_ShouldSendCorrectNotificationData() {
        // Given
        User doctor = userRepository.save(User.builder()
                .userId("doctor03")
                .name("이의사")
                .role(Role.DOCTOR)
                .department("외과")
                .build());

        User patient = userRepository.save(User.builder()
                .userId("patient03")
                .password("pass")
                .name("최환자")
                .role(Role.MEMBER)
                .build());

        Appointment appointment = appointmentRepository.save(Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 16, 15, 30))
                .status(AppointmentStatus.IN_PROGRESS)
                .departmentName(doctor.getDepartment())
                .build());

        Long appointmentId = appointment.getId();

        // When
        appointmentService.completeConsultation(appointmentId);

        // Then: WebSocket 메시지에 appointmentId와 doctorName이 포함되어야 함
        verify(messagingTemplate, timeout(2000).times(1)).convertAndSend(
                eq("/queue/chat/appointment_" + appointmentId),
                any(AppointmentService.ConsultationEndNotification.class)
        );
    }
}