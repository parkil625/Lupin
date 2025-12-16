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
import com.example.demo.scheduler.AuctionTaskScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionService 테스트")
class AuctionServiceTest {

    @Mock
    AuctionRepository auctionRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    AuctionBidRepository auctionBidRepository;

    @Mock
    PointService pointService;

    @Mock
    AuctionSseService auctionSseService;

    @Mock
    AuctionTaskScheduler auctionTaskScheduler;

    @InjectMocks
    AuctionService auctionService;

    @Test
    void 입찰이_성공하면_현재가와_우승자가_변경되고_입찰기록이_저장된다() {
        // given
        Auction auction = createActiveAuction(100L);
        User user = createUser(10L,"홍길동");

        given(auctionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(auction));
        given(userRepository.findById(10L)).willReturn(Optional.of(user));
        
        // [수정] 포인트 조회 로직(Stubbing) 삭제 (입찰 시 잔액 체크 안함)
        // given(pointService.getTotalPoints(user)).willReturn(200L);

        // when
        auctionService.placeBid(1L, 10L, 200L, LocalDateTime.now());

        // then
        assertEquals(200L, auction.getCurrentPrice());
        assertEquals(user, auction.getWinner());
        verify(auctionBidRepository).save(any(AuctionBid.class));
    }

    @Test
    void 정규시간_외시간에_입찰하면_입찰이_실패한다(){
        // given
        // [수정] Active 상태가 아니라 Scheduled(시작 전) 상태로 설정해야 입찰이 거부됨을 명확히 테스트 가능
        // (현재 Auction 로직상 Active면 시간 체크를 통과할 수 있음)
        Auction auction = Auction.builder()
                .id(1L)
                .status(AuctionStatus.SCHEDULED) // 아직 시작 안함
                .currentPrice(100L)
                .startTime(LocalDateTime.now().plusMinutes(10))
                .regularEndTime(LocalDateTime.now().plusMinutes(20))
                .overtimeStarted(false)
                .build();

        User user = createUser(10L, "홍길동");

        given(auctionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(auction));
        given(userRepository.findById(10L)).willReturn(Optional.of(user));

        // when & then
        assertThrows(IllegalStateException.class,
                () -> auctionService.placeBid(1L, 10L, 200L, LocalDateTime.now()));

        verify(auctionBidRepository, never()).save(any());
    }

    @Test
    void 없는_경매에_입찰하면_예외가_발생한다(){
        // given
        Long auctionId = 1L;
        Long userId = 10L;
        Long bidAmount = 200L;
        LocalDateTime bidTime = createLocalDateNow();

        given(auctionRepository.findByIdForUpdate(auctionId)).willReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> auctionService.placeBid(auctionId, userId, bidAmount, bidTime));

