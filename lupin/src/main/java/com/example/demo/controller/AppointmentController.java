package com.example.demo.controller;
import com.example.demo.domain.entity.Appointment;
import com.example.demo.dto.request.AppointmentRequest;
import com.example.demo.dto.response.AppointmentResponse;
import com.example.demo.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity; 
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/appointment")
public class AppointmentController {

    private final  AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<Long> createAppointment(@Valid @RequestBody AppointmentRequest request){
            log.info("예약 요청 데이터 확인 - patientId: {}, doctorId: {}, date: {}",
                    request.getPatientId(), request.getDoctorId(), request.getDate());

            Long appointmentId = appointmentService.createAppointment(request);
            return ResponseEntity.ok(appointmentId);

            // 🌟 여기에 덫을 놓습니다! 에러가 나면 무조건 콘솔에 빨간 줄로 사연을 출력합니다.
            log.error("❌ 예약 생성 중 치명적인 에러 발생!", e);
            e.printStackTrace(); // 콘솔에 상세 내용 강제 출력
            throw e; // 에러를 다시 던져서 500 응답 유지
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<AppointmentResponse>> getPatientAppointments(
            @PathVariable("doctorId") Long doctorId){

        List<AppointmentResponse> list = appointmentService.getDoctorAppointments(doctorId)
                .stream()
                .map(AppointmentResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<String> cancelAppointment(@PathVariable Long appointmentId) {
        appointmentService.cancelAppointment(appointmentId);
        return ResponseEntity.ok("예약이 취소되었습니다.");
    }
}
