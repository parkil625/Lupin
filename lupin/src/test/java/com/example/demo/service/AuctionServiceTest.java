package com.example.demo.service;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.domain.entity.AuctionItem;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.BidStatus;
import com.example.demo.repository.AuctionBidRepository;
import com.example.demo.repository.AuctionRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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


    @Test
    void 입찰이_성공하면_현재가와_우승자가_변경되고_입찰기록이_저장된다() {

        Auction auction = createActiveAuction(100L);
        User user = createUser(10L,"홍길동");

        given(auctionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(auction));
        given(userRepository.findById(10L)).willReturn(Optional.of(user));

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
        AuctionItem auctionItem = createAuctionItem(1L);

        given(auctionRepository.findFirstWithItemByStatus(AuctionStatus.ACTIVE)).willReturn(Optional.of(auction));

        //when
        auctionService.getOngoingAuctionWithItem();

    }
//    @Test
//    void 예정된_경매정보와_경매물품조회_처음페이지입장시(){
//
//    }
//
//    @Test
//    void 현재진행중인_경매가_없다면_null또는_예외처리발생(){
//
//    }
//
//    @Test
//    void 현재진행중인_경매정보_가지고오기_업데이트된내용(){
//
//    }
//
//
//    @Test
//    void 낙찰자_낙찰금액만큼_차감(){
//
//    }
//
//    @Test
//    void 입찰내역조회(){
//
//    }








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

    private AuctionItem createAuctionItem(Long auctionId) {
        return AuctionItem.builder()
                .id(auctionId)
                .itemName("경매물품")
                .description("경매 물품 설명")
                .itemImage("아이템이미지경로")
                .build();
    }


    private LocalDateTime createLocalDateNow(){
        return LocalDateTime.now();
    }

}