package com.example.demo.repository;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionItem;
import com.example.demo.domain.enums.AuctionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AuctionRepositoryTest {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("종료 시간이 지난 ACTIVE 경매를 조회한다 (만료된 경매)")
    void findExpiredAuctions() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 1. 만료된 경매 생성 (필수값 채우기)
        Auction expiredAuction = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .currentPrice(1000L)
                .startTime(now.minusHours(2))
                .regularEndTime(now.minusHours(1))
                .overtimeStarted(false)
                .build();

        // 2. 진행 중인 경매 생성
        Auction activeAuction = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .currentPrice(2000L)
                .startTime(now)
                .regularEndTime(now.plusHours(1))
                .overtimeStarted(false)
                .build();

        auctionRepository.save(expiredAuction);
        auctionRepository.save(activeAuction);

        // when
        List<Auction> result = auctionRepository.findExpiredAuctions(now);

        // then
        assertThat(result).extracting("id").contains(expiredAuction.getId());
        assertThat(result).extracting("id").doesNotContain(activeAuction.getId());
    }

    @Test
    @DisplayName("경매 상태로 아이템과 함께 조회한다 (Fetch Join 확인)")
    void findFirstWithItemByStatus() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // Auction 먼저 저장
        Auction auction = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .currentPrice(1000L)
                .startTime(now)
                .regularEndTime(now.plusHours(1))
                .overtimeStarted(false)
                .build();
        auctionRepository.save(auction);

        // AuctionItem을 auction과 연결하여 저장
        AuctionItem item = AuctionItem.builder()
                .itemName("테스트 물품")
                .description("설명")
                .itemImage("image.jpg")
                .auction(auction)
                .build();
        entityManager.persist(item);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Auction> result = auctionRepository.findFirstWithItemByStatus(AuctionStatus.ACTIVE);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAuctionItem()).isNotNull();
        assertThat(result.get().getAuctionItem().getItemName()).isEqualTo("테스트 물품");
    }
}
