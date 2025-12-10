package com.example.demo.scheduler;

import com.example.demo.service.AuctionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionSchedulerTest {

    @Mock
    private AuctionService auctionService;

    @InjectMocks
    private AuctionScheduler auctionScheduler;

    @Test
    @DisplayName("스케줄러가 실행되면 서비스 로직을 호출한다")
    void handleAuctions() {
        // when
        // 스케줄러 메서드를 직접 호출 (Spring 스케줄링 없이 로직만 테스트)
        auctionScheduler.handleAuctions1();
        auctionScheduler.handleAuctions();
        // then
        // 1. 예약된 경매 시작 로직 호출 확인
        verify(auctionService, times(1)).activateScheduledAuctions(any(LocalDateTime.class));

        // 2. 만료된 경매 종료 로직 호출 확인
        verify(auctionService, times(1)).closeExpiredAuctions(any(LocalDateTime.class));
    }
}