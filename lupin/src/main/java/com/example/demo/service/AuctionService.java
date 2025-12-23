package com.example.demo.service;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.BidStatus;
import com.example.demo.domain.enums.PointType;
import com.example.demo.dto.AuctionSseMessage;
import com.example.demo.dto.response.*;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final AuctionBidRepository auctionBidRepository;

    private final PointLogRepository pointLogRepository;
    private final AuctionTaskScheduler auctionTaskScheduler;
    private final ApplicationEventPublisher eventPublisher;

    private final AuctionSseService auctionSseService;
    private final RedissonClient redissonClient;


    @Lazy
    @Autowired
    private AuctionService self;

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
            try {
                // [2] 'this' 대신 'self'를 사용하여 프록시를 통한 호출 (트랜잭션 분리 효과)
                self.processSingleAuctionClose(auction);
            } catch (Exception e) {
                log.error("경매 ID {} 종료 처리 중 오류 발생", auction.getId(), e);
                // 이제 여기서 에러를 잡아도 전체 트랜잭션에 영향을 주지 않습니다!
            }
        }
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleAuctionClose(Auction auction) {
        // [중요 수정 1] 넘어온 auction 객체는 '남의 것'이므로, ID로 이 트랜잭션용 객체를 다시 찾습니다.
        Auction currentAuction = auctionRepository.findById(auction.getId())
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));

        List<AuctionBid> auctionBids = auctionBidRepository.findByAuctionId(currentAuction.getId());

        // 여기서 조회한 currentAuction을 변경합니다.
        currentAuction.deactivate(auctionBids);

        if (currentAuction.getWinner() != null) {
            // [중요 수정 2] Winner 객체도 여기서 다시 조회해야 안전합니다.
            // (currentAuction.getWinner()로 가져와도 되지만, 명시적으로 ID로 찾는 게 확실합니다)
            User winner = userRepository.findById(currentAuction.getWinner().getId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            Long price = currentAuction.getCurrentPrice();

            // 새로 조회한 winner 객체를 수정
            winner.deductPoints(price);
            userRepository.save(winner); // 이제 정상 저장됩니다.

            PointLog pointLog = PointLog.builder()
                    .user(winner)
                    .points(-price)
                    .type(PointType.USE)
                    .build();
            pointLogRepository.save(pointLog);

            eventPublisher.publishEvent(NotificationEvent.auctionWin(
                    winner.getId(),
                    currentAuction.getId(),
                    currentAuction.getAuctionItem().getItemName(),
                    price
            ));
        }

        // 마지막에 저장
        auctionRepository.saveAndFlush(currentAuction);
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

    public List<AuctionResponse> getMonthlyWinners() {
        // 1. 이번 달의 시작(1일)과 끝 계산
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        // 2. DB 조회 (이번 달에 종료된 경매)
        List<Auction> winners = auctionRepository.findMonthlyWinners(
                AuctionStatus.ENDED,
                startOfMonth,
                now
        );

        // 3. 예쁘게 포장해서 반환 (낙찰자 없으면 제외)
        return winners.stream()
                .filter(a -> a.getWinner() != null) // 낙찰자가 있는 경우만
                .map(a -> AuctionResponse.from(a, a.getWinner().getName()))
                .collect(Collectors.toList());
    }
}