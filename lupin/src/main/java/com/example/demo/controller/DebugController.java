package com.example.demo.controller;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Debug", description = "디버깅용 API")
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final AppointmentRepository appointmentRepository;

    @Operation(summary = "예약 상세 정보 조회 (디버깅용)", description = "예약의 상태, 담당 의사, 환자 정보를 조회합니다.")
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<Map<String, Object>> getAppointmentDebugInfo(
            @PathVariable Long appointmentId,
            @CurrentUser User currentUser
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("currentUserId", currentUser.getId());
        result.put("currentUserRole", currentUser.getRole().name());
        result.put("currentUserName", currentUser.getName());

        Appointment appointment = appointmentRepository.findByIdWithPatientAndDoctor(appointmentId)
                .orElse(null);

        if (appointment == null) {
            result.put("appointmentFound", false);
            result.put("message", "예약을 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        result.put("appointmentFound", true);
        result.put("appointmentId", appointment.getId());
        result.put("appointmentStatus", appointment.getStatus().name());
        result.put("appointmentDate", appointment.getDate().toString());

        result.put("doctorId", appointment.getDoctor().getId());
        result.put("doctorName", appointment.getDoctor().getName());
        result.put("doctorRole", appointment.getDoctor().getRole().name());

        result.put("patientId", appointment.getPatient().getId());
        result.put("patientName", appointment.getPatient().getName());

        result.put("isCurrentUserDoctor", currentUser.getId().equals(appointment.getDoctor().getId()));
        result.put("canCreatePrescription",
            currentUser.getId().equals(appointment.getDoctor().getId()) &&
            (appointment.getStatus().name().equals("IN_PROGRESS") ||
             appointment.getStatus().name().equals("COMPLETED"))
        );

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "현재 로그인 사용자 정보", description = "JWT 토큰에서 추출한 현재 사용자 정보를 반환합니다.")
    @GetMapping("/current-user")
    public ResponseEntity<Map<String, Object>> getCurrentUserInfo(@CurrentUser User currentUser) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", currentUser.getId());
        result.put("userLoginId", currentUser.getUserId());
        result.put("userName", currentUser.getName());
        result.put("userRole", currentUser.getRole().name());
        return ResponseEntity.ok(result);
    }
}
