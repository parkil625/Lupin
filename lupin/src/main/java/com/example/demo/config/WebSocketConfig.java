package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000});  // 10초마다 heartbeat
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:3000",            // 로컬 개발용
                        "http://localhost:5173",            // Vite 개발 서버
                        "http://localhost:5174",            // Vite 대체 포트
                        "https://lupin-eosin.vercel.app",   // 현재 Vercel 도메인
                        "https://lupin-care.com",           // [추가됨] 나중에 쓸 실제 도메인 (슬래시 없음!)
                        "https://www.lupin-care.com"        // [추가됨] www 붙은 버전도 혹시 모르니 추가
                )
                .withSockJS()
                .setStreamBytesLimit(512 * 1024)         // 512KB 메시지 크기 제한
                .setHttpMessageCacheSize(1000)           // HTTP 메시지 캐시 크기
                .setDisconnectDelay(30 * 1000)           // 30초 연결 끊김 대기
                .setHeartbeatTime(25 * 1000);            // 25초마다 heartbeat
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(128 * 1024)         // 128KB 메시지 크기
                .setSendBufferSizeLimit(512 * 1024)      // 512KB 전송 버퍼
                .setSendTimeLimit(20 * 1000)             // 20초 전송 타임아웃
                .setTimeToFirstMessage(30 * 1000);       // 첫 메시지 대기 시간 30초
    }
}