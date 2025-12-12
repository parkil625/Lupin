package com.example.demo.controller;

import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 테스트 데이터 정리용 컨트롤러
 * 개발/테스트 환경에서만 사용
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class TestDataCleanupController {

    private final ChatRepository chatRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * 모든 채팅 메시지 삭제
     * DELETE /api/test/cleanup/messages
     */
    @DeleteMapping("/cleanup/messages")
    public ResponseEntity<Map<String, Object>> deleteAllMessages() {
        log.warn("⚠️  모든 채팅 메시지를 삭제합니다");

        long countBefore = chatRepository.count();
        chatRepository.deleteAll();
        long countAfter = chatRepository.count();

        Map<String, Object> result = new HashMap<>();
        result.put("deleted", countBefore);
        result.put("remaining", countAfter);

        log.info("✅ 채팅 메시지 삭제 완료: {} → {}", countBefore, countAfter);

        return ResponseEntity.ok(result);
    }

    /**
     * 의사 ID 22-25의 예약 삭제
     * DELETE /api/test/cleanup/appointments
     */
    @DeleteMapping("/cleanup/appointments")
    public ResponseEntity<Map<String, Object>> deleteTestAppointments() {
        log.warn("⚠️  테스트 예약 데이터를 삭제합니다 (doctor_id: 22-25)");

        long countBefore = appointmentRepository.count();

        // 의사 ID 22-25의 예약만 삭제
        appointmentRepository.deleteAll(
            appointmentRepository.findAll().stream()
                .filter(a -> a.getDoctor().getId() >= 22 && a.getDoctor().getId() <= 25)
                .toList()
        );

        long countAfter = appointmentRepository.count();

        Map<String, Object> result = new HashMap<>();
        result.put("deleted", countBefore - countAfter);
        result.put("remaining", countAfter);

        log.info("✅ 예약 데이터 삭제 완료: {} → {}", countBefore, countAfter);

        return ResponseEntity.ok(result);
    }

    /**
     * 모든 테스트 데이터 삭제 (채팅 + 예약)
     * DELETE /api/test/cleanup/all
     */
    @DeleteMapping("/cleanup/all")
    public ResponseEntity<Map<String, Object>> deleteAllTestData() {
        log.warn("⚠️⚠️  모든 테스트 데이터를 삭제합니다");

        Map<String, Object> messages = deleteAllMessages().getBody();
        Map<String, Object> appointments = deleteTestAppointments().getBody();

        Map<String, Object> result = new HashMap<>();
        result.put("messages", messages);
        result.put("appointments", appointments);

        log.info("✅✅ 모든 테스트 데이터 삭제 완료");

        return ResponseEntity.ok(result);
    }
}
