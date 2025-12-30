package com.example.demo.dto.prescription;

import com.example.demo.domain.entity.Medicine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MedicineResponse 테스트")
class MedicineResponseTest {

    @Test
    @DisplayName("정상적인 약품 엔티티를 DTO로 변환")
    void from_WithValidMedicine_ShouldReturnDto() {
        // given
        Medicine medicine = Medicine.builder()
                .id(1L)
                .code("A01BC01")
                .name("타이레놀")
                .description("해열진통제")
                .precautions("공복 시 복용 주의")
                .build();

        // when
        MedicineResponse response = MedicineResponse.from(medicine);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCode()).isEqualTo("A01BC01");
        assertThat(response.getDescription()).isEqualTo("해열진통제");
        assertThat(response.getPrecautions()).isEqualTo("공복 시 복용 주의");
    }

    @Test
    @DisplayName("설명이 null인 경우 처리")
    void from_WithNullDescription_ShouldHandleGracefully() {
        // given
        Medicine medicine = Medicine.builder()
                .id(1L)
                .code("A01BC01")
                .name("타이레놀")
                .description(null)
                .precautions("공복 시 복용 주의")
                .build();

        // when
        MedicineResponse response = MedicineResponse.from(medicine);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCode()).isEqualTo("A01BC01");
        assertThat(response.getDescription()).isNull();
        assertThat(response.getPrecautions()).isEqualTo("공복 시 복용 주의");
    }

    @Test
    @DisplayName("주의사항이 null인 경우 처리")
    void from_WithNullPrecautions_ShouldHandleGracefully() {
        // given
        Medicine medicine = Medicine.builder()
                .id(1L)
                .code("A01BC01")
                .name("타이레놀")
                .description("해열진통제")
                .precautions(null)
                .build();

        // when
        MedicineResponse response = MedicineResponse.from(medicine);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCode()).isEqualTo("A01BC01");
        assertThat(response.getDescription()).isEqualTo("해열진통제");
        assertThat(response.getPrecautions()).isNull();
    }

    @Test
    @DisplayName("설명과 주의사항 모두 null인 경우 처리")
    void from_WithBothDescriptionAndPrecautionsNull_ShouldHandleGracefully() {
        // given
        Medicine medicine = Medicine.builder()
                .id(1L)
                .code("A01BC01")
                .name("타이레놀")
                .description(null)
                .precautions(null)
                .build();

        // when
        MedicineResponse response = MedicineResponse.from(medicine);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCode()).isEqualTo("A01BC01");
        assertThat(response.getDescription()).isNull();
        assertThat(response.getPrecautions()).isNull();
    }
}
