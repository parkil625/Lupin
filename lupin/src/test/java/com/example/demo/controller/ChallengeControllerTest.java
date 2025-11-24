package com.example.demo.controller;

import com.example.demo.domain.entity.Challenge;
import com.example.demo.domain.entity.ChallengeEntry;
import com.example.demo.service.ChallengeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.domain.enums.ChallengeStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestRedisConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@DisplayName("ChallengeController 테스트")
class ChallengeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChallengeService challengeService;

    @Test
    @DisplayName("활성화된 챌린지 목록 조회")
    void getActiveChallenges_Success() throws Exception {
        // given
        Challenge challenge = Challenge.builder()
                .title("테스트 챌린지")
                .openTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().plusDays(30))
                .status(ChallengeStatus.OPEN)
                .build();

        when(challengeService.getActiveChallenges()).thenReturn(List.of(challenge));

        // when & then
        mockMvc.perform(get("/api/challenges/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("테스트 챌린지"));

        verify(challengeService).getActiveChallenges();
    }

    @Test
    @DisplayName("활성화된 챌린지가 없을 때 빈 목록 반환")
    void getActiveChallenges_Empty() throws Exception {
        // given
        when(challengeService.getActiveChallenges()).thenReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/api/challenges/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("챌린지 상세 조회")
    void getChallengeDetail_Success() throws Exception {
        // given
        Challenge challenge = Challenge.builder()
                .title("상세 챌린지")
                .openTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(14))
                .status(ChallengeStatus.OPEN)
                .build();

        when(challengeService.getChallengeDetail(1L)).thenReturn(challenge);

        // when & then
        mockMvc.perform(get("/api/challenges/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("상세 챌린지"));

        verify(challengeService).getChallengeDetail(1L);
    }

    @Test
    @DisplayName("챌린지 참가")
    void joinChallenge_Success() throws Exception {
        // given
        doNothing().when(challengeService).joinChallenge(1L, 1L);

        // when & then
        mockMvc.perform(post("/api/challenges/1/join")
                        .param("userId", "1"))
                .andExpect(status().isOk());

        verify(challengeService).joinChallenge(1L, 1L);
    }

    @Test
    @DisplayName("챌린지 참가 여부 확인 - 참가함")
    void isUserJoined_True() throws Exception {
        // given
        when(challengeService.isUserJoined(1L, 1L)).thenReturn(true);

        // when & then
        mockMvc.perform(get("/api/challenges/1/joined")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(challengeService).isUserJoined(1L, 1L);
    }

    @Test
    @DisplayName("챌린지 참가 여부 확인 - 참가 안함")
    void isUserJoined_False() throws Exception {
        // given
        when(challengeService.isUserJoined(1L, 1L)).thenReturn(false);

        // when & then
        mockMvc.perform(get("/api/challenges/1/joined")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("챌린지 참가자 목록 조회")
    void getChallengeEntries_Success() throws Exception {
        // given
        when(challengeService.getChallengeEntries(1L)).thenReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/api/challenges/1/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(challengeService).getChallengeEntries(1L);
    }

    @Test
    @DisplayName("챌린지 시작")
    void startChallenge_Success() throws Exception {
        // given
        doNothing().when(challengeService).startChallenge(1L);

        // when & then
        mockMvc.perform(post("/api/challenges/1/start"))
                .andExpect(status().isOk());

        verify(challengeService).startChallenge(1L);
    }

    @Test
    @DisplayName("챌린지 종료")
    void closeChallenge_Success() throws Exception {
        // given
        doNothing().when(challengeService).closeChallenge(1L);

        // when & then
        mockMvc.perform(post("/api/challenges/1/close"))
                .andExpect(status().isOk());

        verify(challengeService).closeChallenge(1L);
    }

    @Test
    @DisplayName("여러 챌린지 조회")
    void getActiveChallenges_Multiple() throws Exception {
        // given
        Challenge challenge1 = Challenge.builder()
                .title("챌린지1")
                .openTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(7))
                .status(ChallengeStatus.OPEN)
                .build();

        Challenge challenge2 = Challenge.builder()
                .title("챌린지2")
                .openTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(14))
                .status(ChallengeStatus.OPEN)
                .build();

        when(challengeService.getActiveChallenges()).thenReturn(Arrays.asList(challenge1, challenge2));

        // when & then
        mockMvc.perform(get("/api/challenges/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
