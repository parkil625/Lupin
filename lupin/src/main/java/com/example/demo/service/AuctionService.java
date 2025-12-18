package com.example.demo.service;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.BidStatus;
import com.example.demo.domain.enums.PointType;
import com.example.demo.dto.AuctionSseMessage;
import com.example.demo.dto.response.AuctionBidResponse;
import com.example.demo.dto.response.AuctionStatusResponse;
import com.example.demo.dto.response.OngoingAuctionResponse;
import com.example.demo.dto.response.ScheduledAuctionResponse;
import com.example.demo.event.NotificationEvent;
import com.example.demo.repository.AuctionBidRepository;
import com.example.demo.repository.AuctionRepository;
import com.example.demo.repository.PointLogRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.scheduler.AuctionTaskScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final PointService pointService;
    private final PointLogRepository pointLogRepository;
    private final AuctionTaskScheduler auctionTaskScheduler;
    private final ApplicationEventPublisher eventPublisher;

    private final AuctionSseService auctionSseService;
    private final RedissonClient redissonClient;

    // 경매 입찰 시켜주는 메소드
    public void placeBid(Long auctionId, Long userId, Long bidAmount, LocalDateTime bidTime) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다. id=" + auctionId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));

        /* * [변경 사항]
         * 1. 보유 포인트 검사 로직 삭제 (부족해도 입찰 가능 -> 나중에 음수 처리)
         * 2. 이전 입찰자 환불 로직 삭제 (입찰 시 차감하지 않으므로 환불도 필요 없음)
         * 3. 현재 입찰자 포인트 차감 로직 삭제 (낙찰 시 일괄 차감)
         */

        // 도메인 로직 위임 (현재가 갱신, 우승자 설정, 마감 시간 연장 등)
        auction.placeBid(user, bidAmount, bidTime);

        // 이전 최고 입찰 가져와서 상태 변경 (ACTIVE -> OUTBID)
        auctionBidRepository
                .findTopByAuctionAndStatusOrderByBidAmountDescBidTimeDesc(auction, BidStatus.ACTIVE)
                .ifPresent(AuctionBid::outBid);

        // 입찰 엔티티 생성 및 저장
        AuctionBid bid = auction.createBid(user, bidAmount, bidTime);
        auctionBidRepository.save(bid);

        // 스케줄러 업데이트 (마감 시간이 변경되었을 수 있으므로)
        auctionTaskScheduler.scheduleAuctionEnd(auction.getId(), auction.getEndTime());

        // SSE 전송
        AuctionSseMessage message = AuctionSseMessage.builder()
                .auctionId(auctionId)
                .bidderId(user.getId())
                .currentPrice(auction.getCurrentPrice()) // 갱신된 가격
                .bidderName(user.getName())              // 입찰자 이름
                .bidTime(bidTime.toString())             // 입찰 시간
                .newEndTime(auction.getEndTime().toString()) // 연장된 종료 시간
                .build();

        auctionSseService.broadcast(message);
    }

    // 경매 시작 시간이 된 경매 active 시켜주는 메소드
    public void activateScheduledAuctions(LocalDateTime now) {
        List<Auction> auctions = auctionRepository.findByStatusAndStartTimeBefore(AuctionStatus.SCHEDULED, now);
        for (Auction auction : auctions) {
            auction.activate(now);

            RBucket<Long> bucket = redissonClient.getBucket("auction_price:" + auction.getId());
            bucket.set(auction.getCurrentPrice());
        }
    }

    // 종료된 경매 ended 시켜주는 메소드
    public void closeExpiredAuctions(LocalDateTime now) {
        LocalDateTime endedTimeThreshold = now.minusSeconds(30);
        List<Auction> auctions = auctionRepository.findExpiredAuctions(now, endedTimeThreshold);

        for (Auction auction : auctions) {
            List<AuctionBid> auctionBids = auctionBidRepository.findByAuctionId(auction.getId());

            // 상태 변경 (ACTIVE -> ENDED) 및 우승자 확정
            auction.deactivate(auctionBids);

            // [수정된 부분] 낙찰자가 존재하면 포인트 차감 (이벤트 발행 없이 직접 처리)
            if (auction.getWinner() != null) {
                try {
                    User winner = auction.getWinner();
                    Long price = auction.getCurrentPrice();

                    // 1. 유저 포인트 직접 차감
                    winner.deductPoints(price);

                    // 2. 변경된 유저 정보 저장
                    userRepository.save(winner);

                    // 3. 포인트 로그 직접 생성 및 저장 (PointService를 안 거치므로 이벤트 발생 X)
                    PointLog pointLog = PointLog.builder()
                            .user(winner)
                            .points(-price) // 사용이므로 음수
                            .type(PointType.USE)
                            .build();
                    pointLogRepository.save(pointLog);

                    log.info("경매 ID {} 낙찰 -> 사용자 {} 포인트 차감 완료 (금액: {})",
                            auction.getId(), winner.getId(), price);

                    eventPublisher.publishEvent(NotificationEvent.auctionWin(
                            winner.getId(),
                            auction.getId(),
                            auction.getAuctionItem().getItemName(), // 물품 이름
                            price
                    ));

                } catch (Exception e) {
                    log.error("경매 ID {} 포인트 차감 중 오류 발생: {}", auction.getId(), e.getMessage());
                }
            }

            // 변경 사항 DB 반영
            auctionRepository.saveAndFlush(auction);
            log.info("경매 ID {} -> 종료(ENDED) 처리 완료", auction.getId());
        }
    }


    // 현재 진행중인 경매정보와 경매물품조회
    public OngoingAuctionResponse getOngoingAuctionWithItem() {
        // 1. 일단 진행 중인 경매를 가져옵니다.
        Auction auction = auctionRepository.findFirstWithItemByStatus(AuctionStatus.ACTIVE)
                .orElse(null);

        // 경매가 없으면 null 반환
        if (auction == null) {
            return null;
        }

        // 2. [핵심 로직 추가] 가져온 경매가 이미 시간이 지났는지 확인합니다.
        if (auction.getEndTime().isBefore(LocalDateTime.now())) {
            log.info("조회 시점 경매 만료 감지! 강제 종료 처리 진행 (ID: {})", auction.getId());

            // 시간이 지났다면 스케줄러를 기다리지 않고 '지금 당장' 종료 처리를 수행합니다.
            // (이 메서드 안에서 상태 변경 + 포인트 차감이 모두 일어납니다)
            closeExpiredAuctions(LocalDateTime.now());

            // 종료되었으므로 '없음'을 반환합니다.
            return null;
        }

        return OngoingAuctionResponse.from(auction);
    }

    // 예정인 경매 정보와 경매 물품 조회
    public List<ScheduledAuctionResponse> scheduledAuctionWithItem() {
        List<Auction> auctions = auctionRepository.findAllByStatusOrderByStartTimeAscWithItem(AuctionStatus.SCHEDULED);

        return auctions.stream()
                .map(ScheduledAuctionResponse::from)
                .toList();
    }

    // 현재 경매 정보 업데이트 내용 조회
    public AuctionStatusResponse getRealtimeStatus(){
        return auctionRepository.findAuctionStatus()
                .orElseThrow(() -> new IllegalStateException("진행 중인 경매가 없습니다."));
    }

    // 현재 경매 정보 내역 리스트 조회
    public List<AuctionBidResponse> getAuctionStatus(){
        // 1. 엔티티 리스트 조회 (User 정보 포함됨)
        List<AuctionBid> bids = auctionBidRepository.findBidsByActiveAuction();

        // 2. DTO로 변환
        return bids.stream()
                .map(AuctionBidResponse::from)
                .toList();
    }

    // 초읽기 모드 전환
    public void startOvertimeForAuctions(LocalDateTime now) {
        List<Auction> auctions = auctionRepository.findAuctionsReadyForOvertime(now);

        if (!auctions.isEmpty()) {
            log.info("초읽기 전환 대상 경매 {}건 발견! (기준 시간: {})", auctions.size(), now);
        }

        for (Auction auction : auctions) {
            try {
                auction.startOvertime(now);
                auctionRepository.saveAndFlush(auction);
                log.info("경매 ID {} -> 초읽기 모드(Overtime)로 변경 및 저장 완료", auction.getId());
            } catch (IllegalStateException e) {
                log.error("경매 ID {} 초읽기 전환 실패: {}", auction.getId(), e.getMessage());
            }
        }
    }
}