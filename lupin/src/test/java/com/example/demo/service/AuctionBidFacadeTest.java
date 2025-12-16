package com.example.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionBidFacadeTest {

    @Mock
    private RedissonClient redissonClient; // 번호표 기계

    @Mock
    private AuctionService auctionService; // 은행원

    @Mock
    private RLock lock; // 번호표(Lock) 객체

    @InjectMocks
    private AuctionBidFacade auctionBidFacade;

    @Test
    @DisplayName("락 획득 성공 시: 서비스 로직을 실행하고 락을 해제한다")
    void bid_success() throws InterruptedException {
        // given
        // 1. 락을 달라고 하면 가짜 락(lock)을 줌
        given(redissonClient.getLock(anyString())).willReturn(lock);
        // 2. 락 획득(tryLock)을 시도하면 성공(true)한다고 가정
        given(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        // 3. 내가 락을 쥐고 있다고 가정 (unlock 조건)
        given(lock.isHeldByCurrentThread()).willReturn(true);

        // when
        auctionBidFacade.bid(1L, 1L, 1000L, LocalDateTime.now());

        // then
        // 1. 실제 서비스(placeBid)가 1번 호출되었는지 검증
        verify(auctionService, times(1)).placeBid(anyLong(), anyLong(), anyLong(), any());
        // 2. [중요] 일이 끝나고 락을 해제(unlock)했는지 검증
        verify(lock, times(1)).unlock();
    }

    @Test
    @DisplayName("락 획득 실패 시: 서비스 로직을 실행하지 않는다")
    void bid_fail_lock() throws InterruptedException {
        // given
        given(redissonClient.getLock(anyString())).willReturn(lock);
        // 락 획득 실패(false) 설정
        given(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(false);

        // when
        auctionBidFacade.bid(1L, 1L, 1000L, LocalDateTime.now());

        // then
        // 서비스가 절대 호출되면 안 됨 (Never)
        verify(auctionService, never()).placeBid(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("예외 발생 시: 서비스가 실패해도 락은 무조건 해제되어야 한다")
    void bid_exception_unlock() throws InterruptedException {
        // given
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);

        // [핵심] 서비스가 일하다가 에러를 뿜도록 설정!
        doThrow(new RuntimeException("예기치 못한 에러"))
                .when(auctionService).placeBid(anyLong(), anyLong(), anyLong(), any());

        // when & then
        try {
            auctionBidFacade.bid(1L, 1L, 1000L, LocalDateTime.now());
        } catch (RuntimeException e) {
            // 에러가 터졌지만...
        }

        // then
        // [초중요] 에러가 났음에도 불구하고 unlock은 반드시 호출되어야 함!!
        verify(lock, times(1)).unlock();
    }
}