package com.example.demo.dto.prescription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 처방전에 포함된 약품 정보
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionMedicineDto {
    private String name;          // 약품명
    private String precautions;   // 주의사항
}
