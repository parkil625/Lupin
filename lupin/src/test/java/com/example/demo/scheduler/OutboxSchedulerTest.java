package com.example.demo.scheduler;

import com.example.demo.domain.entity.Outbox;
import com.example.demo.service.NotificationService;
import com.example.demo.service.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxScheduler 테스트")
class OutboxSchedulerTest {

    @InjectMocks
    private OutboxScheduler outboxScheduler;

    @Mock
    private OutboxService outboxService;

    @Mock
    private NotificationService notificationService;

    @Test
    @DisplayName("대기 이벤트 없음")
    void processPendingEvents_Empty() {
        // given
        given(outboxService.getPendingEvents()).willReturn(Collections.emptyList());

        // when
        outboxScheduler.processPendingEvents();

        // then
        then(outboxService).should().getPendingEvents();
        then(outboxService).should(never()).markProcessed(any());
    }

    @Test
    @DisplayName("알림 이벤트 처리 성공")
    void processPendingEvents_NotificationEvent() {
        // given
        Outbox event = Outbox.builder()
                .id(1L)
                .aggregateType("NOTIFICATION")
                .eventType("CREATED")
                .payload("{\"notificationId\": 1}")
                .build();

        Map<String, Object> payload = new HashMap<>();
        payload.put("notificationId", 1L);

        given(outboxService.getPendingEvents()).willReturn(Arrays.asList(event));
        given(outboxService.parsePayload(anyString())).willReturn(payload);

        // when
        outboxScheduler.processPendingEvents();

        // then
        then(outboxService).should().markProcessed(event);
    }

    @Test
    @DisplayName("이메일 이벤트 처리 성공")
    void processPendingEvents_EmailEvent() {
        // given
        Outbox event = Outbox.builder()
                .id(1L)
                .aggregateType("EMAIL")
                .eventType("SEND")
                .payload("{}")
                .build();

        given(outboxService.getPendingEvents()).willReturn(Arrays.asList(event));
        given(outboxService.parsePayload(anyString())).willReturn(new HashMap<>());

        // when
        outboxScheduler.processPendingEvents();

        // then
        then(outboxService).should().markProcessed(event);
    }

    @Test
    @DisplayName("알 수 없는 이벤트 타입 처리")
    void processPendingEvents_UnknownType() {
        // given
        Outbox event = Outbox.builder()
                .id(1L)
                .aggregateType("UNKNOWN")
                .eventType("TEST")
                .payload("{}")
                .build();

        given(outboxService.getPendingEvents()).willReturn(Arrays.asList(event));
        given(outboxService.parsePayload(anyString())).willReturn(new HashMap<>());

        // when
        outboxScheduler.processPendingEvents();

        // then
        then(outboxService).should().markFailed(eq(event), anyString());
    }

    @Test
    @DisplayName("실패 이벤트 재시도")
    void retryFailedEvents_Success() {
        // given
        Outbox event = Outbox.builder().id(1L).build();
        given(outboxService.getRetryableFailedEvents()).willReturn(Arrays.asList(event));

        // when
        outboxScheduler.retryFailedEvents();

        // then
        then(outboxService).should().markPendingForRetry(event);
    }

    @Test
    @DisplayName("재시도할 실패 이벤트 없음")
    void retryFailedEvents_Empty() {
        // given
        given(outboxService.getRetryableFailedEvents()).willReturn(Collections.emptyList());

        // when
        outboxScheduler.retryFailedEvents();

        // then
        then(outboxService).should(never()).markPendingForRetry(any());
    }
}
