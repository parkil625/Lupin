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
                .name("타이레놀")
                .description("해열진통제")
                .build();

        // when
        MedicineResponse response = MedicineResponse.from(medicine);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("타이레놀");
        assertThat(response.getDescription()).isEqualTo("해열진통제");
    }

    @Test
    @DisplayName("설명이 null인 경우 처리")
    void from_WithNullDescription_ShouldHandleGracefully() {
        // given
        Medicine medicine = Medicine.builder()
                .id(1L)
                .name("타이레놀")
                .description(null)
                .build();

        // when
        MedicineResponse response = MedicineResponse.from(medicine);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("타이레놀");
        assertThat(response.getDescription()).isNull();
    }
}
