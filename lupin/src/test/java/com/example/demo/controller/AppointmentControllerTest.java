package com.example.demo.controller;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.AppointmentRequest;
import com.example.demo.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@DisplayName("AppointmentController 테스트")
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
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
                .date(LocalDateTime.of(2025, 12, 10, 15, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();
    }

    @Test
    @WithMockUser
    @DisplayName("환자 예약 목록 조회 성공")
    void getPatientAppointments_ShouldReturnAppointmentList() throws Exception {
        // Given
        Long patientId = 1L;
        Appointment appointment2 = Appointment.builder()
                .id(2L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 15, 10, 0))
                .status(AppointmentStatus.COMPLETED)
                .build();

        List<Appointment> appointments = List.of(appointment, appointment2);
        given(appointmentService.getPatientAppointments(patientId))
                .willReturn(appointments);

        // When & Then
        mockMvc.perform(get("/api/appointment/patient/{patientId}", patientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].patientId").value(1))
                .andExpect(jsonPath("$[0].doctorId").value(21))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].status").value("COMPLETED"));

        verify(appointmentService).getPatientAppointments(patientId);
    }

    @Test
    @WithMockUser
    @DisplayName("의사 예약 목록 조회 성공")
    void getDoctorAppointments_ShouldReturnAppointmentList() throws Exception {
        // Given
        Long doctorId = 21L;
        List<Appointment> appointments = List.of(appointment);
        given(appointmentService.getDoctorAppointments(doctorId))
                .willReturn(appointments);

        // When & Then
        mockMvc.perform(get("/api/appointment/doctor/{doctorId}", doctorId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].doctorId").value(21));

        verify(appointmentService).getDoctorAppointments(doctorId);
    }

    @Test
    @WithMockUser
    @DisplayName("예약 생성 성공")
    void createAppointment_ShouldReturnAppointmentId() throws Exception {
        // Given
        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(1L)
                .doctorId(21L)
                .date(LocalDateTime.of(2025, 12, 20, 14, 0))
                .build();

        given(appointmentService.createAppointment(any(AppointmentRequest.class)))
                .willReturn(1L);

        // When & Then
        mockMvc.perform(post("/api/appointment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        verify(appointmentService).createAppointment(any(AppointmentRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("예약 취소 성공")
    void cancelAppointment_ShouldReturnSuccessMessage() throws Exception {
        // Given
        Long appointmentId = 1L;

        // When & Then
        mockMvc.perform(put("/api/appointment/{appointmentId}/cancel", appointmentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("예약이 취소되었습니다."));

        verify(appointmentService).cancelAppointment(appointmentId);
    }
}
