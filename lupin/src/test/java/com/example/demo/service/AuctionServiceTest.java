package com.example.demo.service;

import com.example.demo.domain.entity.*;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.BidStatus;
import com.example.demo.domain.enums.PointType;
import com.example.demo.dto.response.AuctionBidResponse;
import com.example.demo.dto.response.AuctionStatusResponse;
import com.example.demo.dto.response.OngoingAuctionResponse;
import com.example.demo.dto.response.ScheduledAuctionResponse;
import com.example.demo.repository.AuctionBidRepository;
import com.example.demo.repository.AuctionRepository;
import com.example.demo.repository.PointLogRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.scheduler.AuctionTaskScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
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

    @Mock
    PointLogRepository pointLogRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<Object> rBucket;

    @InjectMocks
    AuctionService auctionService;

    @Test
    void 입찰이_성공하면_현재가와_우승자가_변경되고_입찰기록이_저장된다() {
        // given
        Auction auction = createActiveAuction(100L);
        User user = createUser(10L,"홍길동");

        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
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

        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
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

        given(auctionRepository.findById(auctionId)).willReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> auctionService.placeBid(auctionId, userId, bidAmount, bidTime));

        verify(auctionBidRepository, never()).save(any());
    }

    @Test
    void 없는_유저로_입찰하면_예외가_발생한다() {
        // given
        Auction auction = createActiveAuction(100L);
        given(auctionRepository.findById(100L)).willReturn(Optional.of(auction));
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

        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
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

        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(auctionRepository.findById(2L)).willReturn(Optional.of(auction2));
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

        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
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
// given
        Auction scheduledAuction = Auction.builder()
                .id(2L)
                .status(AuctionStatus.SCHEDULED)
                .startTime(LocalDateTime.now().minusMinutes(1)) // 이미 시작 시간 지남
                .regularEndTime(LocalDateTime.now().plusHours(2)) // [수정] 필수값 추가! (종료 시간 설정)
                .currentPrice(1000L)
                .build();

        given(auctionRepository.findByStatusAndStartTimeBefore(eq(AuctionStatus.SCHEDULED), any()))
                .willReturn(List.of(scheduledAuction));

        // Redis Mocking
        given(redissonClient.getBucket(anyString())).willReturn(rBucket);

        // when
        auctionService.activateScheduledAuctions(LocalDateTime.now());

        // then
        assertThat(scheduledAuction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);
        verify(rBucket, times(1)).set(any());
    }
    @Test
    void 경매시간이_종료된_경매_비활성화_및_낙찰자_포인트_차감() {
// Given
        LocalDateTime now = LocalDateTime.now();

        // 1. 낙찰자(Winner) 생성 (초기 포인트 1000)
        User winner = User.builder()
                .id(1L)
                .name("낙찰자")
                .totalPoints(1000L)
                .build();

        // 2. 종료 대상 경매 생성 (현재가 500원)
        Auction auction = Auction.builder()
                .id(100L)
                .status(AuctionStatus.ACTIVE)
                .winner(winner)
                .currentPrice(500L)
                .regularEndTime(now.minusMinutes(1)) // 이미 종료 시간 지남
                .build();

        // 3. Mock 동작 정의
        // 종료된 경매 목록 조회 시 위 경매 반환
        given(auctionRepository.findExpiredAuctions(any(), any()))
                .willReturn(List.of(auction));

        // 해당 경매의 입찰 내역 조회 (빈 리스트여도 상관없음)
        given(auctionBidRepository.findByAuctionId(auction.getId()))
                .willReturn(List.of());

        // When
        auctionService.closeExpiredAuctions(now);

        // Then
        // 1. 경매 상태가 ENDED로 변경되었는지 확인
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ENDED);

        // 2. 유저의 포인트가 직접 차감되었는지 확인 (1000 - 500 = 500)
        assertThat(winner.getTotalPoints()).isEqualTo(500L);

        // 3. UserRepository.save가 호출되었는지 확인
        verify(userRepository).save(winner);

        // 4. [핵심] PointLogRepository.save가 호출되었는지 확인 (직접 로그 저장)
        ArgumentCaptor<PointLog> pointLogCaptor = ArgumentCaptor.forClass(PointLog.class);
        verify(pointLogRepository).save(pointLogCaptor.capture());

        PointLog savedLog = pointLogCaptor.getValue();
        assertThat(savedLog.getUser()).isEqualTo(winner);
        assertThat(savedLog.getPoints()).isEqualTo(-500L); // 차감액 확인
        assertThat(savedLog.getType()).isEqualTo(PointType.USE); // 타입 확인

        // 5. [핵심] PointService.usePoints는 호출되지 않아야 함 (중복 차감 방지 확인)
        verify(pointService, never()).usePoints(any(), anyLong());

        // 6. 경매 상태 저장 확인
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

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));
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

    @Test
    @DisplayName("진행 중인 경매가 있지만 종료 시간이 지났다면, 강제 종료 로직이 실행되고 null을 반환한다 (Lazy Close)")
    void getOngoingAuctionWithItem_LazyClose() {
        // given
        LocalDateTime now = LocalDateTime.now();
        // 종료 시간이 1분 지난 ACTIVE 경매 생성
        Auction expiredAuction = Auction.builder()
                .id(1L)
                .status(AuctionStatus.ACTIVE)
                .startTime(now.minusHours(1))
                .regularEndTime(now.minusMinutes(1)) // 이미 시간 지남
                .currentPrice(1000L)
                .overtimeStarted(false)
                .build();

        // (Optional) Item 세팅 - 로직상 item 조회 전에 시간 체크를 하지만 안전하게 세팅
        AuctionItem item = createAuctionItem(expiredAuction);
        ReflectionTestUtils.setField(expiredAuction, "auctionItem", item);

        // Mock: Active 경매를 조회했을 때, 위에서 만든 '시간 지난 경매'가 반환됨
        given(auctionRepository.findFirstWithItemByStatus(AuctionStatus.ACTIVE))
                .willReturn(Optional.of(expiredAuction));

        // when
        OngoingAuctionResponse response = auctionService.getOngoingAuctionWithItem();

        // then
        // 1. 결과는 null이어야 함 (프론트엔드에게 '없음'으로 응답)
        assertThat(response).isNull();

        // 2. 내부적으로 closeExpiredAuctions가 호출되어야 함.
        // closeExpiredAuctions 내에서 호출되는 findExpiredAuctions가 실행되었는지 검증하여 간접 확인
        verify(auctionRepository, times(1)).findExpiredAuctions(any(), any());
    }

    @Test
    @DisplayName("진행 중인 경매가 있고 종료 시간이 지나지 않았다면, 정상적으로 정보를 반환한다")
    void getOngoingAuctionWithItem_Success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        // 종료 시간이 10분 남은 ACTIVE 경매
        Auction activeAuction = Auction.builder()
                .id(1L)
                .status(AuctionStatus.ACTIVE)
                .startTime(now.minusMinutes(10))
                .regularEndTime(now.plusMinutes(10)) // 시간 아직 남음
                .currentPrice(1000L)
                .overtimeStarted(false)
                .build();

        AuctionItem item = createAuctionItem(activeAuction);
        ReflectionTestUtils.setField(activeAuction, "auctionItem", item);

        given(auctionRepository.findFirstWithItemByStatus(AuctionStatus.ACTIVE))
                .willReturn(Optional.of(activeAuction));

        // when
        OngoingAuctionResponse response = auctionService.getOngoingAuctionWithItem();

        // then
        assertThat(response).isNotNull();
        assertThat(response.auctionId()).isEqualTo(activeAuction.getId());

        // 강제 종료 로직이 실행되지 않았어야 함
        verify(auctionRepository, never()).findExpiredAuctions(any(), any());
    }

    @Test
    @DisplayName("진행 중인 경매가 아예 없으면 null을 반환한다")
    void getOngoingAuctionWithItem_NoAuction() {
        // given
        given(auctionRepository.findFirstWithItemByStatus(AuctionStatus.ACTIVE))
                .willReturn(Optional.empty());

        // when
        OngoingAuctionResponse response = auctionService.getOngoingAuctionWithItem();

        // then
        assertThat(response).isNull();
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