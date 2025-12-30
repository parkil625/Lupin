package com.example.demo.dto.prescription;

import com.example.demo.domain.entity.Medicine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String precautions;

    public static MedicineResponse from(Medicine medicine) {
        return MedicineResponse.builder()
                .id(medicine.getId())
                .code(medicine.getCode())
                .name(medicine.getName())
                .description(medicine.getDescription())
                .precautions(medicine.getPrecautions())
                .build();
    }
}
