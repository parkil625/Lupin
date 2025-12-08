package com.example.demo.service;

import com.example.demo.domain.entity.*;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.BidStatus;
import com.example.demo.dto.response.AuctionBidResponse;
import com.example.demo.dto.response.AuctionStatusResponse;
import com.example.demo.dto.response.OngoingAuctionResponse;
import com.example.demo.dto.response.ScheduledAuctionResponse;
import com.example.demo.repository.AuctionBidRepository;
import com.example.demo.repository.AuctionRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionService 테스트")
class AuctionServiceTest {

    @Mock
    AuctionRepository auctionRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    AuctionBidRepository auctionBidRepository;

    @InjectMocks
    AuctionService auctionService;

    @Mock
    PointService pointService;


    @Test
    void 입찰이_성공하면_현재가와_우승자가_변경되고_입찰기록이_저장된다() {

        Auction auction = createActiveAuction(100L);
        User user = createUser(10L,"홍길동");

        given(auctionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(auction));
        given(userRepository.findById(10L)).willReturn(Optional.of(user));
        given(pointService.getTotalPoints(user)).willReturn(200L);


        auctionService.placeBid(1L, 10L, 200L, LocalDateTime.now());
        assertEquals(200L, auction.getCurrentPrice());
        assertEquals(user, auction.getWinner());
        verify(auctionBidRepository).save(any(AuctionBid.class));
    }

    @Test
    void 정규시간_외시간에_입찰하면_입찰이_실패한다(){

        // given
        // 정규 시간이 0:10 ~ 0:20 인 ACTIVE 경매라고 가정
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 0, 0);

        Auction auction = Auction.builder()
                .id(1L)
                .status(AuctionStatus.ACTIVE)
                .currentPrice(100L)
                .startTime(now.plusMinutes(10))      // 0:10 시작
                .regularEndTime(now.plusMinutes(20)) // 0:20 종료
                .overtimeStarted(false)
                .build();

        User user = createUser(10L, "홍길동");

        given(auctionRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(auction));
        given(userRepository.findById(10L))
                .willReturn(Optional.of(user));

        // 시작 전인 시간(정규시간 밖)
        LocalDateTime bidTime = now; // 0:00

        // when & then
        assertThrows(IllegalStateException.class,
                () -> auctionService.placeBid(1L, 10L, 200L, bidTime));

