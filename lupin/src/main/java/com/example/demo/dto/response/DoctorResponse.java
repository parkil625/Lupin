package com.example.demo.dto.response;

import com.example.demo.domain.entity.DoctorProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse {

    private Long id; // User ID
    private String userId; // 로그인 ID
    private String name; // 의사 이름
    private String specialty; // 전공 (내과, 외과 등)
    private String licenseNumber; // 면허번호
    private Integer medicalExperience; // 경력 (년)
    private String phone; // 연락처
    private LocalDate birthDate; // 생년월일
    private String gender; // 성별
    private String address; // 주소

    public static DoctorResponse from(DoctorProfile doctorProfile) {
        return DoctorResponse.builder()
                .id(doctorProfile.getUser().getId())
                .userId(doctorProfile.getUser().getUserId())
                .name(doctorProfile.getUser().getName())
                .specialty(doctorProfile.getSpecialty())
                .licenseNumber(doctorProfile.getLicenseNumber())
                .medicalExperience(doctorProfile.getMedicalExperience())
                .phone(doctorProfile.getPhone())
                .birthDate(doctorProfile.getBirthDate())
                .gender(doctorProfile.getGender())
                .address(doctorProfile.getAddress())
                .build();
    }
}
