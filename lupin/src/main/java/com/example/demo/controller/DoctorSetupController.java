package com.example.demo.controller;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 의사 진료과 설정 컨트롤러 (개발용)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/setup")
public class DoctorSetupController {

    private final UserRepository userRepository;

    /**
     * 의사 진료과 일괄 설정
     * POST /api/setup/doctor-departments
     */
    @PostMapping("/doctor-departments")
    public ResponseEntity<Map<String, Object>> setupDoctorDepartments() {
        log.info("의사 진료과 설정 시작");

        Map<Long, String> departmentMapping = new HashMap<>();
        departmentMapping.put(22L, "internal");
        departmentMapping.put(23L, "surgery");
        departmentMapping.put(24L, "psychiatry");
        departmentMapping.put(25L, "dermatology");

        int updatedCount = 0;

        for (Map.Entry<Long, String> entry : departmentMapping.entrySet()) {
            Long doctorId = entry.getKey();
            String department = entry.getValue();

            User doctor = userRepository.findById(doctorId).orElse(null);
            if (doctor != null && doctor.getRole() == Role.DOCTOR) {
                doctor.setDepartment(department);
                userRepository.save(doctor);
                updatedCount++;
                log.info("의사 ID {}: {} 진료과 설정 완료", doctorId, department);
            }
        }

        // 결과 확인
        List<User> doctors = userRepository.findByRoleAndDepartment(Role.DOCTOR, "internal");
        log.info("내과 의사 수: {}", doctors.size());

        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", updatedCount);
        result.put("message", "의사 진료과 설정 완료");

        return ResponseEntity.ok(result);
    }

    /**
     * 현재 의사 목록 조회
     * GET /api/setup/doctors
     */
    @GetMapping("/doctors")
    public ResponseEntity<List<Map<String, Object>>> getDoctors() {
        List<User> doctors = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.DOCTOR)
                .toList();

        List<Map<String, Object>> result = doctors.stream()
                .map(d -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", d.getId());
                    map.put("name", d.getName());
                    map.put("department", d.getDepartment());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(result);
    }
}
