package com.example.demo.dto.response;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.enums.AppointmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AppointmentResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String departmentName;
    private LocalDateTime date;
    private AppointmentStatus status;
    private String doctorAvatar;      // [N+1 해결] 의사 프로필 이미지
    private String doctorDepartment;  // [N+1 해결] 의사 진료과

    public static AppointmentResponse from(Appointment appointment) {
        String department = appointment.getDoctor().getDepartment();
        String departmentName = (department != null && !department.isEmpty())
            ? department + " 진료"
            : "";

        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatient().getId())
                .patientName(appointment.getPatient().getName())
                .doctorId(appointment.getDoctor().getId())
                .doctorName(appointment.getDoctor().getName())
                .departmentName(departmentName)
                .date(appointment.getDate())
                .status(appointment.getStatus())
                .doctorAvatar(appointment.getDoctor().getAvatar())           // [N+1 해결] JOIN FETCH로 이미 로드됨
                .doctorDepartment(appointment.getDoctor().getDepartment())   // [N+1 해결] JOIN FETCH로 이미 로드됨
                .build();
    }

}
