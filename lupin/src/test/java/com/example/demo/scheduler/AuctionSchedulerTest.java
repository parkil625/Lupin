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
        auctionScheduler.handleAuctions();

        // then
        // activateScheduledAuctions가 1번 호출되었는지 검증
        verify(auctionService, times(1)).activateScheduledAuctions(any(LocalDateTime.class));
        // closeExpiredAuctions가 1번 호출되었는지 검증
        verify(auctionService, times(1)).closeExpiredAuctions(any(LocalDateTime.class));
    }
}