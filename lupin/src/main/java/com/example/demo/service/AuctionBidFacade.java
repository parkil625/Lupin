package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec; // import 확인
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionBidFacade {

    private final RedissonClient redissonClient;
    private final AuctionService auctionService;

    public boolean bid(Long auctionId, Long userId, Long bidAmount, LocalDateTime bidTime) {

        String redisKey = "auction_price:" + auctionId;

        String luaScript =
                "local curr = tonumber(redis.call('get', KEYS[1])); "+
                        "local bid = tonumber(ARGV[1]); " +
                        "if curr == nil or bid > curr then " +
                        "   redis.call('set', KEYS[1], ARGV[1]); " +
                        "   return 1; " +
                        "else " +
                        "   return 0; " +
                        "end";

        // [중요 수정] StringCodec.INSTANCE를 넣어줘야 숫자를 제대로 인식합니다!
        RScript script = redissonClient.getScript(StringCodec.INSTANCE);

        List<Object> keys = Collections.singletonList(redisKey);

        // [권장] 값도 명확하게 문자열로 변환해서 전달
        Object[] values = new Object[]{String.valueOf(bidAmount)};

        Long result = script.eval(
                RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.INTEGER,
                keys,
                values
        );

        if (result == 0) {
            return false;
        }

        log.info("Redis 입찰 성공! DB 업데이트를 진행합니다.");
        auctionService.placeBid(auctionId, userId, bidAmount, bidTime);
        return true;
    }
}