package com.example.demo.controller;

import com.example.demo.dto.request.PrescriptionCreateRequest;
import com.example.demo.dto.response.PrescriptionResponse;
import com.example.demo.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 처방전 관련 API
 */
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    /**
     * 처방전 생성
     */
    @PostMapping
    public ResponseEntity<PrescriptionResponse> createPrescription(
            @Valid @RequestBody PrescriptionCreateRequest request) {
        PrescriptionResponse response = prescriptionService.createPrescription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 환자의 처방전 목록 조회 (페이징)
     */
    @GetMapping("/patients/{patientId}")
    public ResponseEntity<Page<PrescriptionResponse>> getPrescriptionsByPatientId(
            @PathVariable Long patientId,
            @PageableDefault(size = 20, sort = "prescribedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PrescriptionResponse> prescriptions = prescriptionService.getPrescriptionsByPatientId(patientId, pageable);
        return ResponseEntity.ok(prescriptions);
    }

    /**
     * 특정 환자의 처방전 목록 조회 (전체)
     */
    @GetMapping("/patients/{patientId}/all")
    public ResponseEntity<List<PrescriptionResponse>> getAllPrescriptionsByPatientId(@PathVariable Long patientId) {
        List<PrescriptionResponse> prescriptions = prescriptionService.getAllPrescriptionsByPatientId(patientId);
        return ResponseEntity.ok(prescriptions);
    }

    /**
     * 특정 의사가 발행한 처방전 목록 조회 (페이징)
     */
    @GetMapping("/doctors/{doctorId}")
    public ResponseEntity<Page<PrescriptionResponse>> getPrescriptionsByDoctorId(
            @PathVariable Long doctorId,
            @PageableDefault(size = 20, sort = "prescribedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PrescriptionResponse> prescriptions = prescriptionService.getPrescriptionsByDoctorId(doctorId, pageable);
        return ResponseEntity.ok(prescriptions);
    }

    /**
     * 특정 의사가 발행한 처방전 목록 조회 (전체)
     */
    @GetMapping("/doctors/{doctorId}/all")
    public ResponseEntity<List<PrescriptionResponse>> getAllPrescriptionsByDoctorId(@PathVariable Long doctorId) {
        List<PrescriptionResponse> prescriptions = prescriptionService.getAllPrescriptionsByDoctorId(doctorId);
        return ResponseEntity.ok(prescriptions);
    }

    /**
     * 처방전 상세 조회
     */
    @GetMapping("/{prescriptionId}")
    public ResponseEntity<PrescriptionResponse> getPrescriptionDetail(@PathVariable Long prescriptionId) {
        PrescriptionResponse prescription = prescriptionService.getPrescriptionDetail(prescriptionId);
        return ResponseEntity.ok(prescription);
    }

    /**
     * 특정 환자의 최근 처방전 조회
     */
    @GetMapping("/patients/{patientId}/recent")
    public ResponseEntity<List<PrescriptionResponse>> getRecentPrescriptionsByPatientId(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "5") int limit) {
        List<PrescriptionResponse> prescriptions = prescriptionService.getRecentPrescriptionsByPatientId(patientId, limit);
        return ResponseEntity.ok(prescriptions);
    }

    /**
     * 특정 기간 내 처방전 조회
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<PrescriptionResponse> prescriptions = prescriptionService.getPrescriptionsByDateRange(startDate, endDate);
        return ResponseEntity.ok(prescriptions);
    }

    /**
     * 처방전 이름으로 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Page<PrescriptionResponse>> searchPrescriptionsByName(
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "prescribedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PrescriptionResponse> prescriptions = prescriptionService.searchPrescriptionsByName(keyword, pageable);
        return ResponseEntity.ok(prescriptions);
    }

    /**
     * 처방전 삭제
     */
    @DeleteMapping("/{prescriptionId}")
    public ResponseEntity<Void> deletePrescription(
            @PathVariable Long prescriptionId,
            @RequestParam Long doctorId) {
        prescriptionService.deletePrescription(prescriptionId, doctorId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 특정 환자의 처방전 수 조회
     */
    @GetMapping("/patients/{patientId}/count")
    public ResponseEntity<Long> getPrescriptionCountByPatientId(@PathVariable Long patientId) {
        Long count = prescriptionService.getPrescriptionCountByPatientId(patientId);
        return ResponseEntity.ok(count);
    }
}
