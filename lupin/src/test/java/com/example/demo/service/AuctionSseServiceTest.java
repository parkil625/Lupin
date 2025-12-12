package com.example.demo.service;

import com.example.demo.dto.AuctionSseMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionSseServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ChannelTopic topic;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuctionSseService auctionSseService;

    // 테스트용 DTO 생성 (실제 DTO가 있다면 그거 쓰시면 됩니다)
    private AuctionSseMessage createMessage(Long id, Long price) {
        // AuctionSseMessage에 @Builder나 @AllArgsConstructor가 있다고 가정
        // 만약 없다면 Setter를 사용하세요.
        AuctionSseMessage msg = new AuctionSseMessage();
        msg.setAuctionId(id);
        msg.setCurrentPrice(price);
        return msg;
    }

    @Test
    @DisplayName("구독(subscribe): SseEmitter가 정상적으로 생성되어야 한다")
    void subscribe_ShouldReturnEmitter() {
        // given
        Long auctionId = 100L;

        // when
        SseEmitter emitter = auctionSseService.subscribe(auctionId);

        // then
        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(30 * 60 * 1000L); // 30분 설정 확인
    }

    @Test
    @DisplayName("방송(broadcast): 객체를 JSON으로 변환해 Redis로 발행해야 한다")
    void broadcast_ShouldPublishToRedis() throws JsonProcessingException {
        // given
        Long auctionId = 100L;
        AuctionSseMessage message = createMessage(auctionId, 5000L);
        String expectedJson = "{\"auctionId\":100,\"currentPrice\":5000}";
        String topicName = "auction-topic";

        // Mock 설정
        given(topic.getTopic()).willReturn(topicName);
        given(objectMapper.writeValueAsString(any(AuctionSseMessage.class)))
                .willReturn(expectedJson);

        // when
        auctionSseService.broadcast(message);

        // then
        // 1. JSON 변환이 호출되었는지 확인
        verify(objectMapper).writeValueAsString(message);
        // 2. Redis로 변환된 JSON이 전송되었는지 확인
        verify(redisTemplate).convertAndSend(eq(topicName), eq(expectedJson));
    }

    @Test
    @DisplayName("메시지 처리(handleMessage): Redis에서 온 JSON 메시지를 파싱해야 한다")
    void handleMessage_ShouldParseAndSend() throws JsonProcessingException {
        // given
        String jsonMessage = "{\"auctionId\":100,\"currentPrice\":5000}";
        AuctionSseMessage parsedMessage = createMessage(100L, 5000L);

        // Mock 설정: JSON 문자열이 들어오면 객체로 변환해준다고 가정
        given(objectMapper.readValue(anyString(), eq(AuctionSseMessage.class)))
                .willReturn(parsedMessage);

        // 구독자가 있어야 전송 로직이 돌므로, 미리 구독(subscribe)을 시켜둠
        auctionSseService.subscribe(100L);

        // when
        auctionSseService.handleMessage(jsonMessage);

        // then
        // objectMapper가 JSON 파싱을 시도했는지 검증
        verify(objectMapper).readValue(anyString(), eq(AuctionSseMessage.class));
    }

    @Test
    @DisplayName("메시지 처리(handleMessage): 따옴표가 중복된 더러운 JSON 문자열도 잘 처리해야 한다")
    void handleMessage_ShouldCleanDirtyJson() throws JsonProcessingException {
        // given
        // Redis 등에서 가끔 "\"{\"id\":1}\"" 처럼 따옴표가 붙어서 올 때가 있음
        String dirtyJson = "\"{\\\"auctionId\\\":100,\\\"currentPrice\\\":5000}\"";
        String cleanJson = "{\"auctionId\":100,\"currentPrice\":5000}";

        AuctionSseMessage parsedMessage = createMessage(100L, 5000L);

        // Mock 설정: '깨끗해진 JSON'으로 파싱을 시도할 것임
        given(objectMapper.readValue(eq(cleanJson), eq(AuctionSseMessage.class)))
                .willReturn(parsedMessage);

        // when
        auctionSseService.handleMessage(dirtyJson);

        // then
        // 반드시 'cleanJson' (따옴표 제거된 버전)으로 파싱 메서드가 호출되어야 함
        verify(objectMapper).readValue(eq(cleanJson), eq(AuctionSseMessage.class));
    }
}