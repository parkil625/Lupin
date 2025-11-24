package com.example.demo.service;

import com.example.demo.domain.entity.Challenge;
import com.example.demo.domain.entity.ChallengeEntry;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.ChallengeEntryRepository;
import com.example.demo.repository.ChallengeRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengeService 테스트")
class ChallengeServiceTest {

    @InjectMocks
    private ChallengeService challengeService;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ChallengeEntryRepository challengeEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DistributedLockService lockService;

    @BeforeEach
    void setUp() {
        // lockService mock이 supplier를 바로 실행하도록 설정
        given(lockService.withChallengeJoinLock(anyLong(), anyLong(), any()))
                .willAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
    }

    @Test
    @DisplayName("활성화된 챌린지 목록 조회")
    void getActiveChallenges_Success() {
        // given
        List<Challenge> challenges = Arrays.asList(
                Challenge.builder().id(1L).title("챌린지1").build(),
                Challenge.builder().id(2L).title("챌린지2").build()
        );
        given(challengeRepository.findActiveChallenges(any(LocalDateTime.class)))
                .willReturn(challenges);

        // when
        List<Challenge> result = challengeService.getActiveChallenges();

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("챌린지 상세 조회 성공")
    void getChallengeDetail_Success() {
        // given
        Challenge challenge = Challenge.builder().id(1L).title("테스트 챌린지").build();
        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));

