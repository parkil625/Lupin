package com.example.demo.service;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AuctionStatus;
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

    private User createUser(Long id, String name) {
        return User.builder()
                .id(id)
                .name(name)
                .build();
    }

    private LocalDateTime createLocalDateNow(){
        return LocalDateTime.now();
    }

}