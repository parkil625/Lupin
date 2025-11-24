package com.example.demo.service;

import com.example.demo.domain.entity.LotteryTicket;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.LotteryTicketRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LotteryService 테스트")
class LotteryServiceTest {

    @InjectMocks
    private LotteryService lotteryService;

    @Mock
    private LotteryTicketRepository lotteryTicketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DistributedLockService lockService;
    @Mock
    private RedisLuaService redisLuaService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .userId("user01")
                .realName("테스트유저")
                .role(Role.MEMBER)
                .currentPoints(100L)
                .build();

        // lockService mock이 supplier를 바로 실행하도록 설정
        lenient().when(lockService.withTicketIssueLock(anyLong(), any()))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(1);
                    return supplier.get();
                });

        // redisLuaService mock이 성공을 반환하도록 설정
        lenient().when(redisLuaService.issueTicketAtomic(anyLong())).thenReturn(true);
    }

    @Nested
    @DisplayName("추첨권 수 조회")
    class CountTickets {

        @Test
        @DisplayName("추첨권 수 조회 성공")
        void countTickets_Success() {
            // given
            given(lotteryTicketRepository.countByUserId(1L)).willReturn(5L);

            // when
            Long result = lotteryService.countTickets(1L);

            // then
            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("추첨권이 없는 경우")
        void countTickets_Zero() {
            // given
            given(lotteryTicketRepository.countByUserId(1L)).willReturn(0L);

            // when
            Long result = lotteryService.countTickets(1L);

            // then
            assertThat(result).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("추첨권 발급")
    class IssueTicket {

        @Test
        @DisplayName("추첨권 발급 성공")
        void issueTicket_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(lotteryTicketRepository.save(any(LotteryTicket.class))).willAnswer(invocation -> {
                LotteryTicket ticket = invocation.getArgument(0);
                ReflectionTestUtils.setField(ticket, "id", 10L);
                return ticket;
            });

            // when
            LotteryTicket result = lotteryService.issueTicket(1L);

            // then
            assertThat(result).isNotNull();
            then(lotteryTicketRepository).should().save(any(LotteryTicket.class));
        }

        @Test
        @DisplayName("존재하지 않는 유저 추첨권 발급 실패")
        void issueTicket_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> lotteryService.issueTicket(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("추첨권 목록 조회")
    class GetTickets {

        @Test
        @DisplayName("추첨권 목록 조회 성공")
        void getTickets_Success() {
            // given
            LotteryTicket ticket1 = LotteryTicket.builder().user(user).build();
            LotteryTicket ticket2 = LotteryTicket.builder().user(user).build();
            ReflectionTestUtils.setField(ticket1, "id", 1L);
            ReflectionTestUtils.setField(ticket2, "id", 2L);

            given(lotteryTicketRepository.findByUserId(1L))
                    .willReturn(Arrays.asList(ticket1, ticket2));

            // when
            List<LotteryTicket> result = lotteryService.getTickets(1L);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("추첨권이 없는 경우 빈 목록 반환")
        void getTickets_Empty() {
            // given
            given(lotteryTicketRepository.findByUserId(1L)).willReturn(Arrays.asList());

            // when
            List<LotteryTicket> result = lotteryService.getTickets(1L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("동시성 테스트")
    class ConcurrencyTest {

        @Test
        @DisplayName("동시에 여러 추첨권 발급 요청 처리")
        void issueTicket_Concurrent_Success() throws InterruptedException {
            // given
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger ticketId = new AtomicInteger(0);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(lotteryTicketRepository.save(any(LotteryTicket.class))).willAnswer(invocation -> {
                LotteryTicket ticket = invocation.getArgument(0);
                ReflectionTestUtils.setField(ticket, "id", (long) ticketId.incrementAndGet());
                return ticket;
            });

            // when
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        lotteryService.issueTicket(1L);
                        successCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // then
            assertThat(successCount.get()).isEqualTo(threadCount);
            then(lotteryTicketRepository).should(times(threadCount)).save(any(LotteryTicket.class));
        }

        @Test
        @DisplayName("서로 다른 사용자가 동시에 추첨권 발급")
        void issueTicket_DifferentUsers_Concurrent() throws InterruptedException {
            // given
            User user2 = User.builder()
                    .id(2L)
                    .userId("user02")
                    .realName("테스트유저2")
                    .role(Role.MEMBER)
                    .currentPoints(100L)
                    .build();

            int threadCount = 2;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger ticketId = new AtomicInteger(0);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.findById(2L)).willReturn(Optional.of(user2));
            given(lotteryTicketRepository.save(any(LotteryTicket.class))).willAnswer(invocation -> {
                LotteryTicket ticket = invocation.getArgument(0);
                ReflectionTestUtils.setField(ticket, "id", (long) ticketId.incrementAndGet());
                return ticket;
            });

            // when
            executor.submit(() -> {
                try {
                    lotteryService.issueTicket(1L);
                } finally {
                    latch.countDown();
                }
            });
            executor.submit(() -> {
                try {
                    lotteryService.issueTicket(2L);
                } finally {
                    latch.countDown();
                }
            });

            latch.await();
            executor.shutdown();

            // then
            then(lotteryTicketRepository).should(times(2)).save(any(LotteryTicket.class));
        }
    }
}
