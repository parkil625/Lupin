package com.example.demo.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Prescription 엔티티 테스트")
class PrescriptionTest {

    @Test
    @DisplayName("약품을 추가하면 medicines 리스트에 포함된다")
    void addMedicineTest() {
        // given
        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("감기")
                .build();

        PrescriptionMed medicine = PrescriptionMed.builder()
                .medicineName("타이레놀 500mg")
                .dosage("1정")
                .frequency("1일 3회")
                .build();

        // when
        prescription.addMedicine(medicine);

        // then
        assertThat(prescription.getMedicines()).hasSize(1);
        assertThat(prescription.getMedicines()).contains(medicine);
        assertThat(medicine.getPrescription()).isEqualTo(prescription);
    }

    @Test
    @DisplayName("약품을 제거하면 medicines 리스트에서 사라진다")
    void removeMedicineTest() {
        // given
        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("감기")
                .build();

        PrescriptionMed medicine = PrescriptionMed.builder()
                .medicineName("타이레놀 500mg")
                .dosage("1정")
                .frequency("1일 3회")
                .build();

        prescription.addMedicine(medicine);

        // when
        prescription.removeMedicine(medicine);

        // then
        assertThat(prescription.getMedicines()).isEmpty();
    }

    @Test
    @DisplayName("여러 약품을 추가할 수 있다")
    void addMultipleMedicinesTest() {
        // given
        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("감기")
                .build();

        PrescriptionMed medicine1 = PrescriptionMed.builder()
                .medicineName("타이레놀 500mg")
                .dosage("1정")
                .frequency("1일 3회")
                .build();

        PrescriptionMed medicine2 = PrescriptionMed.builder()
                .medicineName("콧물약")
                .dosage("1포")
                .frequency("1일 2회")
                .build();

        PrescriptionMed medicine3 = PrescriptionMed.builder()
                .medicineName("기침약")
                .dosage("1정")
                .frequency("1일 3회")
                .build();

        // when
        prescription.addMedicine(medicine1);
        prescription.addMedicine(medicine2);
        prescription.addMedicine(medicine3);

        // then
        assertThat(prescription.getMedicines()).hasSize(3);
        assertThat(prescription.getMedicines()).containsExactly(medicine1, medicine2, medicine3);
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
    @DisplayName("처방전 생성 시 medicines는 빈 리스트다")
    void defaultMedicinesIsEmptyListTest() {
        // given & when
        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("감기")
                .build();

        // then
        assertThat(prescription.getMedicines()).isNotNull();
        assertThat(prescription.getMedicines()).isEmpty();
    }

    @Test
    @DisplayName("약품 추가 시 양방향 연관관계가 설정된다")
    void bidirectionalRelationshipTest() {
        // given
        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("감기")
                .build();

        PrescriptionMed medicine = PrescriptionMed.builder()
                .medicineName("타이레놀 500mg")
                .dosage("1정")
                .frequency("1일 3회")
                .build();

        // when
        prescription.addMedicine(medicine);

        // then
        assertThat(medicine.getPrescription()).isEqualTo(prescription);
        assertThat(prescription.getMedicines()).contains(medicine);
    }
}
