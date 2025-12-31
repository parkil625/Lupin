package com.example.demo.dto.prescription;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.Prescription;
import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PrescriptionResponse 테스트")
class PrescriptionResponseTest {

    @Test
    @DisplayName("정상적인 처방전 엔티티를 DTO로 변환")
    void from_WithValidPrescription_ShouldReturnDto() {
        // given
        User patient = User.builder()
                .id(1L)
                .name("환자이름")
                .build();

        User doctor = User.builder()
                .id(2L)
                .name("의사이름")
                .build();

        Appointment appointment = Appointment.builder()
                .id(3L)
                .departmentName("내과")
                .build();

        Prescription prescription = Prescription.builder()
                .id(100L)
                .patient(patient)
                .doctor(doctor)
                .appointment(appointment)
                .diagnosis("감기")
                .date(LocalDate.of(2025, 12, 27))
                .build();

        // when
        PrescriptionResponse response = PrescriptionResponse.from(prescription);

        // then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getPatientId()).isEqualTo(1L);
        assertThat(response.getPatientName()).isEqualTo("환자이름");
        assertThat(response.getDoctorId()).isEqualTo(2L);
        assertThat(response.getDoctorName()).isEqualTo("의사이름");
        assertThat(response.getDepartmentName()).isEqualTo("내과");
        assertThat(response.getAppointmentId()).isEqualTo(3L);
        assertThat(response.getDiagnosis()).isEqualTo("감기");
        assertThat(response.getMedicineDetails()).isEmpty(); // No medicines added
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2025, 12, 27));
    }

    @Test
    @DisplayName("환자 정보가 null인 경우 기본값으로 처리")
    void from_WithNullPatient_ShouldUseDefaultValues() {
        // given
        User doctor = User.builder()
                .id(2L)
                .name("의사이름")
                .build();

        Prescription prescription = Prescription.builder()
                .id(100L)
                .patient(null)
                .doctor(doctor)
                .appointment(null)
                .diagnosis("감기")
                .date(LocalDate.of(2025, 12, 27))
                .build();

        // when
        PrescriptionResponse response = PrescriptionResponse.from(prescription);

        // then
        assertThat(response.getPatientId()).isNull();
        assertThat(response.getPatientName()).isEqualTo("알 수 없음");
        assertThat(response.getDoctorId()).isEqualTo(2L);
        assertThat(response.getDoctorName()).isEqualTo("의사이름");
    }

    @Test
    @DisplayName("의사 정보가 null인 경우 기본값으로 처리")
    void from_WithNullDoctor_ShouldUseDefaultValues() {
        // given
        User patient = User.builder()
                .id(1L)
                .name("환자이름")
                .build();

        Prescription prescription = Prescription.builder()
                .id(100L)
                .patient(patient)
                .doctor(null)
                .appointment(null)
                .diagnosis("감기")
                .date(LocalDate.of(2025, 12, 27))
                .build();

        // when
        PrescriptionResponse response = PrescriptionResponse.from(prescription);

        // then
        assertThat(response.getPatientId()).isEqualTo(1L);
        assertThat(response.getPatientName()).isEqualTo("환자이름");
        assertThat(response.getDoctorId()).isNull();
        assertThat(response.getDoctorName()).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("예약 정보가 null인 경우 처리")
    void from_WithNullAppointment_ShouldHandleGracefully() {
        // given
        User patient = User.builder()
                .id(1L)
                .name("환자이름")
                .build();

        User doctor = User.builder()
                .id(2L)
                .name("의사이름")
                .build();

        Prescription prescription = Prescription.builder()
                .id(100L)
                .patient(patient)
                .doctor(doctor)
                .appointment(null)
                .diagnosis("감기")
                .date(LocalDate.of(2025, 12, 27))
                .build();

        // when
        PrescriptionResponse response = PrescriptionResponse.from(prescription);

        // then
        assertThat(response.getDepartmentName()).isNull();
        assertThat(response.getAppointmentId()).isNull();
    }

    @Test
    @DisplayName("환자와 의사 모두 null인 경우 처리")
    void from_WithBothPatientAndDoctorNull_ShouldUseDefaultValues() {
        // given
        Prescription prescription = Prescription.builder()
                .id(100L)
                .patient(null)
                .doctor(null)
                .appointment(null)
                .diagnosis("감기")
                .date(LocalDate.of(2025, 12, 27))
                .build();

        // when
        PrescriptionResponse response = PrescriptionResponse.from(prescription);

        // then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getPatientId()).isNull();
        assertThat(response.getPatientName()).isEqualTo("알 수 없음");
        assertThat(response.getDoctorId()).isNull();
        assertThat(response.getDoctorName()).isEqualTo("알 수 없음");
        assertThat(response.getDepartmentName()).isNull();
        assertThat(response.getAppointmentId()).isNull();
        assertThat(response.getDiagnosis()).isEqualTo("감기");
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2025, 12, 27));
    }

    @Test
    @DisplayName("진단명이 null인 경우 처리")
    void from_WithNullDiagnosis_ShouldHandleGracefully() {
        // given
        User patient = User.builder()
                .id(1L)
                .name("환자이름")
                .build();

        User doctor = User.builder()
                .id(2L)
                .name("의사이름")
                .build();

        Prescription prescription = Prescription.builder()
                .id(100L)
                .patient(patient)
                .doctor(doctor)
                .appointment(null)
                .diagnosis(null)
                .date(LocalDate.of(2025, 12, 27))
                .build();

        // when
        PrescriptionResponse response = PrescriptionResponse.from(prescription);

        // then
        assertThat(response.getDiagnosis()).isNull();
    }

    @Test
    @DisplayName("날짜가 null인 경우 처리")
    void from_WithNullDate_ShouldHandleGracefully() {
        // given
        User patient = User.builder()
                .id(1L)
                .name("환자이름")
                .build();

        User doctor = User.builder()
                .id(2L)
                .name("의사이름")
                .build();

        Prescription prescription = Prescription.builder()
                .id(100L)
                .patient(patient)
                .doctor(doctor)
                .appointment(null)
                .diagnosis("감기")
                .date(null)
                .build();

        // when
        PrescriptionResponse response = PrescriptionResponse.from(prescription);

        // then
        assertThat(response.getDate()).isNull();
        assertThat(response.getDiagnosis()).isEqualTo("감기");
    }
}
