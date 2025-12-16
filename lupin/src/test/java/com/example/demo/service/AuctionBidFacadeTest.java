package com.example.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionBidFacadeTest {

    @Mock
    private RedissonClient redissonClient; // Redis 클라이언트

    @Mock
    private RScript rScript; // Lua Script 실행기

    @Mock
    private AuctionService auctionService; // 실제 서비스 로직

    @InjectMocks
    private AuctionBidFacade auctionBidFacade;

    @Test
    @DisplayName("Lua 스크립트 실행 성공(1 반환) 시: DB 입찰 로직을 호출하고 true를 반환한다")
    void bid_success() {
        // given
        Long auctionId = 1L;
        Long userId = 100L;
        Long bidAmount = 5000L;

        // 1. RedissonClient가 StringCodec을 사용하는 Script 객체를 반환하도록 설정
        given(redissonClient.getScript(StringCodec.INSTANCE)).willReturn(rScript);

        // 2. 스크립트 실행 결과가 '1' (성공)이라고 가정
        given(rScript.eval(
                any(RScript.Mode.class),
                anyString(),
                any(RScript.ReturnType.class),
                any(List.class),
                any()
        )).willReturn(1L);

        // when
        boolean result = auctionBidFacade.bid(auctionId, userId, bidAmount, LocalDateTime.now());

        // then
        // 1. 결과가 true여야 함
        assertThat(result).isTrue();
        // 2. 실제 DB 서비스(placeBid)가 1번 호출되었는지 검증
        verify(auctionService, times(1)).placeBid(eq(auctionId), eq(userId), eq(bidAmount), any());
    }

    @Test
    @DisplayName("Lua 스크립트 실행 실패(0 반환) 시: DB 로직을 호출하지 않고 false를 반환한다")
    void bid_fail_redis_reject() {
        // given
        Long auctionId = 1L;
        Long userId = 100L;
        Long bidAmount = 4000L; // 낮은 금액

        given(redissonClient.getScript(StringCodec.INSTANCE)).willReturn(rScript);

        // 2. 스크립트 실행 결과가 '0' (실패 - 더 높은 가격 존재)이라고 가정
        given(rScript.eval(
                any(RScript.Mode.class),
                anyString(),
                any(RScript.ReturnType.class),
                any(List.class),
                any()
        )).willReturn(0L);

        // when
        boolean result = auctionBidFacade.bid(auctionId, userId, bidAmount, LocalDateTime.now());

        // then
        // 1. 결과가 false여야 함
        assertThat(result).isFalse();
        // 2. DB 서비스(placeBid)는 절대 호출되면 안 됨! (Redis에서 컷 당했으므로)
        verify(auctionService, never()).placeBid(anyLong(), anyLong(), anyLong(), any());
    }
}