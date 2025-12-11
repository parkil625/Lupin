package com.example.demo.controller;

import com.example.demo.dto.response.DoctorResponse;
import com.example.demo.service.DoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    /**
     * 모든 의사 목록 조회
     * 또는 진료과별 의사 목록 조회 (specialty 파라미터 있을 경우)
     */
    @GetMapping
    public ResponseEntity<List<DoctorResponse>> getDoctors(
            @RequestParam(required = false) String specialty
    ) {
        List<DoctorResponse> doctors;

        if (specialty != null && !specialty.isEmpty()) {
            log.info("진료과별 의사 목록 조회: {}", specialty);
            doctors = doctorService.getDoctorsBySpecialty(specialty);
        } else {
            log.info("전체 의사 목록 조회");
            doctors = doctorService.getAllDoctors();
        }

        return ResponseEntity.ok(doctors);
    }

    /**
     * 의사 ID로 상세 정보 조회
     */
    @GetMapping("/{doctorId}")
    public ResponseEntity<DoctorResponse> getDoctorById(@PathVariable Long doctorId) {
        log.info("의사 상세 정보 조회: {}", doctorId);
        DoctorResponse doctor = doctorService.getDoctorById(doctorId);
        return ResponseEntity.ok(doctor);
    }
}