        // 실패했으니 저장되면 안 됨
        verify(auctionBidRepository, never()).save(any());


    }

    @Test
    void 없는_경매에_입찰하면_예외가_발생한다(){

        // given
        Long auctionId = 1L;
        Long userId = 10L;
        Long bidAmount = 200L;
        LocalDateTime bidTime = createLocalDateNow();

        // 경매가 존재하지 않는 상황을 명시적으로 세팅
        given(auctionRepository.findByIdForUpdate(auctionId))
                .willReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> auctionService.placeBid(auctionId, userId, bidAmount, bidTime));

        // 경매가 없으니 입찰 기록은 저장되면 안 됨 (선택이지만 있으면 좋음)
        verify(auctionBidRepository, never()).save(any());
    }

    @Test
    void 없는_유저로_입찰하면_예외가_발생한다() {
        // given
        Auction auction = createActiveAuction(100L);
        Long auctionId = 100L;
        Long id = 10L;
        Long bidAmount = 200L;
        LocalDateTime bidTime = LocalDateTime.of(2025,1,1,0,0);

        given(auctionRepository.findByIdForUpdate(auctionId))
                .willReturn(Optional.of(auction));
        given(userRepository.findById(id)).willReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> auctionService.placeBid(auctionId, id, bidAmount, bidTime));

        verify(auctionBidRepository, never()).save(any());
    }

    @Test
    void 현재가_이하로_입찰하면_예외가_발생한다() {

        //given
        Auction auction = createActiveAuction(100L);
        User user = createUser(10L,"홍길동");
        Long bidAmount = 50L; // 현재가(100L)보다 낮은 금액

        given(auctionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(auction));
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

        // when & then
        assertThrows(IllegalStateException.class,
                () -> auctionService.placeBid(1L, user.getId(), bidAmount, LocalDateTime.now()));

        verify(auctionBidRepository, never()).save(any());
    }

    @Test
    void 상태별_입찰_테스트(){
        //given

        Auction auction = createScheduledAuction();
        Auction auction2 = createEndedAuction();

        User user = createUser(100L,"홍길동");
        Long bidAmount = 100L;


        given(auctionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(auction));
        given(auctionRepository.findByIdForUpdate(2L)).willReturn(Optional.of(auction2));
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

        // when & then
        assertThrows(IllegalStateException.class, () -> auctionService.placeBid(1L, user.getId(), bidAmount, LocalDateTime.now()));
        assertThrows(IllegalStateException.class, () -> auctionService.placeBid(2L, user.getId(), bidAmount, LocalDateTime.now()));


    }

    @Test
    void 최고가입찰시_이전_최고가_상태변경(){
        // given
        Auction auction = createActiveAuction(100L);
        LocalDateTime now = LocalDateTime.now();



        User oldUser = createUser(1L, "옛입찰자");
        User newUser = createUser(2L, "새입찰자");

        // 기존 최고 입찰 (ACTIVE 상태)
        AuctionBid prevBid = AuctionBid.builder()
                .id(999L)
                .auction(auction)
                .user(oldUser)
                .bidAmount(100L)
                .bidTime(now.plusMinutes(10))
                .status(BidStatus.ACTIVE)
                .build();

        // auctionRepository
        given(auctionRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(auction));

        // userRepository
        given(userRepository.findById(2L))
                .willReturn(Optional.of(newUser));

        // 기존 최고 입찰 조회
        given(auctionBidRepository
                .findTopByAuctionAndStatusOrderByBidAmountDescBidTimeDesc(auction, BidStatus.ACTIVE)
        ).willReturn(Optional.of(prevBid));

        given(pointService.getTotalPoints(newUser)).willReturn(200L);

        LocalDateTime bidTime = now.plusMinutes(1); // 00:01, 정규시간 안
        auctionService.placeBid(1L, 2L, 200L, bidTime);

        // then - 기존 입찰이 OUTBID로 바뀌었는지 확인
        assertEquals(BidStatus.OUTBID, prevBid.getStatus());

        // 새 입찰 저장 여부 확인
        verify(auctionBidRepository).save(any(AuctionBid.class));
    }

    @Test
    void 경매시작시간이_다된_경매_활성화() {
        //given
        LocalDateTime now = createLocalDateNow();
        Auction auction = Auction.builder()
                .id(1L)
                .status(AuctionStatus.SCHEDULED)
                .currentPrice(100L)
                .startTime(now)
                .regularEndTime(now.plusMinutes(20))
                .overtimeStarted(false)
                .build();


        given(auctionRepository.findByStatusAndStartTimeBefore(
                AuctionStatus.SCHEDULED,
                now
        )).willReturn(List.of(auction));

        //when
        auctionService.activateScheduledAuctions(now);

        //then
        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
    }

    @Test
    void 경매시간이_종료된_경매_비활성화(){
        //given
        LocalDateTime now = createLocalDateNow();
        User user = createUser(1L,"홍길동");
        Auction auction = Auction.builder()
                .id(1L)
                .status(AuctionStatus.ACTIVE)
                .currentPrice(100L)
                .startTime(now.minusMinutes(40))
                .regularEndTime(now.minusMinutes(30)) // 정규시간 끝났고
                .overtimeStarted(true)
                .overtimeEndTime(now.minusSeconds(1)) // 초읽기도 종료됨!!
                .winner(user)
                .build();

        given(auctionRepository.findExpiredAuctions(now))
                .willReturn(List.of(auction));

        //when
        auctionService.closeExpiredAuctions(now);

        //then
        assertEquals(AuctionStatus.ENDED, auction.getStatus());
    }


    @Test
    void 현재진행중인_경매정보와_경매물품조회_처음페이지입장시(){
        //given
        Auction auction = createActiveAuction(1L);
        AuctionItem auctionItem = createAuctionItem(auction);

        // Auction과 AuctionItem 양방향 연결
        ReflectionTestUtils.setField(auction, "auctionItem", auctionItem);

        given(auctionRepository.findFirstWithItemByStatus(AuctionStatus.ACTIVE)).willReturn(Optional.of(auction));

        //when
        OngoingAuctionResponse response = auctionService.getOngoingAuctionWithItem();

        //then
        assertThat(response.auctionId()).isEqualTo(auction.getId());
        assertThat(response.item().itemName()).isEqualTo(auction.getAuctionItem().getItemName());

    }
    @Test
    void 예정된_경매정보와_경매물품조회_처음페이지입장시(){
        //given
        Auction auction1 = createScheduledAuction2(1L);
        AuctionItem item1 = createAuctionItem(auction1);
        ReflectionTestUtils.setField(auction1, "auctionItem", item1);

        Auction auction2 = createScheduledAuction2(2L);
        AuctionItem item2 = createAuctionItem(auction2);
        ReflectionTestUtils.setField(auction2, "auctionItem", item2);

        given(auctionRepository.findAllByStatusOrderByStartTimeAscWithItem(AuctionStatus.SCHEDULED))
                .willReturn(List.of(auction1, auction2));

        //when
        List<ScheduledAuctionResponse> response = auctionService.scheduledAuctionWithItem();

        //then
        assertThat(response).hasSize(2);

        assertThat(response.get(0).auctionId()).isEqualTo(1L);
        assertThat(response.get(0).item().itemName()).isEqualTo(item1.getItemName());

        assertThat(response.get(1).auctionId()).isEqualTo(2L);
        assertThat(response.get(1).item().itemName()).isEqualTo(item2.getItemName());

    }

    @Test
    void 현재진행중인_경매정보_가지고오기_업데이트된내용(){

        //given
        AuctionStatusResponse auction = new AuctionStatusResponse(
                1L,                 // auctionId
                1000L,              // currentPrice
                "테스트유저",         // winnerName
                true,               // overtimeStarted
                LocalDateTime.now().plusSeconds(30), // overtimeEndTime
                5                   // totalBids
        );

        given(auctionRepository.findAuctionStatus()).willReturn(Optional.of(auction));

        //when
        AuctionStatusResponse response = auctionService.getRealtimeStatus();

        //then
        assertThat(response).isEqualTo(auction);

    }

    @Test
    @DisplayName("진행중인 경매가 없으면 예외가 발생한다")
    void getRealtimeStatus_NotFound() {
        // given
        given(auctionRepository.findAuctionStatus()).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> auctionService.getRealtimeStatus())
                .isInstanceOf(IllegalStateException.class) // 혹은 정의하신 CustomException
                .hasMessage("진행 중인 경매가 없습니다.");
    }


    @Test
    void 낙찰자_낙찰금액만큼_차감(){
// given
        Long auctionId = 1L;
        Long userId = 10L;
        Long bidAmount = 1000L;
        LocalDateTime now = LocalDateTime.now();

        Auction auction = createActiveAuction(500L); // 현재가 500원
        User user = createUser(userId, "입찰자");

        given(auctionRepository.findByIdForUpdate(auctionId)).willReturn(Optional.of(auction));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // 유저의 잔액이 충분하다고 가정 (2000원 보유)
        given(pointService.getTotalPoints(user)).willReturn(2000L);

        // when
        auctionService.placeBid(auctionId, userId, bidAmount, now);

        // then
        // 1. 포인트 차감 메서드가 호출되었는지 검증
        verify(pointService).deductPoints(user, bidAmount);

        // 2. 입찰 기록이 저장되었는지 검증
        verify(auctionBidRepository).save(any(AuctionBid.class));


   }

   @Test
   void 현재_경매_정보_리스트_조회(){
       // given
       User user = createUser(1L, "홍길동");
       Auction auction = createActiveAuction(100L);

       // 1. 가짜 반환값(엔티티 리스트) 만들기
       AuctionBid bid = AuctionBid.builder()
               .user(user)
               .bidAmount(1000L)
               .auction(auction)
               .status(BidStatus.ACTIVE)
               .bidTime(LocalDateTime.now())
               .build();
       List<AuctionBid> mockBids = List.of(bid);

       // 2. Mock 설정: "레포지토리에 find...라고 물어보면 mockBids를 줘라!" (Stubbing)
       given(auctionBidRepository.findBidsByActiveAuction()).willReturn(mockBids);

       // when
       // 서비스 메소드 호출 (서비스는 내부에서 레포지토리의 find...를 부르고, 위에서 정한 mockBids를 받음)
       List<AuctionBidResponse> result = auctionService.getAuctionStatus();

       // then
       assertThat(result).hasSize(1);
       assertThat(result.get(0).getBidAmount()).isEqualTo(1000L);
       assertThat(result.get(0).getUserName()).isEqualTo("홍길동");

       // 레포지토리가 실제로 호출되었는지 확인
       verify(auctionBidRepository).findBidsByActiveAuction();
   }








    // ===========================
    // Helper Methods
    // ===========================

    private Auction createActiveAuction(Long currentPrice) {
        return Auction.builder()
                .id(1L)
                .status(AuctionStatus.ACTIVE)
                .currentPrice(currentPrice)
                .startTime(LocalDateTime.now().minusMinutes(10))
                .regularEndTime(LocalDateTime.now().plusMinutes(10))
                .overtimeStarted(false)
                .build();
    }

    private Auction createScheduledAuction() {
        return Auction.builder()
                .status(AuctionStatus.SCHEDULED)
                .startTime(LocalDateTime.now().plusMinutes(10))
                .overtimeStarted(false)
                .currentPrice(0L)
                .regularEndTime(LocalDateTime.now().plusMinutes(20))
                .build();
    }

    private Auction createScheduledAuction2(Long id) {
        return Auction.builder()
                .id(id)
                .status(AuctionStatus.SCHEDULED)
                .startTime(LocalDateTime.now().plusMinutes(10))
                .overtimeStarted(false)
                .currentPrice(0L)
                .regularEndTime(LocalDateTime.now().plusMinutes(20))
                .build();

    }

    private Auction createEndedAuction(){
        return Auction.builder()
                .status(AuctionStatus.ENDED)
                .startTime(LocalDateTime.now().minusMinutes(10))
                .overtimeStarted(false)
                .currentPrice(100L)
                .winner(null)
                .build();
    }


    private User createUser(Long id, String name) {
        return User.builder()
                .id(id)
                .name(name)
                .build();
    }

    private AuctionItem createAuctionItem(Auction auction) {
        return AuctionItem.builder()
                .id(1L)
                .itemName("경매물품")
                .description("경매 물품 설명")
                .itemImage("아이템이미지경로")
                .auction(auction)
                .build();
    }




    private LocalDateTime createLocalDateNow(){
        return LocalDateTime.now();
    }

}