package com.example.demo.repository;

import com.example.demo.domain.entity.Prescription;
import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Test
    @DisplayName("처방전 생성 시 필수 필드가 저장되는지 테스트")
    void shouldSavePrescriptionWithRequiredFields() {
        // given
        User doctor = createAndSaveUser("doctor1", "Dr. Kim");
        User patient = createAndSaveUser("patient1", "Patient Lee");
        LocalDate prescribedDate = LocalDate.of(2025, 12, 1);

        Prescription prescription = Prescription.builder()
                .doctor(doctor)
                .patient(patient)
                .date(prescribedDate)
                .diagnosis("감기")
                .build();

        // when
        Prescription saved = prescriptionRepository.save(prescription);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDoctor()).isEqualTo(doctor);
        assertThat(saved.getPatient()).isEqualTo(patient);
        assertThat(saved.getDate()).isEqualTo(prescribedDate);
        assertThat(saved.getDiagnosis()).isEqualTo("감기");
    }
}
