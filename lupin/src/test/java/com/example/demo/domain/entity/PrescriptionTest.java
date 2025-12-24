package com.example.demo.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Prescription 엔티티 테스트")
class PrescriptionTest {

    @Test
    @DisplayName("처방전 생성 시 medications 필드에 약물 정보를 저장할 수 있다")
    void createPrescriptionWithMedicationsTest() {
        // given
        String medications = "타이레놀 500mg (1정, 1일 3회)\n콧물약 (1포, 1일 2회)";

        // when
        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("감기")
                .medications(medications)
                .build();

        // then
        assertThat(prescription.getMedications()).isEqualTo(medications);
        assertThat(prescription.getMedications()).contains("타이레놀 500mg");
        assertThat(prescription.getMedications()).contains("콧물약");
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
                .medications("타이레놀 500mg (1정, 1일 3회)")
                .build();

        // when
        prescription.updateDiagnosis("급성 상기도 감염");

        // then
        assertThat(prescription.getDiagnosis()).isEqualTo("급성 상기도 감염");
    }

    @Test
    @DisplayName("처방전 생성 시 medications는 null일 수 있다")
    void medicationsCanBeNullTest() {
        // given & when
        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("경과 관찰")
                .medications(null)
                .build();

        // then
        assertThat(prescription.getMedications()).isNull();
    }

    @Test
    @DisplayName("처방전 생성 시 medications는 빈 문자열일 수 있다")
    void medicationsCanBeEmptyStringTest() {
        // given & when
        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("경과 관찰")
                .medications("")
                .build();

        // then
        assertThat(prescription.getMedications()).isEmpty();
    }

    @Test
    @DisplayName("여러 약품을 줄바꿈으로 구분하여 저장할 수 있다")
    void multipleMedicinesWithNewlineTest() {
        // given
        String medications = "타이레놀 500mg (1정, 1일 3회)\n콧물약 (1포, 1일 2회)\n기침약 (1정, 1일 3회)";

        // when
        Prescription prescription = Prescription.builder()
                .patient(User.builder().id(1L).build())
                .doctor(User.builder().id(21L).build())
                .date(LocalDate.now())
                .diagnosis("감기")
                .medications(medications)
                .build();

        // then
        assertThat(prescription.getMedications()).isEqualTo(medications);
        String[] medicineArray = prescription.getMedications().split("\n");
        assertThat(medicineArray).hasSize(3);
        assertThat(medicineArray[0]).isEqualTo("타이레놀 500mg (1정, 1일 3회)");
        assertThat(medicineArray[1]).isEqualTo("콧물약 (1포, 1일 2회)");
        assertThat(medicineArray[2]).isEqualTo("기침약 (1정, 1일 3회)");
    }
}
