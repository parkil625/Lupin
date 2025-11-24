package com.example.demo.service;

import com.example.demo.domain.entity.Outbox;
import com.example.demo.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxService 테스트")
class OutboxServiceTest {

    @InjectMocks
    private OutboxService outboxService;

    @Mock
    private OutboxRepository outboxRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private Outbox outbox;

    @BeforeEach
    void setUp() {
        outbox = Outbox.builder()
                .id(1L)
                .aggregateType("NOTIFICATION")
                .aggregateId(1L)
                .eventType("LIKE")
                .payload("{\"userId\":1}")
                .build();
    }

    @Nested
    @DisplayName("이벤트 저장")
    class SaveEvent {

        @Test
        @DisplayName("이벤트 저장 성공")
        void saveEvent_Success() {
            // given
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", 1L);
            payload.put("type", "LIKE");

            // when
            outboxService.saveEvent("NOTIFICATION", 1L, "LIKE", payload);

            // then
            then(outboxRepository).should().save(any(Outbox.class));
        }
    }

    @Nested
    @DisplayName("대기 중인 이벤트 조회")
    class GetPendingEvents {

        @Test
        @DisplayName("대기 이벤트 조회 성공")
        void getPendingEvents_Success() {
            // given
            Outbox outbox2 = Outbox.builder()
                    .id(2L)
                    .aggregateType("NOTIFICATION")
                    .aggregateId(2L)
                    .eventType("COMMENT")
                    .payload("{}")
                    .build();

            given(outboxRepository.findPendingEvents(3)).willReturn(Arrays.asList(outbox, outbox2));

            // when
            List<Outbox> result = outboxService.getPendingEvents();

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("재시도 가능한 실패 이벤트 조회")
    class GetRetryableFailedEvents {

        @Test
        @DisplayName("재시도 가능 이벤트 조회 성공")
        void getRetryableFailedEvents_Success() {
            // given
            given(outboxRepository.findRetryableFailedEvents(3)).willReturn(Arrays.asList(outbox));

            // when
            List<Outbox> result = outboxService.getRetryableFailedEvents();

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("이벤트 처리 완료")
    class MarkProcessed {

        @Test
        @DisplayName("처리 완료 표시 성공")
        void markProcessed_Success() {
            // when
            outboxService.markProcessed(outbox);

            // then
            assertThat(outbox.getStatus()).isEqualTo("PROCESSED");
            then(outboxRepository).should().save(outbox);
        }
    }

    @Nested
    @DisplayName("이벤트 처리 실패")
    class MarkFailed {

        @Test
        @DisplayName("처리 실패 표시 성공")
        void markFailed_Success() {
            // when
            outboxService.markFailed(outbox, "Connection timeout");

            // then
            assertThat(outbox.getStatus()).isEqualTo("FAILED");
            then(outboxRepository).should().save(outbox);
        }
    }

    @Nested
    @DisplayName("Payload 파싱")
    class ParsePayload {

        @Test
        @DisplayName("Payload 파싱 성공")
        void parsePayload_Success() {
            // given
            String payload = "{\"userId\":1,\"type\":\"LIKE\"}";

            // when
            Map<String, Object> result = outboxService.parsePayload(payload);

            // then
            assertThat(result).containsEntry("userId", 1);
            assertThat(result).containsEntry("type", "LIKE");
        }

        @Test
        @DisplayName("잘못된 JSON 파싱 실패")
        void parsePayload_InvalidJson_ThrowsException() {
            // given
            String invalidPayload = "invalid json";

            // when & then
            assertThatThrownBy(() -> outboxService.parsePayload(invalidPayload))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
