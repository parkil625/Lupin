package com.example.demo.repository;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionItem;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.config.QueryDslConfig; // QueryDSL 설정이 있다면 Import 필요
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class) // QueryDSL을 쓴다면 필요, 안 쓴다면 제거
class AuctionRepositoryTest {

    @Autowired
    private AuctionRepository auctionRepository;

    @Test
    @DisplayName("종료 시간이 지난 ACTIVE 경매를 조회한다 (만료된 경매)")
    void findExpiredAuctions() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 만료된 경매 생성
        Auction expiredAuction = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .startTime(now.minusHours(2))
                .regularEndTime(now.minusHours(1)) // 이미 끝남
                .overtimeStarted(false)
                .build();

        // 진행 중인 경매 생성 (조회되면 안 됨)
        Auction activeAuction = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .startTime(now)
                .regularEndTime(now.plusHours(1))
                .overtimeStarted(false)
                .build();

        auctionRepository.save(expiredAuction);
        auctionRepository.save(activeAuction);

        // when
        List<Auction> result = auctionRepository.findExpiredAuctions(now);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(expiredAuction.getId());
    }

    @Test
    @DisplayName("경매 상태로 아이템과 함께 조회한다 (Fetch Join 확인)")
    void findFirstWithItemByStatus() {
        // given
        AuctionItem item = AuctionItem.builder()
                .itemName("테스트 물품")
                .description("설명")
                .build();

        Auction auction = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .auctionItem(item) // 연관관계 설정
                .startTime(LocalDateTime.now())
                .build();

        auctionRepository.save(auction);

        // when
        Optional<Auction> result = auctionRepository.findFirstWithItemByStatus(AuctionStatus.ACTIVE);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAuctionItem().getItemName()).isEqualTo("테스트 물품");
    }
}