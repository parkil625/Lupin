package com.example.demo.controller;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.config.TestSecurityConfig;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.AuctionRequest;
import com.example.demo.dto.response.AuctionItemResponse;
import com.example.demo.dto.response.OngoingAuctionResponse;
import com.example.demo.service.AuctionService;
import com.example.demo.service.AuctionSseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuctionController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestRedisConfig.class}) // ★★★ 친구들 다 모여!
class AuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuctionService auctionService;

    @MockitoBean
    private AuctionSseService auctionSseService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .userId("testuser")
                .name("Test User")
                .role(Role.MEMBER)
                .build();
    }

    @Test
    @WithUserDetails(value = "testuser", userDetailsServiceBeanName = "userDetailsService")
    @DisplayName("진행 중인 경매 조회")
    void getOngoingAuctionTest() throws Exception {
        AuctionItemResponse itemResponse = new AuctionItemResponse(1L, "Test Item", "Description", "imageUrl");
        OngoingAuctionResponse response = new OngoingAuctionResponse(
                1L, "ACTIVE", LocalDateTime.now(), LocalDateTime.now().plusHours(1), 1000L,
                false, null, 30, 5, itemResponse
        );
        given(auctionService.getOngoingAuctionWithItem()).willReturn(response);

        mockMvc.perform(get("/api/auction/active"))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "testuser", userDetailsServiceBeanName = "userDetailsService")
    @DisplayName("경매 입찰")
    void placeBidTest() throws Exception {
        AuctionRequest request = new AuctionRequest(1500L);

        mockMvc.perform(post("/api/auction/1/bid")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}