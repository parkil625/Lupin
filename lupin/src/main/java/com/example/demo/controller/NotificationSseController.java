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
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam("token") String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            log.error("SSE 구독 실패: 유효하지 않은 토큰");
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("Invalid token"));
            return emitter;
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(token);
        String userId = authentication.getName();

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("SSE 구독 요청: userId={}", user.getId());
        return notificationSseService.subscribe(user.getId());
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