        // when
        Challenge result = challengeService.getChallengeDetail(1L);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("테스트 챌린지");
    }

    @Test
    @DisplayName("챌린지 상세 조회 실패 - 챌린지 없음")
    void getChallengeDetail_NotFound() {
        // given
        given(challengeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> challengeService.getChallengeDetail(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("챌린지 참가 성공")
    void joinChallenge_Success() {
        // given
        Challenge challenge = mock(Challenge.class);
        User user = User.builder().id(1L).userId("user01").build();

        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(challengeEntryRepository.existsByChallengeIdAndUserId(1L, 1L)).willReturn(false);
        given(challenge.canJoin(any(LocalDateTime.class))).willReturn(true);

        // when
        challengeService.joinChallenge(1L, 1L);

        // then
        then(challengeEntryRepository).should().save(any(ChallengeEntry.class));
    }

    @Test
    @DisplayName("챌린지 참가 실패 - 이미 참가")
    void joinChallenge_AlreadyJoined() {
        // given
        Challenge challenge = mock(Challenge.class);
        User user = User.builder().id(1L).userId("user01").build();

        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(challengeEntryRepository.existsByChallengeIdAndUserId(1L, 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> challengeService.joinChallenge(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("챌린지 참가 실패 - 참가 불가 상태")
    void joinChallenge_NotActive() {
        // given
        Challenge challenge = mock(Challenge.class);
        User user = User.builder().id(1L).userId("user01").build();

        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(challengeEntryRepository.existsByChallengeIdAndUserId(1L, 1L)).willReturn(false);
        given(challenge.canJoin(any(LocalDateTime.class))).willReturn(false);

        // when & then
        assertThatThrownBy(() -> challengeService.joinChallenge(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("챌린지 참가 여부 확인")
    void isUserJoined_Success() {
        // given
        given(challengeEntryRepository.existsByChallengeIdAndUserId(1L, 1L)).willReturn(true);

        // when
        boolean result = challengeService.isUserJoined(1L, 1L);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("챌린지 참가자 목록 조회")
    void getChallengeEntries_Success() {
        // given
        List<ChallengeEntry> entries = Arrays.asList(
                mock(ChallengeEntry.class),
                mock(ChallengeEntry.class)
        );
        given(challengeEntryRepository.findByChallengeIdOrderByJoinedAtAsc(1L)).willReturn(entries);

        // when
        List<ChallengeEntry> result = challengeService.getChallengeEntries(1L);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("챌린지 시작 성공")
    void startChallenge_Success() {
        // given
        Challenge challenge = mock(Challenge.class);
        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));

        // when
        challengeService.startChallenge(1L);

        // then
        then(challenge).should().open(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("챌린지 시작 실패 - 활성화 불가")
    void startChallenge_NotActive() {
        // given
        Challenge challenge = mock(Challenge.class);
        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        willThrow(new IllegalStateException("활성화 불가")).given(challenge).open(any(LocalDateTime.class));

        // when & then
        assertThatThrownBy(() -> challengeService.startChallenge(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("챌린지 종료 성공")
    void closeChallenge_Success() {
        // given
        Challenge challenge = mock(Challenge.class);
        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));

        // when
        challengeService.closeChallenge(1L);

        // then
        then(challenge).should().close(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("동시에 여러 사용자가 챌린지 참가 시도")
    void joinChallenge_Concurrent_MultipleUsers() throws InterruptedException {
        // given
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        Challenge challenge = mock(Challenge.class);
        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        given(challenge.canJoin(any(LocalDateTime.class))).willReturn(true);

        // 각 사용자별로 설정
        for (int i = 1; i <= threadCount; i++) {
            long userId = i;
            User user = User.builder().id(userId).userId("user" + userId).build();
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(challengeEntryRepository.existsByChallengeIdAndUserId(1L, userId)).willReturn(false);
        }

        // when
        for (int i = 1; i <= threadCount; i++) {
            final long userId = i;
            executor.submit(() -> {
                try {
                    challengeService.joinChallenge(1L, userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 예외 발생 시 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(threadCount);
        then(challengeEntryRepository).should(times(threadCount)).save(any(ChallengeEntry.class));
    }

    @Test
    @DisplayName("같은 사용자가 동시에 중복 참가 시도 - 첫 번째만 성공")
    void joinChallenge_Concurrent_DuplicateUser() throws InterruptedException {
        // given
        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger callCount = new AtomicInteger(0);

        Challenge challenge = mock(Challenge.class);
        User user = User.builder().id(1L).userId("user01").build();

        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(challenge.canJoin(any(LocalDateTime.class))).willReturn(true);

        // 첫 번째 호출은 false(참가 안 함), 이후는 true(이미 참가)
        given(challengeEntryRepository.existsByChallengeIdAndUserId(1L, 1L))
                .willAnswer(invocation -> callCount.getAndIncrement() > 0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    challengeService.joinChallenge(1L, 1L);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);
    }

    @Test
    @DisplayName("챌린지 참가 실패 - 사용자 없음")
    void joinChallenge_UserNotFound() {
        // given
        Challenge challenge = mock(Challenge.class);
        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> challengeService.joinChallenge(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("챌린지 참가 실패 - 챌린지 없음")
    void joinChallenge_ChallengeNotFound() {
        // given
        given(challengeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> challengeService.joinChallenge(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("챌린지 참가 여부 확인 - 참가 안 함")
    void isUserJoined_NotJoined() {
        // given
        given(challengeEntryRepository.existsByChallengeIdAndUserId(1L, 1L)).willReturn(false);

        // when
        boolean result = challengeService.isUserJoined(1L, 1L);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("챌린지 참가자 목록 조회 - 빈 목록")
    void getChallengeEntries_Empty() {
        // given
        given(challengeEntryRepository.findByChallengeIdOrderByJoinedAtAsc(1L)).willReturn(Arrays.asList());

        // when
        List<ChallengeEntry> result = challengeService.getChallengeEntries(1L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("챌린지 시작 실패 - 챌린지 없음")
    void startChallenge_NotFound() {
        // given
        given(challengeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> challengeService.startChallenge(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("챌린지 종료 실패 - 챌린지 없음")
    void closeChallenge_NotFound() {
        // given
        given(challengeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> challengeService.closeChallenge(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("챌린지 종료 실패 - 종료 불가 상태")
    void closeChallenge_NotActive() {
        // given
        Challenge challenge = mock(Challenge.class);
        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        willThrow(new IllegalStateException("종료 불가")).given(challenge).close(any(LocalDateTime.class));

        // when & then
        assertThatThrownBy(() -> challengeService.closeChallenge(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("활성화된 챌린지 목록 조회 - 빈 목록")
    void getActiveChallenges_Empty() {
        // given
        given(challengeRepository.findActiveChallenges(any(LocalDateTime.class)))
                .willReturn(Arrays.asList());

        // when
        List<Challenge> result = challengeService.getActiveChallenges();

        // then
        assertThat(result).isEmpty();
    }
}
