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
    private TestEntityManager entityManager; // 데이터를 먼저 저장하기 위해 사용

    @Test
    @DisplayName("종료 시간이 지난 ACTIVE 경매를 조회한다 (만료된 경매)")
    void findExpiredAuctions() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 1. 만료된 경매 생성 (필수값 채우기)
        Auction expiredAuction = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .currentPrice(1000L) // 필수값 가정
                .startTime(now.minusHours(2))
                .regularEndTime(now.minusHours(1)) // 이미 끝남
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

        // 저장 (AuctionItem 없이도 저장이 되는지 확인, 안되면 Item도 만들어야 함)
        // 보통 AuctionItem이 필수라면 여기서도 Item을 만들어 넣어줘야 함.
        // 안전하게 Item 없이 저장 시도 (실패 시 엔티티 제약조건 확인 필요)
        auctionRepository.save(expiredAuction);
        auctionRepository.save(activeAuction);

        // when
        List<Auction> result = auctionRepository.findExpiredAuctions(now);

        // then
        // 결과에 만료된 경매가 포함되어야 함
        assertThat(result).extracting("id").contains(expiredAuction.getId());
        // 진행중인 경매는 포함되지 않아야 함
        assertThat(result).extracting("id").doesNotContain(activeAuction.getId());
    }

    @Test
    @DisplayName("경매 상태로 아이템과 함께 조회한다 (Fetch Join 확인)")
    void findFirstWithItemByStatus() {
        // given
        // 1. 경매(Auction)를 먼저 생성하고 저장 (ID 생성)
        Auction auction = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .currentPrice(1000L)
                .startTime(LocalDateTime.now())
                .regularEndTime(LocalDateTime.now().plusHours(1))
                .overtimeStarted(false)
                .build();

        auctionRepository.save(auction);

        // 2. 아이템(AuctionItem) 생성 시 경매(Auction)를 연결 (중요: Owning Side에서 설정)
        AuctionItem item = AuctionItem.builder()
                .itemName("테스트 물품")
                .description("설명")
                .itemImage("image.jpg")
                .auction(auction) // 여기에 아까 만든 경매를 넣어줘야 DB에 FK가 들어갑니다.
                .build();

        // 아이템 저장
        entityManager.persist(item);

        // 영속성 컨텍스트 초기화 (DB에서 진짜로 조회를 다시 해오기 위함)
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Auction> result = auctionRepository.findFirstWithItemByStatus(AuctionStatus.ACTIVE);

        // then
        assertThat(result).isPresent(); // 이제 비어있지 않을 것입니다.
        assertThat(result.get().getAuctionItem()).isNotNull();
        assertThat(result.get().getAuctionItem().getItemName()).isEqualTo("테스트 물품");
    }
}