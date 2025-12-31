package com.example.demo.dto.prescription;

import com.example.demo.domain.entity.Medicine;
import com.example.demo.domain.entity.PrescriptionMedicine;
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
    private Long medicineId;      // 약품 ID
    private String name;          // 약품명
    private String precautions;   // 주의사항

    public static PrescriptionMedicineDto from(PrescriptionMedicine pm) {
        Medicine medicine = pm.getMedicine();
        return PrescriptionMedicineDto.builder()
                .medicineId(medicine != null ? medicine.getId() : null)
                .name(medicine != null ? medicine.getName() : "알 수 없음")
                .precautions(medicine != null ? medicine.getPrecautions() : null)
                .build();
    }
}
