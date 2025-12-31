package com.example.demo.controller;

import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.NotificationSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationSseController {

    private final NotificationSseService notificationSseService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * SSE 구독 엔드포인트
     * GET /api/notifications/subscribe?token=xxx
     * SSE는 Authorization 헤더를 지원하지 않으므로 토큰을 쿼리 파라미터로 받음
     *
     * @param token JWT 토큰
     * @param lastEventId 마지막으로 받은 이벤트 ID (재연결 시 브라우저가 자동 전송)
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(
            @RequestParam("token") String token,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

        log.info("[SSE Debug] 구독 요청 수신: token_exists={}, lastEventId={}", 
                (token != null), lastEventId);

        // "Bearer " 접두사가 있다면 제거
        String resolvedToken = (token != null && token.startsWith("Bearer ")) ? token.substring(7) : token;

        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(resolvedToken)) {
            log.warn("[SSE Debug] 구독 실패: 유효하지 않은 토큰");
            return ResponseEntity.status(401).body(null);
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(resolvedToken);
        String userId = authentication.getName();

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Last-Event-ID 파싱
        Long lastEventIdLong = parseLastEventId(lastEventId);

        log.info("[SSE Debug] 서비스 구독 시작: userId={}, lastEventId={}", user.getId(), lastEventIdLong);
        SseEmitter emitter = notificationSseService.subscribe(user.getId(), lastEventIdLong);

        // [절대 수정 금지] SSE 연결 끊김 및 버퍼링 방지를 위한 필수 헤더 설정 (이 부분은 수정하거나 삭제하지 마세요!)
        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no") // Nginx/Cloudflare 버퍼링 방지 (필수)
                .header("Cache-Control", "no-cache, no-transform") // 캐시 방지
                .header("Connection", "keep-alive") // 연결 유지 (HTTP/1.1 호환)
                .header("Content-Type", "text/event-stream;charset=UTF-8") // 인코딩 명시
                .body(emitter);
    }

    private Long parseLastEventId(String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(lastEventId);
        } catch (NumberFormatException e) {
            log.warn("Last-Event-ID 파싱 실패: {}", lastEventId);
            return null;
        }
    }

    /**
     * SSE 연결 해제
     * DELETE /api/notifications/subscribe
     */
    @DeleteMapping("/subscribe")
    public void unsubscribe(@CurrentUser User user) {
        notificationSseService.disconnect(user.getId());
    }
}
