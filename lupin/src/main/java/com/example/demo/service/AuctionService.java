package com.example.demo.service;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.BidStatus;
import com.example.demo.repository.AuctionBidRepository;
import com.example.demo.repository.AuctionRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 경매 서비스
 * - 최고 성능: 비관적 락 + Redis 캐싱
 * - 최고 정합성: 트랜잭션 격리 수준 SERIALIZABLE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionBidRepository bidRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String VIEWER_KEY_PREFIX = "auction:viewers:";
    private static final long VIEWER_TTL_SECONDS = 30; // 30초 TTL (활성 시청자 판단)

    /**
     * 활성 경매 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Auction> getActiveAuctions() {
        List<Auction> auctions = auctionRepository.findActiveAuctions(LocalDateTime.now());
        // 각 경매의 시청자 수 조회는 별도 메서드로
        return auctions;
    }

    /**
     * 경매 상세 조회
     */
    @Transactional(readOnly = true)
    public Auction getAuction(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));
    }

    /**
     * 경매 입찰 내역 조회
     */
    @Transactional(readOnly = true)
    public List<AuctionBid> getBidHistory(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByBidTimeDesc(auctionId);
    }

    /**
     * 입찰하기
     * - 비관적 락(SELECT FOR UPDATE)으로 동시성 제어
     * - 트랜잭션 격리 수준: REPEATABLE_READ
     * - 이전 최고 입찰자 OUTBID 처리 및 환불
     * - 카운트다운 연장
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public AuctionBid placeBid(Long auctionId, Long userId, Long bidAmount) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 경매 조회 (비관적 락)
        Auction auction = auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 3. 입찰 가능 여부 확인
        if (!auction.canBid(now)) {
            throw new IllegalStateException("입찰 불가능한 경매입니다.");
        }

        // 4. 입찰가 유효성 확인
        if (!auction.isValidBidAmount(bidAmount)) {
            throw new IllegalStateException("현재가보다 높은 금액을 입찰해주세요.");
        }

        // 5. 보유 포인트 확인
        if (user.getCurrentPoints() < bidAmount) {
            throw new IllegalStateException("보유 포인트가 부족합니다.");
        }

        // 6. 이전 최고 입찰자 OUTBID 처리
        List<AuctionBid> activeBids = bidRepository.findByAuctionIdAndStatus(auctionId, BidStatus.ACTIVE);
        for (AuctionBid activeBid : activeBids) {
            activeBid.markAsOutbid();
            bidRepository.save(activeBid);

            // 이전 입찰자에게 포인트 환불
            User previousBidder = userRepository.findById(activeBid.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("이전 입찰자를 찾을 수 없습니다."));
            previousBidder.addPoints(activeBid.getBidAmount());
            userRepository.save(previousBidder);

            log.info("이전 입찰 OUTBID 처리 완료 - bidId: {}, userId: {}, 환불: {}점",
                    activeBid.getId(), activeBid.getUserId(), activeBid.getBidAmount());
        }

        // 7. 현재 사용자 포인트 차감
        user.setCurrentPoints(user.getCurrentPoints() - bidAmount);
        userRepository.save(user);

        // 8. 새 입찰 생성
        AuctionBid newBid = AuctionBid.of(auctionId, userId, bidAmount);
        bidRepository.save(newBid);

        // 9. 경매 정보 업데이트 (초읽기 모드 처리 포함)
        auction.placeBid(userId, bidAmount, now);
        auctionRepository.save(auction);

        log.info("입찰 완료 - auctionId: {}, userId: {}, bidAmount: {}, 초읽기 모드: {}",
                auctionId, userId, bidAmount, auction.getOvertimeStarted());

        return newBid;
    }

    /**
     * 경매 시청자 등록
     * Redis SET 자료구조 사용, TTL 30초
     */
    public void registerViewer(Long auctionId, Long userId) {
        String key = VIEWER_KEY_PREFIX + auctionId;
        redisTemplate.opsForSet().add(key, userId.toString());
        redisTemplate.expire(key, VIEWER_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 경매 시청자 수 조회
     */
    public Long getViewerCount(Long auctionId) {
        String key = VIEWER_KEY_PREFIX + auctionId;
        Set<String> viewers = redisTemplate.opsForSet().members(key);
        return viewers != null ? (long) viewers.size() : 0L;
    }

    /**
     * 경매 시작
     */
    @Transactional
    public void startAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));

        auction.start();
        auctionRepository.save(auction);

        log.info("경매 시작 - auctionId: {}, itemName: {}", auctionId, auction.getItemName());
    }

    /**
     * 경매 종료
     */
    @Transactional
    public void endAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));

        auction.end();
        auctionRepository.save(auction);

        // 낙찰되지 않은 입찰들 LOST 처리 및 환불
        List<AuctionBid> activeBids = bidRepository.findByAuctionIdAndStatus(auctionId, BidStatus.ACTIVE);
        for (AuctionBid bid : activeBids) {
            if (bid.getUserId().equals(auction.getWinnerId())) {
                // 낙찰자는 WINNING으로
                bid.markAsWinning();
            } else {
                // 나머지는 LOST로 처리하고 환불
                bid.markAsLost();
                User user = userRepository.findById(bid.getUserId())
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                user.addPoints(bid.getBidAmount());
                userRepository.save(user);
            }
            bidRepository.save(bid);
        }

        log.info("경매 종료 - auctionId: {}, winnerId: {}, winningBid: {}",
                auctionId, auction.getWinnerId(), auction.getWinningBid());
    }

    /**
     * 예정된 경매들을 시작
     */
    @Transactional
    public void startScheduledAuctions() {
        List<Auction> scheduled = auctionRepository.findScheduledAuctions(LocalDateTime.now());
        for (Auction auction : scheduled) {
            try {
                startAuction(auction.getId());
            } catch (Exception e) {
                log.error("경매 시작 실패 - auctionId: {}", auction.getId(), e);
            }
        }
    }

    /**
     * 종료 시간이 지난 경매들을 종료
     */
    @Transactional
    public void endExpiredAuctions() {
        List<Auction> expired = auctionRepository.findExpiredAuctions(LocalDateTime.now());
        for (Auction auction : expired) {
            try {
                endAuction(auction.getId());
            } catch (Exception e) {
                log.error("경매 종료 실패 - auctionId: {}", auction.getId(), e);
            }
        }
    }
}
