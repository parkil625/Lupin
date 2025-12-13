package com.example.demo.dto.response;

import com.example.demo.domain.entity.DoctorProfile;
import com.example.demo.domain.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DoctorResponse {
    private Long id;
    private String name;
    private String department;

    public static DoctorResponse from(User doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .name(doctor.getName())
                .department(doctor.getDepartment() != null ? doctor.getDepartment() : "")
                .build();
    }

    public static DoctorResponse from(DoctorProfile doctorProfile) {
        User user = doctorProfile.getUser();
        return DoctorResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .department(doctorProfile.getSpecialty() != null ? doctorProfile.getSpecialty() : "")
                .build();
    }
}
