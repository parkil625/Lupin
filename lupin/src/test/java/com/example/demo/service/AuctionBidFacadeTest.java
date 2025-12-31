package com.example.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionBidFacadeTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RScript rScript;

    @Mock
    private RLock rLock; // [추가] 락 객체 Mocking

    @Mock
    private AuctionService auctionService;

    @InjectMocks
    private AuctionBidFacade auctionBidFacade;

    @Test
    @DisplayName("성공: 락 획득 성공 -> Lua 스크립트 성공(1) -> DB 저장 수행 -> true 반환")
    void bid_success() throws InterruptedException {
        // given
        Long auctionId = 1L;
        Long userId = 100L;
        Long bidAmount = 5000L;
        String lockKey = "auction_lock:" + auctionId;

        // 1. 락 Mocking 설정
        given(redissonClient.getLock(lockKey)).willReturn(rLock);
        // tryLock이 true를 반환한다고 가정 (락 획득 성공!)
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        // unlock 검증을 위해 락 상태 설정
        given(rLock.isLocked()).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        // 2. Redis Script Mocking 설정
        given(redissonClient.getScript(StringCodec.INSTANCE)).willReturn(rScript);
        given(rScript.eval(
                any(RScript.Mode.class),
                anyString(),
                any(RScript.ReturnType.class),
                any(List.class),
                any()
        )).willReturn(1L); // 스크립트 성공(1)

        // when
        boolean result = auctionBidFacade.bid(auctionId, userId, bidAmount, LocalDateTime.now());

        // then
        assertThat(result).isTrue();

        // [중요 검증]
        // 1. DB 서비스가 호출되었는지
        verify(auctionService, times(1)).placeBid(eq(auctionId), eq(userId), eq(bidAmount), any());
        // 2. 락을 획득 시도 했는지
        verify(rLock, times(1)).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        // 3. 작업이 끝나고 락을 잘 해제했는지 (unlock)
        verify(rLock, times(1)).unlock();
    }

    @Test
    @DisplayName("실패: 락 획득 실패(타임아웃) 시 -> 로직 실행 안 함 -> false 반환")
    void bid_fail_lock_timeout() throws InterruptedException {
        // given
        Long auctionId = 1L;
        String lockKey = "auction_lock:" + auctionId;

        given(redissonClient.getLock(lockKey)).willReturn(rLock);

        // [핵심] 누군가 이미 락을 잡고 있어서 tryLock이 false를 반환한다고 가정
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(false);

        // when
        boolean result = auctionBidFacade.bid(auctionId, 100L, 5000L, LocalDateTime.now());

        // then
        assertThat(result).isFalse();

        // [중요 검증] Redis 스크립트나 DB 로직은 아예 실행조차 되면 안 됨
        verify(rScript, never()).eval(any(), any(), any(), any(), any());
        verify(auctionService, never()).placeBid(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("실패: 락은 잡았으나 -> Lua 스크립트 실패(0) -> DB 저장 안 함 -> false 반환")
    void bid_fail_redis_reject() throws InterruptedException {
        // given
        Long auctionId = 1L;
        String lockKey = "auction_lock:" + auctionId;

        // 1. 락 획득은 성공한다고 가정
        given(redissonClient.getLock(lockKey)).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(rLock.isLocked()).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);

        // 2. 하지만 Redis 스크립트 결과가 0 (실패 - 더 높은 가격 존재)
        given(redissonClient.getScript(StringCodec.INSTANCE)).willReturn(rScript);
        given(rScript.eval(
                any(RScript.Mode.class),
                anyString(),
                any(RScript.ReturnType.class),
                any(List.class),
                any()
        )).willReturn(0L);

        // when
        boolean result = auctionBidFacade.bid(auctionId, 100L, 4000L, LocalDateTime.now());

        // then
        assertThat(result).isFalse();

        // [중요 검증]
        // 1. DB 서비스는 호출되면 안 됨
        verify(auctionService, never()).placeBid(anyLong(), anyLong(), anyLong(), any());
        // 2. 하지만 락은 반드시 해제되어야 함 (finally 블록)
        verify(rLock, times(1)).unlock();
    }
}