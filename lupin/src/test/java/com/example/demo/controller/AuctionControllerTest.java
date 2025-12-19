package com.example.demo.controller;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionItem;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.AuctionRequest;
import com.example.demo.dto.response.AuctionBidResponse;
import com.example.demo.dto.response.OngoingAuctionResponse;
import com.example.demo.dto.response.ScheduledAuctionResponse;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.service.AuctionBidFacade;
import com.example.demo.service.AuctionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class AuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuctionService auctionService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AuctionBidFacade auctionBidFacade;


    private User testUser;

    @BeforeEach
    void setUp() throws ServletException, java.io.IOException {
        // 1. JWT 필터 무력화
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        // 2. 테스트용 유저 생성
        testUser = User.builder()
                .id(1L)
                .userId("testuser")
                .name("입찰자")
                .role(Role.MEMBER)
                .build();

        // 3. Mock Repository 설정 (CurrentUserArgumentResolver에서 사용)
        given(userRepository.findByUserId(anyString())).willReturn(Optional.of(testUser));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /active - 진행 중인 경매 조회 성공")
    void getOngoingAuction_Success() throws Exception {
        AuctionItem item = AuctionItem.builder().itemName("맥북 프로").build();
        Auction auction = Auction.builder()
                .id(10L)
                .auctionItem(item)
                .currentPrice(5000L)
                .status(AuctionStatus.ACTIVE)
                .build();

        OngoingAuctionResponse response = OngoingAuctionResponse.from(auction);
        given(auctionService.getOngoingAuctionWithItem()).willReturn(response);

        mockMvc.perform(get("/api/auction/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auctionId").value(10L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /scheduled - 예정된 경매 조회 성공")
    void getScheduledAuction_Success() throws Exception {
        AuctionItem item = AuctionItem.builder().itemName("에어팟 맥스").build();
        Auction auction = Auction.builder()
                .id(11L)
                .auctionItem(item)
                .status(AuctionStatus.SCHEDULED)
                .startTime(LocalDateTime.now().plusHours(2))
                .build();

        ScheduledAuctionResponse response = ScheduledAuctionResponse.from(auction);
        given(auctionService.scheduledAuctionWithItem()).willReturn(List.of(response));

        mockMvc.perform(get("/api/auction/scheduled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].auctionId").value(11L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /{id}/bid - 경매 입찰 성공")
    void placeBid_Success() throws Exception {
        Long auctionId = 10L;
        Long bidAmount = 6000L;
        AuctionRequest request = new AuctionRequest(bidAmount);

        // API 호출
        mockMvc.perform(post("/api/auction/{auctionId}/bid", auctionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        // Service 호출 검증
        verify(auctionBidFacade).bid(
                eq(auctionId),
                eq(testUser.getId()), // 1L
                eq(bidAmount),
                any(LocalDateTime.class)
        );
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /active/history")
    void getBidHistory_Success() throws Exception {
        // Given
        // 1. 가짜 응답 데이터 생성
        AuctionBidResponse bid1 = AuctionBidResponse.builder()
                .id(1L)
                .userId(10L)
                .userName("입찰왕")
                .bidAmount(50000L)
                .bidTime(LocalDateTime.now().minusMinutes(1))
                .status("ACTIVE")
                .build();

        AuctionBidResponse bid2 = AuctionBidResponse.builder()
                .id(2L)
                .userId(11L)
                .userName("도전자")
                .bidAmount(45000L)
                .bidTime(LocalDateTime.now().minusMinutes(5))
                .status("ACTIVE")
                .build();

        List<AuctionBidResponse> responseList = List.of(bid1, bid2);

        // 2. 서비스 메소드 호출 시 가짜 데이터 반환하도록 설정 (Stubbing)
        given(auctionService.getAuctionStatus()).willReturn(responseList);

        // When & Then
        mockMvc.perform(get("/api/auction/active/history") // 3. 요청 전송
                        .with(csrf()) // Spring Security 사용 시 필요할 수 있음
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 로그 출력
                .andExpect(status().isOk()) // 4. 상태 코드 200 검증
                .andExpect(jsonPath("$.size()").value(2)) // 리스트 크기 검증
                .andExpect(jsonPath("$[0].userName").value("입찰왕")) // 첫 번째 데이터 검증
                .andExpect(jsonPath("$[0].bidAmount").value(50000))
                .andExpect(jsonPath("$[1].userName").value("도전자"));

    }
    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /winners/monthly - 이달의 낙찰자 조회 성공")
    void getMonthlyWinners_Success() throws Exception {
        // Given
        // 1. 가짜 응답 데이터(AuctionResponse) 생성
        // (AuctionResponse DTO에 Builder가 있다고 가정하고 작성했습니다)
        // 만약 import가 안 되면 import com.example.demo.dto.response.AuctionResponse; 추가 필요
        com.example.demo.dto.response.AuctionResponse winner1 = com.example.demo.dto.response.AuctionResponse.builder()
                .auctionId(100L)
                .winnerName("낙찰왕") // 우리가 추가했던 필드
                .currentPrice(15000L)
                .status(AuctionStatus.ENDED)
                .build();

        com.example.demo.dto.response.AuctionResponse winner2 = com.example.demo.dto.response.AuctionResponse.builder()
                .auctionId(101L)
                .winnerName("행운아")
                .currentPrice(30000L)
                .status(AuctionStatus.ENDED)
                .build();

        List<com.example.demo.dto.response.AuctionResponse> responseList = List.of(winner1, winner2);

        // 2. 서비스 메소드 호출 시 가짜 데이터 반환하도록 설정 (Stubbing)
        given(auctionService.getMonthlyWinners()).willReturn(responseList);

        // When & Then
        mockMvc.perform(get("/api/auction/winners/monthly") // 3. 요청 전송
                        .with(csrf()) // 보안 설정 통과용
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 로그 출력
                .andExpect(status().isOk()) // 4. 상태 코드 200 검증
                .andExpect(jsonPath("$.size()").value(2)) // 리스트 크기 검증
                .andExpect(jsonPath("$[0].winnerName").value("낙찰왕")) // 첫 번째 데이터 검증
                .andExpect(jsonPath("$[0].currentPrice").value(15000))
                .andExpect(jsonPath("$[1].winnerName").value("행운아")); // 두 번째 데이터 검증
    }

}