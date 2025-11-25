package com.example.demo.controller;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.dto.response.PrescriptionResponse;
import com.example.demo.service.PrescriptionService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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
@DisplayName("PrescriptionController 테스트")
class PrescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrescriptionService prescriptionService;

    @Test
    @DisplayName("처방전 생성 성공 - 의사 권한")
    @WithMockUser(roles = "DOCTOR")
    void createPrescription_Success() throws Exception {
        // given
        PrescriptionResponse response = PrescriptionResponse.builder()
                .id(1L)
                .patientId(10L)
                .doctorId(5L)
                .prescribedDate(LocalDate.now())
                .build();

        given(prescriptionService.createPrescription(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\": \"감기\", \"prescribedDate\": \"2025-11-25\", \"patientId\": 10, \"doctorId\": 5, \"medicines\": []}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientId").value(10))
                .andExpect(jsonPath("$.doctorId").value(5));
    }

    @Test
    @DisplayName("특정 환자의 처방전 목록 조회 - 페이징")
    @WithMockUser
    void getPrescriptionsByPatientId_Success() throws Exception {
        // given
        PrescriptionResponse response1 = PrescriptionResponse.builder()
                .id(1L)
                .patientId(10L)
                .doctorId(5L)
                .build();
        PrescriptionResponse response2 = PrescriptionResponse.builder()
                .id(2L)
                .patientId(10L)
                .doctorId(6L)
                .build();

        Page<PrescriptionResponse> page = new PageImpl<>(Arrays.asList(response1, response2), PageRequest.of(0, 20), 2);
        given(prescriptionService.getPrescriptionsByPatientId(eq(10L), any())).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/prescriptions/patients/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("특정 환자의 처방전 목록 조회 - 전체")
    @WithMockUser
    void getAllPrescriptionsByPatientId_Success() throws Exception {
        // given
        PrescriptionResponse response1 = PrescriptionResponse.builder()
                .id(1L)
                .patientId(10L)
                .build();
        PrescriptionResponse response2 = PrescriptionResponse.builder()
                .id(2L)
                .patientId(10L)
                .build();

        List<PrescriptionResponse> responses = Arrays.asList(response1, response2);
        given(prescriptionService.getAllPrescriptionsByPatientId(10L)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/prescriptions/patients/10/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("특정 의사가 발행한 처방전 목록 조회 - 페이징")
    @WithMockUser(roles = "DOCTOR")
    void getPrescriptionsByDoctorId_Success() throws Exception {
        // given
        PrescriptionResponse response1 = PrescriptionResponse.builder()
                .id(1L)
                .doctorId(5L)
                .build();

        Page<PrescriptionResponse> page = new PageImpl<>(Arrays.asList(response1), PageRequest.of(0, 20), 1);
        given(prescriptionService.getPrescriptionsByDoctorId(eq(5L), any())).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/prescriptions/doctors/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("특정 의사가 발행한 처방전 목록 조회 - 전체")
    @WithMockUser(roles = "DOCTOR")
    void getAllPrescriptionsByDoctorId_Success() throws Exception {
        // given
        PrescriptionResponse response1 = PrescriptionResponse.builder()
                .id(1L)
                .doctorId(5L)
                .build();

        List<PrescriptionResponse> responses = Arrays.asList(response1);
        given(prescriptionService.getAllPrescriptionsByDoctorId(5L)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/prescriptions/doctors/5/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("처방전 상세 조회")
    @WithMockUser
    void getPrescriptionDetail_Success() throws Exception {
        // given
        PrescriptionResponse response = PrescriptionResponse.builder()
                .id(1L)
                .patientId(10L)
                .doctorId(5L)
                .build();

        given(prescriptionService.getPrescriptionDetail(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/prescriptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientId").value(10))
                .andExpect(jsonPath("$.doctorId").value(5));
    }

    @Test
    @DisplayName("특정 환자의 최근 처방전 조회")
    @WithMockUser
    void getRecentPrescriptionsByPatientId_Success() throws Exception {
        // given
        PrescriptionResponse response1 = PrescriptionResponse.builder()
                .id(1L)
                .patientId(10L)
                .build();

        List<PrescriptionResponse> responses = Arrays.asList(response1);
        given(prescriptionService.getRecentPrescriptionsByPatientId(10L, 5)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/prescriptions/patients/10/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("특정 기간 내 처방전 조회")
    @WithMockUser
    void getPrescriptionsByDateRange_Success() throws Exception {
        // given
        PrescriptionResponse response1 = PrescriptionResponse.builder()
                .id(1L)
                .prescribedDate(LocalDate.of(2025, 1, 15))
                .build();

        List<PrescriptionResponse> responses = Arrays.asList(response1);
        given(prescriptionService.getPrescriptionsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/prescriptions/date-range")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("처방전 이름으로 검색")
    @WithMockUser
    void searchPrescriptionsByName_Success() throws Exception {
        // given
        PrescriptionResponse response1 = PrescriptionResponse.builder()
                .id(1L)
                .build();

        Page<PrescriptionResponse> page = new PageImpl<>(Arrays.asList(response1), PageRequest.of(0, 20), 1);
        given(prescriptionService.searchPrescriptionsByName(eq("keyword"), any())).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/prescriptions/search")
                        .param("keyword", "keyword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("처방전 삭제 - 의사 권한")
    @WithMockUser(roles = "DOCTOR")
    void deletePrescription_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/prescriptions/1")
                        .param("doctorId", "5"))
                .andExpect(status().isNoContent());

        then(prescriptionService).should().deletePrescription(1L, 5L);
    }

    @Test
    @DisplayName("특정 환자의 처방전 수 조회")
    @WithMockUser
    void getPrescriptionCountByPatientId_Success() throws Exception {
        // given
        given(prescriptionService.getPrescriptionCountByPatientId(10L)).willReturn(5L);

        // when & then
        mockMvc.perform(get("/api/prescriptions/patients/10/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));
    }
}
