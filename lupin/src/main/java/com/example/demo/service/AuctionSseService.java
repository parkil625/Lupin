package com.example.demo.service;

import com.example.demo.dto.AuctionSseMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AuctionSseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic topic;
    private final ObjectMapper objectMapper;

    public AuctionSseService(
            StringRedisTemplate redisTemplate,
            @Qualifier("auctionTopic") ChannelTopic topic,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.topic = topic;
        this.objectMapper = objectMapper;
    }


    public SseEmitter subscribe(Long auctionId) {
        SseEmitter emitter = new SseEmitter(30*60*1000L);

        String id = auctionId + "_"+System.currentTimeMillis();
        emitters.put(id,emitter);

        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));
        emitter.onError((e) -> emitters.remove(id));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            log.error("SSE 연결 실패", e);
        }

        return emitter;
    }

    public void broadcast(AuctionSseMessage message) {
        try {
            // 객체 -> JSON 문자열로 변환 (예: {"price": 1000, ...})
            String json = objectMapper.writeValueAsString(message);
            // Redis에 JSON을 쏘아 올립니다!
            redisTemplate.convertAndSend(topic.getTopic(), json);
        } catch (Exception e) {
            log.error("SSE 방송 실패", e);
        }
    }

    public void handleMessage(String message) {
        try {
            // [수정] JSON 문자열을 다시 객체로 변환
            // Redis에서 온 메시지에서 따옴표(")가 겉에 붙어있을 수 있어 제거 처리
            String cleanMessage = message.startsWith("\"") && message.endsWith("\"")
                    ? message.substring(1, message.length() - 1).replace("\\\"", "\"")
                    : message;

            AuctionSseMessage sseMessage = objectMapper.readValue(cleanMessage, AuctionSseMessage.class);
            String auctionIdStr = String.valueOf(sseMessage.getAuctionId());

            log.info("📢 SSE 전송: 경매ID={}, 가격={}", sseMessage.getAuctionId(), sseMessage.getCurrentPrice());

            emitters.forEach((key, emitter) -> {
                if (key.startsWith(auctionIdStr + "_")) {
                    try {
                        // 클라이언트에게 JSON 통째로 전송!
                        emitter.send(SseEmitter.event()
                                .name("refresh")
                                .data(cleanMessage)); // JSON 데이터
                    } catch (Exception e) {
                        emitters.remove(key);
                    }
                }
            });
        } catch (Exception e) {
            log.error("메시지 처리 중 에러", e);
        }
    }
}
