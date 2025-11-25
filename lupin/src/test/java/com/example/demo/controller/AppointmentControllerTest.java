package com.example.demo.controller;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.service.AppointmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestRedisConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@DisplayName("AppointmentController 테스트")
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;

    @Test
    @DisplayName("예약 생성 성공")
    void createAppointment_Success() throws Exception {
        // given
        LocalDateTime apptDate = LocalDateTime.of(2025, 12, 1, 10, 0);
        User patient = User.builder().id(1L).userId("patient01").build();
        User doctor = User.builder().id(2L).userId("doctor01").build();

        Appointment appointment = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .apptDate(apptDate)
                .reason("정기 검진")
                .build();

        given(appointmentService.createAppointment(anyLong(), anyLong(), any(LocalDateTime.class), anyString()))
                .willReturn(appointment);

        // when & then
        mockMvc.perform(post("/api/appointments")
                        .param("patientId", "1")
                        .param("doctorId", "2")
                        .param("apptDate", "2025-12-01T10:00:00")
                        .param("reason", "정기 검진"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reason").value("정기 검진"));
    }

    @Test
    @DisplayName("환자의 예약 목록 조회")
    void getPatientAppointments_Success() throws Exception {
        // given
        User patient = User.builder().id(1L).userId("patient01").build();

        Appointment appointment1 = Appointment.builder()
                .id(1L)
                .patient(patient)
                .build();
        Appointment appointment2 = Appointment.builder()
                .id(2L)
                .patient(patient)
                .build();

        List<Appointment> appointments = Arrays.asList(appointment1, appointment2);
        given(appointmentService.getPatientAppointments(1L)).willReturn(appointments);

        // when & then
        mockMvc.perform(get("/api/appointments/patient/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("의사의 예약 목록 조회")
    void getDoctorAppointments_Success() throws Exception {
        // given
        User doctor = User.builder().id(2L).userId("doctor01").build();

        Appointment appointment1 = Appointment.builder()
                .id(1L)
                .doctor(doctor)
                .build();

        List<Appointment> appointments = Arrays.asList(appointment1);
        given(appointmentService.getDoctorAppointments(2L)).willReturn(appointments);

        // when & then
        mockMvc.perform(get("/api/appointments/doctor/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("예약 상세 조회")
    void getAppointmentDetail_Success() throws Exception {
        // given
        User patient = User.builder().id(1L).userId("patient01").build();
        User doctor = User.builder().id(2L).userId("doctor01").build();

        Appointment appointment = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .reason("정기 검진")
                .build();

        given(appointmentService.getAppointmentDetail(1L)).willReturn(appointment);

        // when & then
        mockMvc.perform(get("/api/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reason").value("정기 검진"));
    }

    @Test
    @DisplayName("예약 완료 처리")
    void completeAppointment_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/appointments/1/complete"))
                .andExpect(status().isOk());

        then(appointmentService).should().completeAppointment(1L);
    }

    @Test
    @DisplayName("예약 취소")
    void cancelAppointment_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/appointments/1/cancel"))
                .andExpect(status().isOk());

        then(appointmentService).should().cancelAppointment(1L);
    }

    @Test
    @DisplayName("특정 기간의 예약 조회")
    void getAppointmentsBetween_Success() throws Exception {
        // given
        LocalDateTime start = LocalDateTime.of(2025, 12, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);

        User patient = User.builder().id(1L).userId("patient01").build();

        Appointment appointment1 = Appointment.builder()
                .id(1L)
                .patient(patient)
                .apptDate(LocalDateTime.of(2025, 12, 15, 10, 0))
                .build();

        List<Appointment> appointments = Arrays.asList(appointment1);
        given(appointmentService.getAppointmentsBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(appointments);

        // when & then
        mockMvc.perform(get("/api/appointments/between")
                        .param("start", "2025-12-01T00:00:00")
                        .param("end", "2025-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
