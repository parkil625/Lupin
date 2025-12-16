package com.example.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data // [중요] Getter, Setter 자동 생성
@NoArgsConstructor
public class UserUpdateRequest {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("height")
    private Double height;
    
    @JsonProperty("weight")
    private Double weight;

    // [중요] JSON의 "1995-05-05"를 LocalDate로 변환하기 위한 설정
    @JsonProperty("birthDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
    
    @JsonProperty("gender")
    private String gender;
}