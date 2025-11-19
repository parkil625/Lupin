package com.example.demo.controller;

import com.example.demo.domain.entity.Appointment;
import com.example.demo.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 관련 API
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * 예약 생성
     */
    @PostMapping
    public ResponseEntity<Appointment> createAppointment(
            @RequestParam Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime apptDate,
            @RequestParam String reason) {
        Appointment appointment = appointmentService.createAppointment(patientId, doctorId, apptDate, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }

    /**
     * 환자의 예약 목록 조회
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getPatientAppointments(@PathVariable Long patientId) {
        List<Appointment> appointments = appointmentService.getPatientAppointments(patientId);
        return ResponseEntity.ok(appointments);
    }

    /**
     * 의사의 예약 목록 조회
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Appointment>> getDoctorAppointments(@PathVariable Long doctorId) {
        List<Appointment> appointments = appointmentService.getDoctorAppointments(doctorId);
        return ResponseEntity.ok(appointments);
    }

    /**
     * 예약 상세 조회
     */
    @GetMapping("/{appointmentId}")
    public ResponseEntity<Appointment> getAppointmentDetail(@PathVariable Long appointmentId) {
        Appointment appointment = appointmentService.getAppointmentDetail(appointmentId);
        return ResponseEntity.ok(appointment);
    }

    /**
     * 예약 완료 처리
     */
    @PostMapping("/{appointmentId}/complete")
    public ResponseEntity<Void> completeAppointment(@PathVariable Long appointmentId) {
        appointmentService.completeAppointment(appointmentId);
        return ResponseEntity.ok().build();
    }

    /**
     * 예약 취소
     */
    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long appointmentId) {
        appointmentService.cancelAppointment(appointmentId);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 기간의 예약 조회
     */
    @GetMapping("/between")
    public ResponseEntity<List<Appointment>> getAppointmentsBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<Appointment> appointments = appointmentService.getAppointmentsBetween(start, end);
        return ResponseEntity.ok(appointments);
    }
}
