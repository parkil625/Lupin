package com.example.demo.controller;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionItem;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.AuctionRequest;
import com.example.demo.dto.response.OngoingAuctionResponse;
import com.example.demo.dto.response.ScheduledAuctionResponse;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtAuthenticationFilter;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils; // 필수 Import
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
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

    // 테스트 대상 컨트롤러를 직접 주입받음 (Reflection으로 필드 설정을 위해)
    @Autowired
    private AuctionController auctionController;

    @MockBean
    private AuctionService auctionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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

        // 3. Mock Repository 설정
        given(userRepository.findByUserId(anyString())).willReturn(Optional.of(testUser));

        // 4. [핵심] ReflectionTestUtils로 부모 클래스(BaseController)의 필드 강제 주입
        // 이 부분이 없으면 BaseController의 userRepository가 null이거나 실제 빈이 들어가서 Mock이 동작 안 할 수 있음
        ReflectionTestUtils.setField(auctionController, "userRepository", userRepository);
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
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andDo(print())
                .andExpect(status().isOk());

        // Service 호출 검증
        verify(auctionService).placeBid(
                eq(auctionId),
                eq(testUser.getId()), // 1L
                eq(bidAmount),
                any(LocalDateTime.class)
        );
    }


}