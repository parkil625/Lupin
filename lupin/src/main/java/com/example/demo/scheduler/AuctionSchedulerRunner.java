package com.example.demo.scheduler;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionSchedulerRunner implements ApplicationRunner {

    private final AuctionRepository auctionRepository;
    private final AuctionTaskScheduler auctionTaskScheduler;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("[서버 시작] DB에 있던 경매들을 스케줄러에 다시 등록합니다...");

        LocalDateTime now = LocalDateTime.now();

        // 1. '진행 중(ACTIVE)'인 경매 찾기 -> 종료 알람 다시 맞추기
        List<Auction> activeAuctions = auctionRepository.findAllByStatus(AuctionStatus.ACTIVE);
        for (Auction auction : activeAuctions) {
            if (auction.getEndTime().isAfter(now)) { // 아직 안 끝난 거만
                auctionTaskScheduler.scheduleAuctionEnd(auction.getId(), auction.getEndTime());

                RBucket<String> bucket = redissonClient.getBucket("auction_price:" + auction.getId(), StringCodec.INSTANCE);
                bucket.set(String.valueOf(auction.getCurrentPrice()));
            } else {
                // (선택) 이미 시간이 지났는데 ACTIVE인 애들은 여기서 바로 종료 처리 해도 됨
            }
        }
        log.info("진행 중인 경매 {}건 복구 완료", activeAuctions.size());

        // 2. '예정(SCHEDULED)'인 경매 찾기 -> 시작 알람 다시 맞추기
        List<Auction> scheduledAuctions = auctionRepository.findAllByStatus(AuctionStatus.SCHEDULED);
        for (Auction auction : scheduledAuctions) {
            if (auction.getStartTime().isAfter(now)) {
                auctionTaskScheduler.scheduleAuctionStart(auction.getId(), auction.getStartTime());
            }
        }
        log.info("예정된 경매 {}건 예약 완료", scheduledAuctions.size());
    }
}