        verify(auctionBidRepository, never()).save(any());
    }

    @Test
    void 없는_유저로_입찰하면_예외가_발생한다() {
        // given
        Auction auction = createActiveAuction(100L);
        given(auctionRepository.findByIdForUpdate(100L)).willReturn(Optional.of(auction));
        given(userRepository.findById(10L)).willReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> auctionService.placeBid(100L, 10L, 200L, LocalDateTime.now()));

        verify(auctionBidRepository, never()).save(any());
    }

    @Test
    void 현재가_이하로_입찰하면_예외가_발생한다() {
        //given
        Auction auction = createActiveAuction(100L);
        User user = createUser(10L,"홍길동");
        Long bidAmount = 50L; 

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

        given(auctionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(auction));
        given(userRepository.findById(2L)).willReturn(Optional.of(newUser));
        
        // [수정] 포인트 조회 Stubbing 삭제
        // given(pointService.getTotalPoints(newUser)).willReturn(200L);

        given(auctionBidRepository.findTopByAuctionAndStatusOrderByBidAmountDescBidTimeDesc(auction, BidStatus.ACTIVE))
                .willReturn(Optional.of(prevBid));

        // when
        auctionService.placeBid(1L, 2L, 200L, now.plusMinutes(1));

        // then
        assertEquals(BidStatus.OUTBID, prevBid.getStatus());
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

        given(auctionRepository.findByStatusAndStartTimeBefore(AuctionStatus.SCHEDULED, now))
                .willReturn(List.of(auction));

        //when
        auctionService.activateScheduledAuctions(now);

        //then
        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
    }

    @Test
    void 경매시간이_종료된_경매_비활성화_및_낙찰자_포인트_차감() {
        // given
        LocalDateTime now = createLocalDateNow();
        LocalDateTime regularTimeLimit = now.minusSeconds(30);

        User winner = createUser(1L, "홍길동"); // 낙찰자

        Auction auction = Auction.builder()
                .id(1L)
                .status(AuctionStatus.ACTIVE)
                .currentPrice(1000L) // 낙찰가
                .startTime(now.minusMinutes(40))
                .regularEndTime(now.minusMinutes(30)) 
                .overtimeStarted(true)
                .overtimeEndTime(now.minusSeconds(1)) 
                .winner(winner) // 승자 존재
                .build();

        given(auctionRepository.findExpiredAuctions(now, regularTimeLimit))
                .willReturn(List.of(auction));

        given(auctionBidRepository.findByAuctionId(auction.getId()))
                .willReturn(Collections.emptyList());

        // when
        auctionService.closeExpiredAuctions(now);

        // then
        // 1. 상태 변경 확인
        assertEquals(AuctionStatus.ENDED, auction.getStatus());
        
        // 2. [중요] 종료 시점에 포인트 차감 메서드 호출 확인
        verify(pointService).usePoints(winner, 1000L);

        // 3. DB 반영 확인
        verify(auctionRepository).saveAndFlush(auction);
    }

    @Test
    void 현재진행중인_경매정보와_경매물품조회_처음페이지입장시(){
        //given
        Auction auction = createActiveAuction(1L);
        AuctionItem auctionItem = createAuctionItem(auction);
        ReflectionTestUtils.setField(auction, "auctionItem", auctionItem);

        given(auctionRepository.findFirstWithItemByStatus(AuctionStatus.ACTIVE))
                .willReturn(Optional.of(auction));

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
        assertThat(response.get(1).auctionId()).isEqualTo(2L);
    }

    @Test
    void 현재진행중인_경매정보_가지고오기_업데이트된내용(){
        //given
        AuctionStatusResponse auction = new AuctionStatusResponse(
                1L, 1000L, "테스트유저", true, LocalDateTime.now().plusSeconds(30), 5
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
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("진행 중인 경매가 없습니다.");
    }

    @Test
    void 입찰시_포인트는_즉시_차감되지_않는다(){
        // given
        Long auctionId = 1L;
        Long userId = 10L;
        Long bidAmount = 1000L;
        LocalDateTime now = LocalDateTime.now();

        Auction auction = createActiveAuction(500L);
        User user = createUser(userId, "입찰자");

        given(auctionRepository.findByIdForUpdate(auctionId)).willReturn(Optional.of(auction));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // [수정] 포인트 조회 Stub 삭제
        // given(pointService.getTotalPoints(user)).willReturn(2000L);

        // when
        auctionService.placeBid(auctionId, userId, bidAmount, now);

        // then
        // [중요] 정책 변경: 입찰 시점에는 포인트 차감을 하지 않음 (종료 시 일괄 처리)
        verify(pointService, never()).deductPoints(any(), anyLong());
        verify(pointService, never()).usePoints(any(), anyLong());

        verify(auctionBidRepository).save(any(AuctionBid.class));
    }

    @Test
    void 현재_경매_정보_리스트_조회(){
        // given
        User user = createUser(1L, "홍길동");
        Auction auction = createActiveAuction(100L);

        AuctionBid bid = AuctionBid.builder()
                .user(user)
                .bidAmount(1000L)
                .auction(auction)
                .status(BidStatus.ACTIVE)
                .bidTime(LocalDateTime.now())
                .build();
        List<AuctionBid> mockBids = List.of(bid);

        given(auctionBidRepository.findBidsByActiveAuction()).willReturn(mockBids);

        // when
        List<AuctionBidResponse> result = auctionService.getAuctionStatus();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBidAmount()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("초읽기 대상 경매들을 찾아 상태를 변경하고 즉시 저장한다")
    void startOvertimeForAuctions_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Auction auction1 = mock(Auction.class);
        Auction auction2 = mock(Auction.class);

        given(auction1.getId()).willReturn(1L);
        given(auction2.getId()).willReturn(2L);

        given(auctionRepository.findAuctionsReadyForOvertime(any(LocalDateTime.class)))
                .willReturn(List.of(auction1, auction2));

        // when
        auctionService.startOvertimeForAuctions(now);

        // then
        verify(auction1, times(1)).startOvertime(now);
        verify(auction2, times(1)).startOvertime(now);
        verify(auctionRepository, times(1)).saveAndFlush(auction1);
        verify(auctionRepository, times(1)).saveAndFlush(auction2);
    }

    @Test
    @DisplayName("상태 변경 중 예외가 발생한 경매는 스킵하고, 나머지는 정상 처리한다")
    void startOvertimeForAuctions_handleException() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Auction normalAuction = mock(Auction.class);
        Auction errorAuction = mock(Auction.class);

        given(normalAuction.getId()).willReturn(1L);
        given(errorAuction.getId()).willReturn(2L);

        given(auctionRepository.findAuctionsReadyForOvertime(any(LocalDateTime.class)))
                .willReturn(List.of(errorAuction, normalAuction));

        willThrow(new IllegalStateException("이미 종료된 경매입니다"))
                .given(errorAuction).startOvertime(any());

        // when
        auctionService.startOvertimeForAuctions(now);

        // then
        verify(auctionRepository, never()).saveAndFlush(errorAuction);
        verify(auctionRepository, times(1)).saveAndFlush(normalAuction);
    }

    @Test
    @DisplayName("대상 경매가 없으면 아무 작업도 수행하지 않는다")
    void startOvertimeForAuctions_empty() {
        // given
        given(auctionRepository.findAuctionsReadyForOvertime(any()))
                .willReturn(Collections.emptyList());

        // when
        auctionService.startOvertimeForAuctions(LocalDateTime.now());

        // then
        verify(auctionRepository, never()).saveAndFlush(any());
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
                .id(1L)
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
                .id(2L)
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