package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionBidFacade {

    private final RedissonClient redissonClient; // 번호표 기계 (이미 build.gradle에 있어서 바로 주입 가능!)
    private final AuctionService auctionService; // 실제 입찰 업무 담당

    public void bid(Long auctionId, Long userId, Long bidAmount, LocalDateTime bidTime) {
        // 1. 락 이름(번호표) 만들기 - 경매장마다 별도의 번호표가 있어야겠죠?
        String lockKey = "auction_lock:" + auctionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 2. 락 획득 시도 (번호표 뽑기)
            // waitTime 초 동안 기다려봄, leaseTime: 초 안에 처리 못하면 락 자동 반납 (데드락 방지)
            boolean available = lock.tryLock(7, 10, TimeUnit.SECONDS);

            if (!available) {
                // 락을 못 얻었으면(사람이 너무 많으면) 튕겨내거나 재시도 안내
                log.info("현재 입찰이 몰려 잠시 후 다시 시도해주세요.");
                return;
            }

            // 3. 락을 얻었으니 실제 서비스의 입찰 로직 호출! (트랜잭션은 여기서 시작됨)
            auctionService.placeBid(auctionId, userId, bidAmount, bidTime);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 4. 업무 다 봤으니 락 반납 (번호표 버리기)
            if (lock.isHeldByCurrentThread()) { // 내가 쥔 락인지 확인하고 해제
                lock.unlock();
            }
        }
    }
}
