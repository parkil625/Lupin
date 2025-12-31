package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionBidFacade {

    private final RedissonClient redissonClient;
    private final AuctionService auctionService;

    public boolean bid(Long auctionId, Long userId, Long bidAmount, LocalDateTime bidTime) {

        // 1. 락(번호표) 이름 생성: 경매 ID별로 락을 겁니다.
        String lockKey = "auction_lock:" + auctionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 2. 락 획득 시도 (최대 2초 기다리고, 락을 잡으면 3초 뒤에 자동으로 풂)
            // waitTime: 락을 얻기 위해 기다리는 시간
            // leaseTime: 락을 잡고 있는 최대 시간 (이 시간이 지나면 강제로 락 해제)
            boolean available = lock.tryLock(2, 3, TimeUnit.SECONDS);

            if (!available) {
                log.warn("현재 입찰이 몰려 처리가 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
                return false;
            }

            // --- 여기서부터는 한 번에 한 명만 들어올 수 있습니다 (Safe Zone) ---

            String redisKey = "auction_price:" + auctionId;
            String luaScript =
                    "local curr = tonumber(redis.call('get', KEYS[1])); " +
                            "local bid = tonumber(ARGV[1]); " +
                            "if curr == nil or bid > curr then " +
                            "   redis.call('set', KEYS[1], ARGV[1]); " +
                            "   return 1; " +
                            "else " +
                            "   return 0; " +
                            "end";

            RScript script = redissonClient.getScript(StringCodec.INSTANCE);
            List<Object> keys = Collections.singletonList(redisKey);
            Object[] values = new Object[]{String.valueOf(bidAmount)};

            Long result = script.eval(
                    RScript.Mode.READ_WRITE,
                    luaScript,
                    RScript.ReturnType.INTEGER,
                    keys,
                    values
            );

            if (result == 0) {
                return false; // 누군가 그새 더 높은 가격을 불렀음
            }

            log.info("Redis 입찰 성공! DB 업데이트를 진행합니다.");

            // 3. 안전하게 DB 업데이트 (이제 방해꾼이 없습니다)
            auctionService.placeBid(auctionId, userId, bidAmount, bidTime);

            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock 획득 중 인터럽트 발생", e);
            return false;
        } finally {
            // 4. 작업이 끝나면 반드시 락을 해제해야 다음 사람이 들어옵니다.
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}