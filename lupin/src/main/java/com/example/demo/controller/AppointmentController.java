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
@RequestMapping()
public class AppointmentController {

    private final  AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<Long> createAppointment(@Valid @RequestBody AppointmentRequest request){
        log.info("예약 요청- 환자ID : {}, 의사ID : {}, 시간 : {}",
                request.getPatientId(), request.getDoctorId(),request.getDate());
        Long appointmentId = appointmentService.createAppointment(request);

        return ResponseEntity.ok(appointmentId);
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Appointment>> getPatientAppointments(
            @PathVariable("doctorId") Long doctorId){

        List<AppointmentResponse> list = appointmentService.getDoctorAppointments(doctorId)
                .stream()
                .map(AppointmentResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);

        @PutMapping("/{appointmentId}/cancel")
        public ResponseEntity<String> cancelAppointment(@PathVariable Long appointmentId) {
            appointmentService.cancelAppointment(appointmentId);
            return ResponseEntity.ok("예약이 취소되었습니다.");
        }
    }
}
