package com.example.demo.dto.response;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AppointmentResponse 테스트")
class AppointmentResponseTest {

    @Test
    @DisplayName("의사의 진료과목이 응답에 포함되어야 함 - 내과")
    void from_ShouldIncludeDepartmentName_InternalMedicine() {
        // Given
        User patient = User.builder()
                .id(1L)
                .userId("patient01")
                .name("환자1")
                .role(Role.MEMBER)
                .build();

        User doctor = User.builder()
                .id(21L)
                .userId("doctor01")
                .name("김의사")
                .role(Role.DOCTOR)
                .department("내과")
                .build();

        Appointment appointment = Appointment.builder()
                .id(1L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 1, 14, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        // When
        AppointmentResponse response = AppointmentResponse.from(appointment);

        // Then
        assertThat(response.getDepartmentName()).isEqualTo("내과 진료");
    }

    @Test
    @DisplayName("의사의 진료과목이 응답에 포함되어야 함 - 외과")
    void from_ShouldIncludeDepartmentName_Surgery() {
        // Given
        User patient = User.builder()
                .id(1L)
                .userId("patient01")
                .name("환자1")
                .role(Role.MEMBER)
                .build();

        User doctor = User.builder()
                .id(22L)
                .userId("doctor02")
                .name("이의사")
                .role(Role.DOCTOR)
                .department("외과")
                .build();

        Appointment appointment = Appointment.builder()
                .id(2L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 2, 15, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        // When
        AppointmentResponse response = AppointmentResponse.from(appointment);

        // Then
        assertThat(response.getDepartmentName()).isEqualTo("외과 진료");
    }

    @Test
    @DisplayName("의사의 진료과목이 null인 경우 빈 문자열 반환")
    void from_ShouldReturnEmptyString_WhenDepartmentIsNull() {
        // Given
        User patient = User.builder()
                .id(1L)
                .userId("patient01")
                .name("환자1")
                .role(Role.MEMBER)
                .build();

        User doctor = User.builder()
                .id(23L)
                .userId("doctor03")
                .name("박의사")
                .role(Role.DOCTOR)
                .department(null)
                .build();

        Appointment appointment = Appointment.builder()
                .id(3L)
                .patient(patient)
                .doctor(doctor)
                .date(LocalDateTime.of(2025, 12, 3, 16, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();

        // When
        AppointmentResponse response = AppointmentResponse.from(appointment);

        // Then
        assertThat(response.getDepartmentName()).isEqualTo("");
    }
}
