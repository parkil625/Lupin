package com.example.demo.controller;

import com.example.demo.domain.entity.LotteryTicket;
import com.example.demo.domain.entity.User;
import com.example.demo.service.LotteryService;
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

import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestRedisConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@DisplayName("LotteryController 테스트")
class LotteryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LotteryService lotteryService;

    @Test
    @DisplayName("추첨권 발급 성공")
    void issueTicket_Success() throws Exception {
        // given
        User user = User.builder().id(1L).userId("user01").build();
        LotteryTicket ticket = LotteryTicket.builder()
                .id(1L)
                .user(user)
                .build();

        given(lotteryService.issueTicket(1L)).willReturn(ticket);

        // when & then
        mockMvc.perform(post("/api/lottery/issue")
                        .param("userId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("사용자 추첨권 조회 성공")
    void getTickets_Success() throws Exception {
        // given
        User user = User.builder().id(1L).userId("user01").build();
        LotteryTicket ticket1 = LotteryTicket.builder().id(1L).user(user).build();
        LotteryTicket ticket2 = LotteryTicket.builder().id(2L).user(user).build();

        List<LotteryTicket> tickets = Arrays.asList(ticket1, ticket2);

        given(lotteryService.getTickets(1L)).willReturn(tickets);

        // when & then
        mockMvc.perform(get("/api/lottery/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("추첨권 개수 조회 성공")
    void countTickets_Success() throws Exception {
        // given
        given(lotteryService.countTickets(1L)).willReturn(5L);

        // when & then
        mockMvc.perform(get("/api/lottery/users/1/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }
}
