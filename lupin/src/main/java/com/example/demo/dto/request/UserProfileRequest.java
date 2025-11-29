package com.example.demo.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserProfileRequest {

    @Size(min = 1, max = 20, message = "이름은 1~20자로 입력해주세요")
    private String name;

    @DecimalMin(value = "50.0", message = "키는 50cm 이상이어야 합니다")
    @DecimalMax(value = "250.0", message = "키는 250cm 이하여야 합니다")
    private Double height;

    @DecimalMin(value = "20.0", message = "몸무게는 20kg 이상이어야 합니다")
    @DecimalMax(value = "300.0", message = "몸무게는 300kg 이하여야 합니다")
    private Double weight;

    @Builder
    public UserProfileRequest(String name, Double height, Double weight) {
        this.name = name;
        this.height = height;
        this.weight = weight;
    }
}
