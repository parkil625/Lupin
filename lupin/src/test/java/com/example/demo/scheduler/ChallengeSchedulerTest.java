package com.example.demo.scheduler;

import com.example.demo.domain.entity.Challenge;
import com.example.demo.repository.ChallengeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengeScheduler 테스트")
class ChallengeSchedulerTest {

    @InjectMocks
    private ChallengeScheduler challengeScheduler;

    @Mock
    private ChallengeRepository challengeRepository;

    @Test
    @DisplayName("챌린지 종료 성공")
    void checkChallengeClosed_Success() {
        // given
        Challenge challenge = mock(Challenge.class);
        given(challengeRepository.findActiveChallengesToClose(any(LocalDateTime.class)))
                .willReturn(Arrays.asList(challenge));

        // when
        challengeScheduler.checkChallengeClosed();

        // then
        then(challenge).should().close(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("종료할 챌린지 없음")
    void checkChallengeClosed_NoChallenge() {
        // given
        given(challengeRepository.findActiveChallengesToClose(any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        challengeScheduler.checkChallengeClosed();

        // then
        then(challengeRepository).should().findActiveChallengesToClose(any(LocalDateTime.class));
    }
}
