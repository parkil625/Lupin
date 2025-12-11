package com.example.demo.domain.entity;

import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.BidStatus;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    // ===========================
    // 테스트 메서드들
    // ===========================

    @Test
    void schedule상태이고_경매시간이된_경매_active로하기() {
        // given
        Auction auction = createScheduledAuction(
                LocalDateTime.of(2025, 11, 26, 0, 0, 0),
                LocalDateTime.of(2026, 11, 28, 0, 0, 0)
        );

        LocalDateTime now = auction.getStartTime().plusMinutes(1);

        // when & then
        assertDoesNotThrow(() -> auction.activate(now));
        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
    }

    @Test
    void 경매시간이_아닐때의_경매active하기() {
        // given
        Auction auction = createScheduledAuction(
                LocalDateTime.of(2025, 11, 26, 0, 0, 0),
                LocalDateTime.of(2026, 11, 28, 0, 0, 0)
        );
        LocalDateTime now = auction.getStartTime().minusMinutes(1);

        // when & then
        assertThrows(IllegalStateException.class, () -> auction.activate(now));
        assertNotEquals(AuctionStatus.ACTIVE, auction.getStatus());
    }

    @Test
    void active상태이고_경매시간이_끝난_경매_종료상태로하기() {
        // given
        User winner = createUser("winner");

        Auction auction = createActiveAuction(
                LocalDateTime.of(2025, 11, 26, 0, 0, 0),
                LocalDateTime.of(2025, 11, 27, 0, 0, 0),
                winner
        );

        List<AuctionBid> bids = new ArrayList<>();

        // when
        assertDoesNotThrow(() -> auction.deactivate(bids));

        // then
        assertEquals(AuctionStatus.ENDED, auction.getStatus());
    }

    @Test
    void 정해진시간이_아닐때는_입찰이불가능하다() {
        // given
        Auction auction = createScheduledAuction(
                LocalDateTime.of(2025, 11, 26, 0, 0, 0),
                LocalDateTime.of(2026, 11, 28, 0, 0, 0)
        );

        // when & then
        assertThrows(IllegalStateException.class,
                () -> auction.validateTime(LocalDateTime.now()));
    }

    @Test
    void 입찰금액은_null이_아니며_현재가보다_높아야한다() {
        // given
        Auction auction = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .startTime(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                .overtimeStarted(true)
                .currentPrice(10_000_000L)
                .regularEndTime(LocalDateTime.of(2026, 1, 1, 0, 10, 0))
                .build();

        // when & then
        assertThrows(IllegalStateException.class,
                () -> auction.validateBid(100L));
    }

    @Test
    void 시간이_다끝났을때_제일높은_금액이_winner() {
        // given
        Auction auction = createActiveAuction(
                LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 10, 0)
        );

        User user = createUser("홍길동");
        User user1 = createUser("홍길동1");

        List<AuctionBid> bids = new ArrayList<>();

        // when
        auction.placeBid(user, 100L, LocalDateTime.of(2025, 1, 1, 1, 0, 0));
        auction.placeBid(user1, 200L, LocalDateTime.of(2025, 1, 1, 1, 0, 0));
        auction.placeBid(user, 300L, LocalDateTime.of(2025, 1, 1, 1, 0, 0));

        auction.deactivate(bids);

        // then
        assertEquals(user, auction.getWinner());
    }

    @Test
    void 자기가_최고가였는데_밀리면_입찰상태_OUTBID로_만들기() {
        // given
        Auction auction = createActiveAuction(
                LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 10, 0)
        );
        User first = createUser("첫입찰자");
        User second = createUser("두번째입찰자");

        // when
        auction.placeBid(first, 100L, LocalDateTime.of(2025, 1, 1, 1, 0, 0));
        auction.placeBid(second, 200L, LocalDateTime.of(2025, 1, 1, 1, 3, 0));

        // then
        // TODO: Auction이 bids 컬렉션을 갖고 있다면 거기서 상태를 꺼내서 검증
        // 예: assertEquals(BidStatus.OUTBID, auction.getBids().get(0).getStatus());
    }

    @Test
    void 초읽기시간에_입찰이성공되면_30초가_리셋된다() {
        // given
        Auction auction = createActiveAuction(
                LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                LocalDateTime.of(2025, 1, 1, 0, 10, 0)
        );

        LocalDateTime overtimeStartTime = LocalDateTime.of(2025, 1, 1, 0, 10, 1);
        auction.startOvertime(overtimeStartTime);

        LocalDateTime oldEndTime = auction.getOvertimeEndTime();

        User user = createUser("홍길동");
        LocalDateTime bidTime = overtimeStartTime.plusSeconds(5);

        // when
        auction.placeBid(user, 200L, bidTime);

        // then
        assertThat(auction.getOvertimeEndTime())
                .as("입찰 발생 시 초읽기 종료 시간이 30초 뒤로 리셋되어야 한다.")
                .isAfter(oldEndTime);

        assertThat(auction.getOvertimeEndTime())
                .isEqualTo(bidTime.plusSeconds(30));
    }

    @Test
    void 초읽기_시간_종료시_winner를_제외한_입찰상태Lost만들기() {
        // given
        User winner = createUser2(1L,"winner");
        User loser = createUser2(2L,"loser");

        Auction auction = createActiveAuction(
                LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                LocalDateTime.of(2025, 10, 1, 0, 10, 0),
                winner
        );

        AuctionBid winnerBid = createBid(winner, auction, 10_000L, BidStatus.ACTIVE);
        AuctionBid otherBid = createBid(loser, auction, 1_000L, BidStatus.ACTIVE);

        List<AuctionBid> bids = List.of(winnerBid, otherBid);

        // when
        otherBid.outBid();
        auction.deactivate(bids);

        // then
        assertEquals(AuctionStatus.ENDED, auction.getStatus());
        assertEquals(BidStatus.WINNING, winnerBid.getStatus());
        assertEquals(BidStatus.LOST, otherBid.getStatus());
    }

    @Test
    void 입찰금액에_이상한값() {
        // given
        User user = createUser("홍길동");
        Auction auction = createActiveAuction(
                LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 10, 0)
        );

        AuctionBid invalidBid = AuctionBid.builder()
                .user(user)
                .auction(auction)
                .bidAmount(null)
                .build();

        // when & then
        assertThrows(IllegalStateException.class,
                () -> auction.placeBid(user, invalidBid.getBidAmount(), invalidBid.getBidTime()));
    }

    @Test
    void 입찰생성_하기() {
        // given
        User user = createUser("홍길동");
        Auction auction = createActiveAuction(
                LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 10, 0)
        );

        Long bidAmount = 1_000L;
        LocalDateTime bidTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

        // when
        AuctionBid bid = auction.createBid(user, bidAmount, bidTime);

        // then
        assertNotNull(bid);
        assertEquals(bidAmount, bid.getBidAmount());
        assertEquals(user, bid.getUser());
    }

    @Test
    @DisplayName("정규 종료 시간이 임박했을 때(30초 미만), 입찰하면 초읽기가 시작된다")
    void placeBid_triggers_overtime() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Auction auction = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .startTime(now.minusMinutes(10))
                .regularEndTime(now.plusMinutes(10)) // 10분 뒤 종료
                .overtimeSeconds(30)
                .overtimeStarted(false)
                .build();

        User user = User.builder().id(1L).build();

        // 💡 [핵심 수정 포인트]
        // 이전 코드: regularEndTime.plusSeconds(1) -> 이미 끝난 시간이라 에러 발생 (정상)
        // 수정 코드: regularEndTime.minusSeconds(10) -> 종료 10초 전 (초읽기 발동 조건)
        LocalDateTime bidTime = auction.getRegularEndTime().minusSeconds(10);

        // when
        auction.placeBid(user, 1000L, bidTime);

        // then
        // 1. 초읽기 모드가 켜졌는지 확인
        assertThat(auction.getOvertimeStarted()).isTrue();

        // 2. 종료 시간이 '입찰 시간 + 30초'로 잘 연장되었는지 확인
        assertThat(auction.getOvertimeEndTime()).isEqualTo(bidTime.plusSeconds(30));
    }

    @Test
    @DisplayName("이미 초읽기 진행 중이라면, 입찰 시 종료 시간이 연장된다")
    void placeBid_extends_overtime() {
        // given
        LocalDateTime now = LocalDateTime.now();
        User bidder = createDummyUser(3L);

        Auction auction = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .currentPrice(1000L)
                .regularEndTime(now.minusHours(1))
                .overtimeStarted(true) // 이미 초읽기 중
                .overtimeSeconds(30)   // 연장 단위 30초
                .overtimeEndTime(now.plusSeconds(10)) // 원래 10초 남았음
                .build();

        // when
        auction.placeBid(bidder, 2000L, now);

        // then
        assertThat(auction.getCurrentPrice()).isEqualTo(2000L);

        // [핵심] 원래 10초 남았었지만, 방금 입찰했으므로 (now + 30초)로 늘어나야 함
        assertThat(auction.getOvertimeEndTime()).isEqualTo(now.plusSeconds(30));
    }
    // 테스트용 경매를 쉽게 만들기 위한 도우미 메소드
    private Auction createAuction(LocalDateTime startTime, LocalDateTime regularEndTime) {
        return Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .startTime(startTime)
                .regularEndTime(regularEndTime)
                .currentPrice(1000L)
                .overtimeSeconds(30)
                .overtimeStarted(false)
                .build();
    }

    @Test
    @DisplayName("1. [정상] 정규 시간 내 입찰 (마감 임박 아님) -> 초읽기 발동 안 함")
    void bid_regular_time_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        // 10분 전 시작 ~ 10분 후 종료 (시간 넉넉함)
        Auction auction = createAuction(now.minusMinutes(10), now.plusMinutes(10));
        User user = User.builder().id(1L).name("테스트유저").build();
        LocalDateTime bidTime = now;

        // when
        auction.placeBid(user, 2000L, bidTime);

        // then
        assertThat(auction.getCurrentPrice()).isEqualTo(2000L); // 가격 변경됨?
        assertThat(auction.getWinner()).isEqualTo(user);        // 우승자 변경됨?
        assertThat(auction.getOvertimeStarted()).isFalse();     // 초읽기는 꺼져있어야 함
    }

    @Test
    @DisplayName("2. [정상] 정규 마감 30초 전 입찰 -> 초읽기 발동 (Overtime Started)")
    void bid_triggers_overtime() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Auction auction = createAuction(now.minusMinutes(10), now.plusMinutes(10));
        User user = User.builder().id(1L).build();

        // 상황: 종료 10초 전 입찰! (30초 이내 조건 충족)
        LocalDateTime bidTime = auction.getRegularEndTime().minusSeconds(10);

        // when
        auction.placeBid(user, 2000L, bidTime);

        // then
        assertThat(auction.getOvertimeStarted()).isTrue(); // 초읽기 켜짐!
        // 종료 시간 = 입찰시간 + 30초 로 늘어났는지 확인
        assertThat(auction.getOvertimeEndTime()).isEqualTo(bidTime.plusSeconds(30));
    }

    @Test
    @DisplayName("3. [좀비 방지] 정규 시간이 1초라도 지났는데 입찰 -> 예외 발생 (절대 받아주면 안 됨)")
    void bid_after_regular_end_fails() {
        // given
        LocalDateTime now = LocalDateTime.now();
        // 10분 전에 시작해서, 1분 전에 이미 끝난 경매
        Auction auction = createAuction(now.minusMinutes(20), now.minusMinutes(1));
        User user = User.builder().id(1L).build();

        // 상황: 종료 시간보다 1초 늦게 입찰 시도
        LocalDateTime bidTime = auction.getRegularEndTime().plusSeconds(1);

        // when & then
        // "입찰하면 에러가 터져야 한다!"라고 검증
        assertThatThrownBy(() -> auction.placeBid(user, 2000L, bidTime))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("정규 경매 시간이 종료되었습니다.");
    }

    @Test
    @DisplayName("4. [정상] 초읽기 진행 중 입찰 -> 종료 시간 계속 연장 (리셋)")
    void bid_during_overtime_extends_time() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Auction auction = createAuction(now.minusMinutes(30), now.plusMinutes(1));
        User user1 = User.builder().id(1L).build();
        User user2 = User.builder().id(2L).build();

        // [1단계] 먼저 마감 직전에 입찰해서 초읽기를 켬
        LocalDateTime triggerTime = auction.getRegularEndTime().minusSeconds(10);
        auction.placeBid(user1, 2000L, triggerTime);

        assertThat(auction.getOvertimeStarted()).isTrue(); // 초읽기 진입 확인
        LocalDateTime firstOvertimeEnd = auction.getOvertimeEndTime();

        // [2단계] 늘어난 시간 안에 다른 사람이 또 입찰
        LocalDateTime secondBidTime = triggerTime.plusSeconds(10);
        auction.placeBid(user2, 3000L, secondBidTime);

        // then
        assertThat(auction.getCurrentPrice()).isEqualTo(3000L);
        assertThat(auction.getWinner()).isEqualTo(user2);

        // 종료 시간이 '두 번째 입찰 시간 + 30초'로 더 늘어났는지 확인
        assertThat(auction.getOvertimeEndTime()).isEqualTo(secondBidTime.plusSeconds(30));
        // 당연히 첫 번째 종료 시간보다 더 뒤여야 함
        assertThat(auction.getOvertimeEndTime()).isAfter(firstOvertimeEnd);
    }

    @Test
    @DisplayName("5. [예외] 이미 ENDED 상태인 경매에 입찰 시도 -> 무조건 실패")
    void bid_ended_auction_fails() {
        // given
        Auction auction = Auction.builder()
                .status(AuctionStatus.ENDED) // 이미 끝난 상태
                .currentPrice(1000L)
                .build();
        User user = User.builder().id(1L).build();

        // when & then
        assertThatThrownBy(() -> auction.placeBid(user, 2000L, LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("경매가 종료되었습니다.");
    }


    // ===========================
    // Helper Methods
    // ===========================
    private User createDummyUser(Long id) {
        return User.builder().id(id).name("테스터" + id).build();
    }


    private User createUser(String name) {
        return User.builder()
                .name(name)
                .role(Role.MEMBER)
                .height(170.1)
                .weight(100.0)
                .gender("남")
                .birthDate(LocalDate.of(2003, 12, 1))
                .department("영업")
                .build();
    }

    private User createUser2(Long id ,String name) {
        return User.builder()
                .id(id)
                .name(name)
                .role(Role.MEMBER)
                .height(170.1)
                .weight(100.0)
                .gender("남")
                .birthDate(LocalDate.of(2003, 12, 1))
                .department("영업")
                .build();
    }

    private Auction createScheduledAuction(LocalDateTime start, LocalDateTime end) {
        return Auction.builder()
                .status(AuctionStatus.SCHEDULED)
                .startTime(start)
                .overtimeStarted(false)
                .currentPrice(0L)
                .regularEndTime(end)
                .build();
    }

    private Auction createActiveAuction(LocalDateTime start, LocalDateTime end) {
        return Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .startTime(start)
                .overtimeStarted(false)
                .currentPrice(0L)
                .regularEndTime(end)
                .build();
    }

    private Auction createActiveAuction(LocalDateTime start, LocalDateTime end, User winner) {
        return Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .startTime(start)
                .overtimeStarted(false)
                .currentPrice(0L)
                .regularEndTime(end)
                .winner(winner)
                .build();
    }

    private AuctionBid createBid(User user, Auction auction, Long amount, BidStatus status) {
        return AuctionBid.builder()
                .user(user)
                .auction(auction)
                .bidAmount(amount)
                .bidTime(LocalDateTime.now())
                .status(status)
                .build();
    }
}
