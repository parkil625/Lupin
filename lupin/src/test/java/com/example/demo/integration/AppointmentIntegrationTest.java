package com.example.demo.integration;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.AppointmentRequest;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest // 실제 서버 환경을 띄움
@AutoConfigureMockMvc // API 요청을 보낼 수 있는 MockMvc 설정
@Transactional // 테스트 끝나면 데이터 롤백 (DB 오염 방지)
class AppointmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

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
                .build();
        doctor = userRepository.save(doctor);
        doctorId = doctor.getId();
    }

    @Test
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
}