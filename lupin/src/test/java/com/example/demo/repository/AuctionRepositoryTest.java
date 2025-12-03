package com.example.demo.repository;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionItem;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.response.AuctionStatusResponse;
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

    @Autowired
    private UserRepository userRepository; // 유저 저장을 위해 추가

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

    @Test
    @DisplayName("경매 상태로 아이템과 함께 조회하며 시작시간 순으로 정렬한다 (Fetch Join 확인)")
    void findAllByStatusOrderByStartTimeAscWithItem() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 경매 1 생성 (현재 시작)
        Auction auction1 = Auction.builder()
                .status(AuctionStatus.SCHEDULED)
                .currentPrice(1000L)
                .startTime(now)
                .regularEndTime(now.plusHours(1))
                .build();
        auctionRepository.save(auction1);

        AuctionItem item1 = AuctionItem.builder()
                .itemName("물품1")
                .description("설명1")
                .itemImage("img1.jpg")
                .auction(auction1)
                .build();
        entityManager.persist(item1);

        // 경매 2 생성 (1시간 뒤 시작) - 정렬 확인용
        Auction auction2 = Auction.builder()
                .status(AuctionStatus.SCHEDULED)
                .currentPrice(2000L)
                .startTime(now.plusHours(1)) // 더 늦게 시작
                .regularEndTime(now.plusHours(2))
                .build();
        auctionRepository.save(auction2);

        AuctionItem item2 = AuctionItem.builder()
                .itemName("물품2")
                .description("설명2")
                .itemImage("img2.jpg")
                .auction(auction2)
                .build();
        entityManager.persist(item2);

        // 영속성 컨텍스트 초기화 (실제 DB 조회 및 Fetch Join 동작 확인을 위함)
        entityManager.flush();
        entityManager.clear();

        // when
        List<Auction> result = auctionRepository.findAllByStatusOrderByStartTimeAscWithItem(AuctionStatus.SCHEDULED);

        // then
        assertThat(result).hasSize(2); // 2개가 조회되어야 함

        // 정렬 순서 확인 (auction1 -> auction2)
        assertThat(result.get(0).getId()).isEqualTo(auction1.getId());
        assertThat(result.get(1).getId()).isEqualTo(auction2.getId());

        // Fetch Join 확인 (지연 로딩 없이 데이터가 채워져 있어야 함)
        assertThat(result.get(0).getAuctionItem()).isNotNull();
        assertThat(result.get(0).getAuctionItem().getItemName()).isEqualTo("물품1");

        assertThat(result.get(1).getAuctionItem()).isNotNull();
        assertThat(result.get(1).getAuctionItem().getItemName()).isEqualTo("물품2");
    }

    @Test
    @DisplayName("진행중인 경매 조회 시 가격, 시간, 그리고 입찰자 이름(Join)까지 정확히 가져온다")
    void findAuctionStatus() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 1. 입찰자(User) 생성 및 저장 (필수 필드 채우기)
        User winner = User.builder()
                .userId("testUser")
                .password("password123")
                .name("홍길동") // 우리가 확인할 핵심 데이터
                .role(Role.MEMBER)
                .build();
        userRepository.save(winner);

        // 2. 경매 생성 (Winner 설정 포함)
        Auction auction = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .currentPrice(5500L)            // 가격 검증용
                .startTime(now)
                .regularEndTime(now.plusHours(1))
                .overtimeStarted(true)          // 초읽기 상태 검증용
                .overtimeEndTime(now.plusSeconds(30))
                .winner(winner)                 // ★ 핵심: 승자 연결
                .totalBids(10)
                .build();
        auctionRepository.save(auction);

        // when
        Optional<AuctionStatusResponse> result = auctionRepository.findAuctionStatus();

        // then
        assertThat(result).isPresent();
        AuctionStatusResponse response = result.get();

        // 데이터 정합성 검증
        assertThat(response.currentPrice()).isEqualTo(5500L);
        assertThat(response.overtimeStarted()).isTrue();
        assertThat(response.totalBids()).isEqualTo(10);

        // ★ 핵심 검증: User 엔티티와 Join해서 이름을 잘 가져왔는가?
        assertThat(response.winnerName()).isEqualTo("홍길동");
    }

}
