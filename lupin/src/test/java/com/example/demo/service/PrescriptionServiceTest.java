package com.example.demo.service;

import com.example.demo.domain.entity.Prescription;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionService 테스트")
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @InjectMocks
    private PrescriptionService prescriptionService;

    private User doctor;
    private User anotherDoctor;
    private User patient;
    private Prescription prescription;

    @BeforeEach
    void setUp() {
        doctor = User.builder()
                .id(1L)
                .userId("doctor1")
                .name("Dr. Kim")
                .build();

        anotherDoctor = User.builder()
                .id(2L)
                .userId("doctor2")
                .name("Dr. Park")
                .build();

        patient = User.builder()
                .id(3L)
                .userId("patient1")
                .name("Patient Lee")
                .build();

        prescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .date(LocalDate.of(2025, 12, 1))
                .diagnosis("감기")
                .build();
    }

    @Test
    @DisplayName("타인의 처방전 수정 시 예외 발생")
    void shouldThrowExceptionWhenUnauthorizedDoctorTriesToUpdate() {
        // given
        Long prescriptionId = 1L;
        Long unauthorizedDoctorId = 2L;
        String newDiagnosis = "독감";

        given(prescriptionRepository.findById(prescriptionId))
                .willReturn(Optional.of(prescription));

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.updateDiagnosis(prescriptionId, unauthorizedDoctorId, newDiagnosis))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("처방전을 수정할 권한이 없습니다.");
    }

    @Test
    @DisplayName("타인의 처방전 삭제 시 예외 발생")
    void shouldThrowExceptionWhenUnauthorizedDoctorTriesToDelete() {
        // given
        Long prescriptionId = 1L;
        Long unauthorizedDoctorId = 2L;

        given(prescriptionRepository.findById(prescriptionId))
                .willReturn(Optional.of(prescription));

        // when & then
        assertThatThrownBy(() ->
                prescriptionService.deletePrescription(prescriptionId, unauthorizedDoctorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("처방전을 삭제할 권한이 없습니다.");
    }
}
