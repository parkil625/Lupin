package com.example.demo.controller;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionItem;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.dto.response.OngoingAuctionResponse;
import com.example.demo.service.AuctionService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class) // FeedControllerTest와 동일한 설정 적용
class AuctionContollerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // JSON 변환용

    @MockBean
    private AuctionService auctionService; // 실제 DB 대신 가짜 서비스 사용

    @Test
    @WithMockUser(username = "testuser") // 가짜 인증 유저 주입
    @DisplayName("GET /api/auction/active - 진행 중인 경매 조회 성공")
    void getOngoingAuction_Success() throws Exception {
        // given: 서비스가 반환할 가짜 데이터 설정
        AuctionItem item = AuctionItem.builder()
                .itemName("아이패드 프로")
                .description("미개봉 새상품")
                .build();

        Auction auction = Auction.builder()
                .id(1L)
                .auctionItem(item)
                .currentPrice(1000L)
                .status(AuctionStatus.ACTIVE)
                .startTime(LocalDateTime.now())
                .build();

        OngoingAuctionResponse response = OngoingAuctionResponse.from(auction);

        // 서비스가 호출되면 위 response를 리턴하도록 설정 (Mocking)
        given(auctionService.getOngoingAuctionWithItem()).willReturn(response);

        // when & then: API 호출 및 검증
        mockMvc.perform(get("/api/auction/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auctionId").value(1L))
                .andExpect(jsonPath("$.currentPrice").value(1000))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.item.itemName").value("아이패드 프로"));
    }


}