package com.example.demo.controller;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.prescription.MedicineResponse;
import com.example.demo.dto.prescription.PrescriptionRequest;
import com.example.demo.dto.prescription.PrescriptionResponse;
import com.example.demo.repository.MedicineRepository;
import com.example.demo.security.CurrentUser;
import com.example.demo.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Prescription", description = "처방전 관리 API")
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final MedicineRepository medicineRepository;

    @Operation(summary = "처방전 발급", description = "의사가 환자에게 처방전을 발급합니다.")
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponse> createPrescription(
            @CurrentUser User currentUser,
            @Valid @RequestBody PrescriptionRequest request
    ) {
        System.out.println("=== 처방전 발급 요청 시작 ===");
        System.out.println("의사 ID: " + currentUser.getId());
        System.out.println("요청 데이터: " + request);
        System.out.println("약품 목록:");
        if (request.getMedicines() != null) {
            for (int i = 0; i < request.getMedicines().size(); i++) {
                var med = request.getMedicines().get(i);
                System.out.println("  [" + i + "] " + med.getMedicineName() +
                    " - dosage: " + med.getDosage() +
                    ", freq: " + med.getFrequency() +
                    ", duration: " + med.getDurationDays() +
                    ", instructions: " + med.getInstructions());
            }
        }

        try {
            PrescriptionResponse response = prescriptionService.createPrescription(
                    currentUser.getId(),
                    request
            );
            System.out.println("처방전 발급 성공: " + response.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("처방전 발급 실패: " + e.getClass().getName() + " - " + e.getMessage());
            System.err.println("스택 트레이스:");
            e.printStackTrace();
            throw e;
        }
    }

    @Operation(summary = "환자 처방전 목록 조회", description = "환자의 모든 처방전을 조회합니다.")
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('MEMBER') or hasRole('DOCTOR')")
    public ResponseEntity<List<PrescriptionResponse>> getPatientPrescriptions(
            @PathVariable Long patientId,
            @CurrentUser User currentUser
    ) {
        // 환자 본인이거나 의사만 조회 가능
        if (currentUser.getRole().name().equals("MEMBER") && !currentUser.getId().equals(patientId)) {
            return ResponseEntity.status(403).build();
        }

        List<PrescriptionResponse> prescriptions = prescriptionService.getPatientPrescriptions(patientId);
        return ResponseEntity.ok(prescriptions);
    }

    @Operation(summary = "의사 발급 처방전 목록 조회", description = "의사가 발급한 모든 처방전을 조회합니다.")
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<PrescriptionResponse>> getDoctorPrescriptions(
            @PathVariable Long doctorId,
            @CurrentUser User currentUser
    ) {
        // 본인의 처방전만 조회 가능
        if (!currentUser.getId().equals(doctorId)) {
            return ResponseEntity.status(403).build();
        }

        List<PrescriptionResponse> prescriptions = prescriptionService.getDoctorPrescriptions(doctorId);
        return ResponseEntity.ok(prescriptions);
    }

    @Operation(summary = "처방전 상세 조회", description = "처방전 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponse> getPrescription(@PathVariable Long id) {
        return prescriptionService.getPrescriptionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "예약별 처방전 조회", description = "예약 ID로 처방전을 조회합니다.")
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<PrescriptionResponse> getPrescriptionByAppointment(@PathVariable Long appointmentId) {
        return prescriptionService.getPrescriptionByAppointmentId(appointmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "약품 검색", description = "약품명으로 검색합니다.")
    @GetMapping("/medicines/search")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<MedicineResponse>> searchMedicines(@RequestParam String query) {
        List<MedicineResponse> medicines = medicineRepository
                .findByNameContainingIgnoreCase(query)
                .stream()
                .map(MedicineResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(medicines);
    }

    @Operation(summary = "전체 약품 목록 조회", description = "모든 약품을 조회합니다.")
    @GetMapping("/medicines")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<MedicineResponse>> getAllMedicines() {
        List<MedicineResponse> medicines = medicineRepository
                .findByOrderByNameAsc()
                .stream()
                .map(MedicineResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(medicines);
    }
}
