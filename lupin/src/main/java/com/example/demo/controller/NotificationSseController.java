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
    // [수정] HttpServletResponse를 주입받아 Nginx 버퍼링 방지 헤더 설정
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam("token") String token,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            jakarta.servlet.http.HttpServletResponse response) {

        // [핵심] Nginx에게 "이 응답은 버퍼링하지 말고 즉시 전송해!"라고 명령
        // Spring Boot 3.x 이상에서는 javax 대신 jakarta 패키지를 사용합니다.
        response.addHeader("X-Accel-Buffering", "no");

        // "Bearer " 접두사가 있다면 제거
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("SSE 구독 실패: 유효하지 않은 토큰");
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Invalid token"));
                emitter.complete();
            } catch (Exception e) {
                emitter.complete();
            }
            return emitter;
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        String userId = authentication.getName();

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Last-Event-ID를 Long으로 파싱 (없거나 파싱 실패 시 null)
        Long lastEventIdLong = parseLastEventId(lastEventId);

        log.info("SSE 구독 요청: userId={}, lastEventId={}", user.getId(), lastEventIdLong);
        return notificationSseService.subscribe(user.getId(), lastEventIdLong);
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
