package com.example.demo.domain.entity;

import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.BidStatus;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {
    LocalDateTime now = LocalDateTime.now();

    //예정된 경매
    Auction auction1 = Auction.builder()
            .status(AuctionStatus.SCHEDULED)
            .startTime(LocalDateTime.of(2025, 11, 26, 0, 0, 0))
            .overtimeStarted(false)
            .regularEndTime(
                    LocalDateTime.of(2025, 11, 28, 0, 0, 0)
            )
            .build();

    //진행중인 경매 준비된 입찰 없는 버전
    Auction auction2 = Auction.builder()
            .status(AuctionStatus.ACTIVE)
            .startTime(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
            .overtimeStarted(false)
            .regularEndTime(LocalDateTime.of(2026, 1, 1, 0, 10, 0))
            .build();

    //진행중인 경매 준비된 입찰 있는 버전
    Auction auction3 = Auction.builder()
            .status(AuctionStatus.ACTIVE)
            .startTime(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
            .overtimeStarted(true)
            .currentPrice(10000000L)
            .regularEndTime(LocalDateTime.of(2026, 1, 1, 0, 10, 0))
            .build();

    //스케쥴상태 경매
    Auction auction4 = Auction.builder()
            .status(AuctionStatus.SCHEDULED)
            .startTime(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
            .overtimeStarted(false)
            .currentPrice(0L)
            .regularEndTime(LocalDateTime.of(2026, 1, 1, 0, 10, 0))
            .build();

    //유저1
    User user = User.builder()
            .name("홍길동")
            .role(Role.MEMBER)
            .height(170.1)
            .weight(100.0)
            .gender("남")
            .birthDate(LocalDate.of(2003,12,1))
            .department("영업")
            .build();

    //경매 물품
    AuctionItem auctionItem = AuctionItem.builder()
            .itemName("100만원 상당의 100만원")
            .description("설명하는 글")
            .auction(auction1)
            .build();

    //경매 입찰
    AuctionBid auctionBid1 = AuctionBid.builder()
            .user(user)
            .auction(auction2)
            .bidAmount(100000L)
            .bidTime(LocalDateTime.now())
            .status(BidStatus.ACTIVE)
            .build();

    @Test
    void schedule상태이고_경매시간이된_경매_active로하기() {

        LocalDateTime now = auction1.getStartTime().plusMinutes(1);


        assertDoesNotThrow(() -> auction1.activate(now));
        assertEquals(AuctionStatus.ACTIVE, auction1.getStatus());
    }

    @Test
    void active상태이고_경매시간이_끝난_경매_종료상태로하기() {

        List<AuctionBid> bids = new ArrayList<>();

        Auction auction1 = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .startTime(LocalDateTime.of(2025, 11, 26, 0, 0, 0))
                .overtimeStarted(false)
                .regularEndTime(
                        LocalDateTime.of(2025, 11, 27, 0, 0, 0)
                )
                .winner(user)
                .build();


        assertDoesNotThrow(() -> auction1.deactivate(bids));
        assertEquals(AuctionStatus.ENDED, auction1.getStatus());
    }


    @Test
    void 정해진시간이_아닐때는_입찰이불가능하다() {
    //        경매 입찰의 시간을 받아서 canBid에 주기

        assertThrows(IllegalStateException.class,() -> auction1.validateTime(LocalDateTime.now()));

    }

    @Test
    void 입찰금액은_null이_아니며_현재가보다_높아야한다(){

        assertThrows(IllegalStateException.class,() -> auction3.validateBid(100L));

    }

    @Test
    void 시간이_다끝났을때_제일높은_금액이_winner(){
        //given
        List<AuctionBid> bids = new ArrayList<>();

        Auction auction2 = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .startTime(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                .overtimeStarted(false)
                .regularEndTime(LocalDateTime.of(2026, 1, 1, 0, 10, 0))
                .build();


        User user = User.builder()
                .name("홍길동")
                .build();
        User user1 = User.builder()
                .name("홍길동1")
                .build();

        //when

        //1. 경매 입찰 조건좀 보고 최고가면 현재가에 업데이트 해주고 그 사람 고정
        auction2.placeBid(user,100L,LocalDateTime.of(2025, 1, 1, 1, 0, 0));
        auction2.placeBid(user1,200L,LocalDateTime.of(2025, 1, 1, 1, 0, 0));
        auction2.placeBid(user,300L,LocalDateTime.of(2025, 1, 1, 1, 0, 0));

        auction2.deactivate(bids);

        //then
        assertEquals(user, auction2.getWinner());

    }


    @Test
    void 자기가_최고가였는데_밀리면_입찰상태_OUTBID로_만들기(){

        //given
        Auction auction1 = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .startTime(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                .overtimeStarted(false)
                .regularEndTime(LocalDateTime.of(2026, 1, 1, 0, 10, 0))
                .winner(user)
                .build();

        //when
        auction1.placeBid(user,100L,LocalDateTime.of(2025, 1, 1, 1, 0, 0));
        auction1.placeBid(user,200L,LocalDateTime.of(2025, 1, 1, 1, 3, 0));



    }

    @Test
    void 초읽기시간에_입찰이성공되면_30초가_리셋된다() {
        //given
        Auction auction = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .startTime(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                .overtimeStarted(false)
                .regularEndTime(LocalDateTime.of(2025, 1, 1, 0, 10, 0))
                .build();

        // 정규 시간이 지난 시점
        LocalDateTime overtimeStartTime = LocalDateTime.of(2025, 1, 1, 0, 10, 1);

        // 초읽기 시작
        auction.startOvertime(overtimeStartTime);

        LocalDateTime oldEndTime = auction.getOvertimeEndTime();

        //when - 초읽기 내에서 입찰 발생
        LocalDateTime bidTime = overtimeStartTime.plusSeconds(5);
        auction.placeBid(user, 200L, bidTime);

        //then - 30초가 리셋됐는지 확인
        assertThat(auction.getOvertimeEndTime())
                .isAfter(oldEndTime)
                .as("입찰 발생 시 초읽기 종료 시간이 30초 뒤로 리셋되어야 한다.");

        assertThat(auction.getOvertimeEndTime())
                .isEqualTo(bidTime.plusSeconds(30));
    }

    @Test
    void 초읽기_시간_종료시_winner를_제외한_입찰상태Lost만들기(){
        //given

        User winner1 = User.builder()
                .name("홍길동")
                .role(Role.MEMBER)
                .height(170.1)
                .weight(100.0)
                .gender("남")
                .birthDate(LocalDate.of(2003,12,1))
                .department("영업")
                .build();


        User loser = User.builder()
                .name("홍길동")
                .role(Role.MEMBER)
                .height(170.1)
                .weight(100.0)
                .gender("남")
                .birthDate(LocalDate.of(2003,12,1))
                .department("영업")
                .build();


        Auction auction2 = Auction.builder()
                .status(AuctionStatus.ACTIVE)
                .startTime(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                .overtimeStarted(false)
                .regularEndTime(LocalDateTime.of(2025, 10, 1, 0, 10, 0))
                .winner(winner1)
                .build();





        AuctionBid winnerBid = AuctionBid.builder().user(winner1).auction(auction2).bidAmount(10000L).build();



        AuctionBid otherBid = AuctionBid.builder().user(loser).auction(auction2).bidAmount(1000L).status(BidStatus.ACTIVE).build();


        List<AuctionBid> bids = List.of(winnerBid, otherBid);

        // when
        otherBid.outBid();
        auction2.deactivate(bids);

        // then
        assertEquals(AuctionStatus.ENDED, auction2.getStatus());
        assertEquals(BidStatus.WINNING, winnerBid.getStatus());
        assertEquals(BidStatus.LOST, otherBid.getStatus());


    }



}