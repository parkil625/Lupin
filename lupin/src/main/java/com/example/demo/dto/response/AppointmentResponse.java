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
                .build();
    }

}
