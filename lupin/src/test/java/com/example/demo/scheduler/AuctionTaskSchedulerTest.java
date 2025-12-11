package com.example.demo.scheduler;

import com.example.demo.service.AuctionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionTaskSchedulerTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private AuctionService auctionService;

    @InjectMocks
    private AuctionTaskScheduler auctionTaskScheduler; // 테스트할 대상

    @Test
    @DisplayName("예약된 경매 시작 시간이 되면 알람을 등록한다")
    void activeAuction() {
        // given (상황)
        Long auctionId = 1L;
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);

        // when (실행)
        // "이 경매 1시간 뒤에 시작하게 예약해줘!"
        auctionTaskScheduler.scheduleAuctionStart(auctionId, startTime);

        // then (검증)
        // "시계야(taskScheduler), 너 아까 schedule 함수 호출받았니?" 확인
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("경매 종료(비활성화) 알람을 등록한다")
        // 설명: 입찰이 없으면 그냥 이 시간에 끝납니다. '입찰이 없던' 조건은 스케줄러가 아니라 로직의 결과입니다.
    void deactiveAuction() {
        // given
        Long auctionId = 2L;
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(30);

        // [추가된 부분] 가짜 예약증(dummyFuture)을 만들어서 쥐어줍니다.
        ScheduledFuture dummyFuture = mock(ScheduledFuture.class);

        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .thenReturn(dummyFuture);

        // when
        auctionTaskScheduler.scheduleAuctionEnd(auctionId, endTime);

        // then
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("입찰이 들어오면 기존 종료 알람을 취소하고, 새로운 시간으로 다시 맞춘다")
    void overtimeReset() {
        // given
        Long auctionId = 3L;
        LocalDateTime firstEndTime = LocalDateTime.now().plusMinutes(5);
        LocalDateTime newEndTime = LocalDateTime.now().plusMinutes(10); // 시간 연장!

        // 가짜(Mock) 예약증(ScheduledFuture) 만들기 - 취소 기능 확인용
        ScheduledFuture mockedTask = mock(ScheduledFuture.class);

        // "첫 번째 예약할 때는 이 가짜 예약증을 리턴해줘"라고 설정
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .thenReturn(mockedTask);

        // when
        // 1. 처음 종료 예약
        auctionTaskScheduler.scheduleAuctionEnd(auctionId, firstEndTime);
        // 2. 입찰 발생! -> 시간 연장해서 다시 예약
        auctionTaskScheduler.scheduleAuctionEnd(auctionId, newEndTime);

        // then (검증)
        // "아까 그 예약증(mockedTask), 취소(cancel) 도장 찍혔니?" 확인
        verify(mockedTask).cancel(false);
        // "그리고 스케줄 예약은 총 2번 일어났니?" (처음 등록 + 재등록)
        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Instant.class));
    }
}