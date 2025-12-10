package com.example.demo.service;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.BidStatus;
import com.example.demo.dto.response.AuctionBidResponse;
import com.example.demo.dto.response.AuctionStatusResponse;
import com.example.demo.dto.response.OngoingAuctionResponse;
import com.example.demo.dto.response.ScheduledAuctionResponse;
import com.example.demo.repository.AuctionBidRepository;
import com.example.demo.repository.AuctionRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    //경매 입찰 시켜주는 메소드
    public void placeBid(Long auctionId, Long Id, Long bidAmount, LocalDateTime bidTime) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다. id=" + auctionId));

        User user = userRepository.findById(Id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + Id));

        long currentBalance = pointService.getTotalPoints(user);
        if (currentBalance < bidAmount) {
            throw new IllegalStateException("포인트가 부족합니다. 현재 잔액: " + currentBalance);
        }

        // 이전 최고 입찰자가 있다면 포인트를 환불해줌 (기존 로직 사이에 추가)
        if (auction.getWinner() != null) {
            User previousWinner = auction.getWinner();
            Long previousPrice = auction.getCurrentPrice();

            // 이전 우승자에게 포인트 반환 (+금액)
            pointService.addPoints(previousWinner, previousPrice);
        }

        // 3. 현재 입찰자의 포인트 차감 (-금액)
        pointService.deductPoints(user, bidAmount);

        // 도메인 로직 위임 (현재가/우승자/시간 검증 등)
        auction.placeBid(user, bidAmount, bidTime);

        // 이전 최고 입찰 가져와서 상태 변경
        auctionBidRepository
                .findTopByAuctionAndStatusOrderByBidAmountDescBidTimeDesc(auction, BidStatus.ACTIVE)
                .ifPresent(AuctionBid::outBid);

        // 입찰 엔티티 생성,저장
        AuctionBid bid = auction.createBid(user, bidAmount, bidTime);
        auctionBidRepository.save(bid);
    }

    //경매 시작 시간이 된 경매 active 시켜주는 메소드
    public void activateScheduledAuctions(LocalDateTime now) {

       List<Auction> auctions = auctionRepository.findByStatusAndStartTimeBefore(AuctionStatus.SCHEDULED, now);
        for (Auction auction : auctions) {
            auction.activate(now);
        }
    }

    //종료된 경매 ended 시켜주는 메소드
    public void closeExpiredAuctions(LocalDateTime now) {
        List<Auction> auctions = auctionRepository.findExpiredAuctions(now);

        for (Auction auction : auctions) {
            List<AuctionBid> auctionBids = auctionBidRepository.findByAuctionId(auction.getId());

            // 상태 변경 (ACTIVE -> ENDED)
            auction.deactivate(auctionBids);

            // [추가] 변경 사항 즉시 DB 반영!
            auctionRepository.saveAndFlush(auction);

            log.info("🏁 경매 ID {} -> 종료(ENDED) 처리 완료", auction.getId());
        }

    }

    //현재 진행중인 경매정보와 경매물품조회
    public OngoingAuctionResponse getOngoingAuctionWithItem() {

        Auction auction = auctionRepository.findFirstWithItemByStatus(AuctionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("진행 중인 경매가 없습니다."));

        return OngoingAuctionResponse.from(auction);
    }

    //예정인 경매 정보와 경매 물품 조회
    public List<ScheduledAuctionResponse> scheduledAuctionWithItem() {
        List<Auction> auctions = auctionRepository.findAllByStatusOrderByStartTimeAscWithItem(AuctionStatus.SCHEDULED);
        
        return auctions.stream()
                .map(ScheduledAuctionResponse::from)
                .toList();
    }

    //현재 경매 정보 업데이트 내용 조회
    public AuctionStatusResponse getRealtimeStatus(){
        return auctionRepository.findAuctionStatus().orElseThrow(() -> new IllegalStateException("진행 중인 경매가 없습니다."));
    }

    //현재 경매 정보 내역 리스트 조회
    public List<AuctionBidResponse> getAuctionStatus(){
        // 1. 엔티티 리스트 조회 (User 정보 포함됨)
        List<AuctionBid> bids = auctionBidRepository.findBidsByActiveAuction();

        // 2. DTO로 변환 (이미 만든 from 메서드 활용)
        return bids.stream()
                .map(AuctionBidResponse::from)
                .toList();
    }

    public void startOvertimeForAuctions(LocalDateTime now) {
        List<Auction> auctions = auctionRepository.findAuctionsReadyForOvertime(now);

        // [추가] 대상을 찾았는지 확인하는 로그
        if (!auctions.isEmpty()) {
            log.info("⏰ 초읽기 전환 대상 경매 {}건 발견! (기준 시간: {})", auctions.size(), now);
        }

        for (Auction auction : auctions) {
            try {
                auction.startOvertime(now);

                // [필수] 즉시 반영
                auctionRepository.saveAndFlush(auction);

                log.info("✅ 경매 ID {} -> 초읽기 모드(Overtime)로 변경 및 저장 완료", auction.getId());
            } catch (IllegalStateException e) {
                log.error("경매 ID {} 초읽기 전환 실패: {}", auction.getId(), e.getMessage());
            }
        }
    }

}
