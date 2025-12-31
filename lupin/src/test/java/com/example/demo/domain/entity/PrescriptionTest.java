package com.example.demo.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Prescription 엔티티 테스트")
class PrescriptionTest {

    @Test
    @DisplayName("처방전 생성 시 약품 정보를 medicines 컬렉션에 추가할 수 있다")
    void createPrescriptionWithMedicinesTest() {
        // given
        Medicine tylenol = Medicine.builder()
                .id(1L)
                .name("타이레놀")
                .code("MED001")
                .precautions("간 질환 환자 주의")
                .build();

        Medicine aspirin = Medicine.builder()
                .id(2L)
                .name("아스피린")
                .code("MED002")
                .precautions("출혈 위험 주의")
                .build();

        // when
        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("감기")
                .build();

        PrescriptionMedicine pm1 = PrescriptionMedicine.builder()
                .medicine(tylenol)
                .dosage("1정")
                .frequency("1일 3회")
                .durationDays(3)
                .build();

        PrescriptionMedicine pm2 = PrescriptionMedicine.builder()
                .medicine(aspirin)
                .dosage("1정")
                .frequency("1일 2회")
                .durationDays(5)
                .build();

        prescription.addMedicine(pm1);
        prescription.addMedicine(pm2);

        // then
        assertThat(prescription.getMedicines()).hasSize(2);
        assertThat(prescription.getMedicines()).contains(pm1, pm2);
        assertThat(pm1.getPrescription()).isEqualTo(prescription);
        assertThat(pm2.getPrescription()).isEqualTo(prescription);
    }

    @Test
    @DisplayName("진단명을 수정할 수 있다")
    void updateDiagnosisTest() {
        // given
        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("감기")
                .build();

        // when
        prescription.updateDiagnosis("급성 상기도 감염");

        // then
        assertThat(prescription.getDiagnosis()).isEqualTo("급성 상기도 감염");
    }

    @Test
    @DisplayName("처방전 생성 시 medicines 컬렉션은 빈 리스트로 초기화된다")
    void medicinesInitializedAsEmptyListTest() {
        // given & when
        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("경과 관찰")
                .build();

        // then
        assertThat(prescription.getMedicines()).isNotNull();
        assertThat(prescription.getMedicines()).isEmpty();
    }

    @Test
    @DisplayName("약품을 제거할 수 있다")
    void removeMedicineTest() {
        // given
        Medicine tylenol = Medicine.builder()
                .id(1L)
                .name("타이레놀")
                .build();

        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("감기")
                .build();

        PrescriptionMedicine pm = PrescriptionMedicine.builder()
                .medicine(tylenol)
                .dosage("1정")
                .frequency("1일 3회")
                .durationDays(3)
                .build();

        prescription.addMedicine(pm);
        assertThat(prescription.getMedicines()).hasSize(1);

        // when
        prescription.removeMedicine(pm);

        // then
        assertThat(prescription.getMedicines()).isEmpty();
        assertThat(pm.getPrescription()).isNull();
    }

    @Test
    @DisplayName("모든 약품을 제거할 수 있다")
    void clearMedicinesTest() {
        // given
        Medicine tylenol = Medicine.builder()
                .id(1L)
                .name("타이레놀")
                .build();

        Medicine aspirin = Medicine.builder()
                .id(2L)
                .name("아스피린")
                .build();

        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("감기")
                .build();

        PrescriptionMedicine pm1 = PrescriptionMedicine.builder()
                .medicine(tylenol)
                .dosage("1정")
                .frequency("1일 3회")
                .durationDays(3)
                .build();

        PrescriptionMedicine pm2 = PrescriptionMedicine.builder()
                .medicine(aspirin)
                .dosage("1정")
                .frequency("1일 2회")
                .durationDays(5)
                .build();

        prescription.addMedicine(pm1);
        prescription.addMedicine(pm2);
        assertThat(prescription.getMedicines()).hasSize(2);

        // when
        prescription.clearMedicines();

        // then
        assertThat(prescription.getMedicines()).isEmpty();
    }
}
