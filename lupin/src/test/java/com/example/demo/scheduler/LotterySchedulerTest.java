package com.example.demo.scheduler;

import com.example.demo.domain.entity.LotteryTicket;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.LotteryTicketRepository;
import com.example.demo.repository.PrizeClaimRepository;
import com.example.demo.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LotteryScheduler 테스트")
class LotterySchedulerTest {

    @InjectMocks
    private LotteryScheduler lotteryScheduler;

    @Mock
    private LotteryTicketRepository lotteryTicketRepository;

    @Mock
    private PrizeClaimRepository prizeClaimRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    @DisplayName("일일 추첨 성공 - 충분한 참가자")
    void runDailyLottery_Success() {
        // given
        User user1 = User.builder().id(1L).userId("user01").realName("유저1").build();
        User user2 = User.builder().id(2L).userId("user02").realName("유저2").build();
        User user3 = User.builder().id(3L).userId("user03").realName("유저3").build();
        User user4 = User.builder().id(4L).userId("user04").realName("유저4").build();

        LotteryTicket ticket1 = LotteryTicket.builder().id(1L).user(user1).build();
        LotteryTicket ticket2 = LotteryTicket.builder().id(2L).user(user2).build();
        LotteryTicket ticket3 = LotteryTicket.builder().id(3L).user(user3).build();
        LotteryTicket ticket4 = LotteryTicket.builder().id(4L).user(user4).build();

        given(lotteryTicketRepository.findAll())
                .willReturn(Arrays.asList(ticket1, ticket2, ticket3, ticket4));

        // when
        lotteryScheduler.runDailyLottery();

        // then
        then(prizeClaimRepository).should(times(3)).save(any());
        then(notificationService).should(atLeast(3)).createSystemNotification(anyLong(), anyString());
        then(lotteryTicketRepository).should().deleteAll();
    }

    @Test
    @DisplayName("추첨권 없음")
    void runDailyLottery_NoTickets() {
        // given
        given(lotteryTicketRepository.findAll()).willReturn(Collections.emptyList());

        // when
        lotteryScheduler.runDailyLottery();

        // then
        then(prizeClaimRepository).should(never()).save(any());
        then(lotteryTicketRepository).should(never()).deleteAll();
    }

    @Test
    @DisplayName("참가자 1명만 있는 경우")
    void runDailyLottery_OnlyOneParticipant() {
        // given
        User user = User.builder().id(1L).userId("user01").realName("유저1").build();
        LotteryTicket ticket = LotteryTicket.builder().id(1L).user(user).build();

        given(lotteryTicketRepository.findAll()).willReturn(Arrays.asList(ticket));

        // when
        lotteryScheduler.runDailyLottery();

        // then
        then(prizeClaimRepository).should(times(1)).save(any());
        then(lotteryTicketRepository).should().deleteAll();
    }
}
