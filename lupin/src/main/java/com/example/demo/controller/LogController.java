package com.example.demo.controller;

import com.example.demo.util.LogCapture;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Log", description = "로그 조회 API (디버깅용)")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogCapture logCapture;

    @Operation(summary = "최근 로그 조회", description = "최근 N개의 로그를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<String>> getLogs(@RequestParam(defaultValue = "100") int count) {
        return ResponseEntity.ok(logCapture.getLastLogs(count));
    }

    @Operation(summary = "모든 로그 조회", description = "저장된 모든 로그를 조회합니다.")
    @GetMapping("/all")
    public ResponseEntity<List<String>> getAllLogs() {
        return ResponseEntity.ok(logCapture.getLogs());
    }

    @Operation(summary = "로그 삭제", description = "저장된 모든 로그를 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<String> clearLogs() {
        logCapture.clear();
        return ResponseEntity.ok("로그가 삭제되었습니다.");
    }
}
