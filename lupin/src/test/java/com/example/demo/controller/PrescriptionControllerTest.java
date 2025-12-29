package com.example.demo.controller;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.Prescription;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.prescription.MedicineResponse;
import com.example.demo.dto.prescription.PrescriptionRequest;
import com.example.demo.dto.prescription.PrescriptionResponse;
import com.example.demo.repository.MedicineRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.PrescriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@DisplayName("PrescriptionController 테스트")
class PrescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PrescriptionService prescriptionService;

    @MockitoBean
    private MedicineRepository medicineRepository;

    @MockitoBean
    private UserRepository userRepository;

    private User patient;
    private User doctor;
    private Prescription prescription;
    private PrescriptionResponse prescriptionResponse;

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

        Appointment appointment = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .departmentName("내과")
                .status(AppointmentStatus.SCHEDULED)
                .build();

        prescription = Prescription.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .appointment(appointment)
                .diagnosis("감기")
                .medications("타이레놀 500mg")
                .date(LocalDate.now())
                .build();

        prescriptionResponse = PrescriptionResponse.from(prescription);

        // UserRepository Mock 설정
        given(userRepository.findByUserId("patient01")).willReturn(Optional.of(patient));
        given(userRepository.findByUserId("doctor01")).willReturn(Optional.of(doctor));
        given(userRepository.findByUserId("patient02")).willReturn(Optional.of(
                User.builder()
                        .id(2L)
                        .userId("patient02")
                        .name("환자2")
                        .role(Role.MEMBER)
                        .build()
        ));
    }

    @Test
    @DisplayName("환자 본인이 자신의 처방전 목록 조회 성공 - MEMBER 역할")
    void getPatientPrescriptions_AsPatient_Success() throws Exception {
        // given
        given(prescriptionService.getPatientPrescriptions(anyLong()))
                .willReturn(List.of(prescriptionResponse));

        // when & then
        mockMvc.perform(get("/api/prescriptions/patient/{patientId}", patient.getId())
                        .with(user(patient.getUserId()).roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(prescription.getId()))
                .andExpect(jsonPath("$[0].diagnosis").value("감기"));
    }

    @Test
    @DisplayName("의사가 환자의 처방전 목록 조회 성공 - DOCTOR 역할")
    void getPatientPrescriptions_AsDoctor_Success() throws Exception {
        // given
        given(prescriptionService.getPatientPrescriptions(anyLong()))
                .willReturn(List.of(prescriptionResponse));

        // when & then
        mockMvc.perform(get("/api/prescriptions/patient/{patientId}", patient.getId())
                        .with(user(doctor.getUserId()).roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].patientName").value("환자1"));
    }

    @Test
    @DisplayName("환자가 다른 환자의 처방전 목록 조회 시도 시 403 Forbidden")
    void getPatientPrescriptions_AsOtherPatient_Forbidden() throws Exception {
        // given
        User otherPatient = User.builder()
                .id(2L)
                .userId("patient02")
                .name("환자2")
                .role(Role.MEMBER)
                .build();

        // when & then
        mockMvc.perform(get("/api/prescriptions/patient/{patientId}", patient.getId())
                        .with(user(otherPatient.getUserId()).roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("처방전이 없는 환자 조회 시 빈 배열 반환")
    void getPatientPrescriptions_NoPrescriptions_ReturnsEmptyArray() throws Exception {
        // given
        given(prescriptionService.getPatientPrescriptions(anyLong()))
                .willReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/api/prescriptions/patient/{patientId}", patient.getId())
                        .with(user(patient.getUserId()).roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("처방전 상세 조회 성공")
    void getPrescription_Success() throws Exception {
        // given
        given(prescriptionService.getPrescriptionById(anyLong()))
                .willReturn(Optional.of(prescriptionResponse));

        // when & then
        mockMvc.perform(get("/api/prescriptions/{id}", prescription.getId())
                        .with(user(patient.getUserId()).roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prescription.getId()))
                .andExpect(jsonPath("$.diagnosis").value("감기"))
                .andExpect(jsonPath("$.medications").value("타이레놀 500mg"));
    }

    @Test
    @DisplayName("존재하지 않는 처방전 조회 시 404 Not Found")
    void getPrescription_NotFound() throws Exception {
        // given
        given(prescriptionService.getPrescriptionById(anyLong()))
                .willReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/prescriptions/{id}", 999L)
                        .with(user(patient.getUserId()).roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("약품 검색 성공 - DOCTOR 역할")
    void searchMedicines_Success() throws Exception {
        // given
        given(medicineRepository.findByNameContainingIgnoreCase(any()))
                .willReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/api/prescriptions/medicines/search")
                        .param("query", "타이레놀")
                        .with(user(doctor.getUserId()).roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("전체 약품 목록 조회 성공 - DOCTOR 역할")
    void getAllMedicines_Success() throws Exception {
        // given
        given(medicineRepository.findByOrderByNameAsc())
                .willReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/api/prescriptions/medicines")
                        .with(user(doctor.getUserId()).roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
