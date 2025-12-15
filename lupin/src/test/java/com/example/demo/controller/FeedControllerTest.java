package com.example.demo.controller;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.config.TestSecurityConfig;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.FeedRequest;
import com.example.demo.dto.response.FeedResponse;
import com.example.demo.service.FeedFacade;
import com.example.demo.service.FeedLikeService;
import com.example.demo.service.ReportService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestRedisConfig.class}) // ★★★ 친구들 다 모여!
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedFacade feedFacade;

    @MockitoBean
    private FeedLikeService feedLikeService;

    @MockitoBean
    private ReportService reportService;

    private User user;
    private Feed feed;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .userId("testuser")
                .name("testUser")
                .role(Role.MEMBER)
                .build();

        feed = Feed.builder()
                .writer(user)
                .content("content")
                .activity("running")
                .points(10)
                .calories(100)
                .build();
    }

    @Test
    @WithUserDetails(value = "testuser", userDetailsServiceBeanName = "userDetailsService")
    @DisplayName("피드 생성 성공")
    void createFeedTest() throws Exception {
        FeedRequest request = new FeedRequest("running", "content", "start.jpg", "end.jpg", List.of(), List.of());
        given(feedFacade.createFeed(any())).willReturn(FeedResponse.from(feed));

        mockMvc.perform(post("/api/feeds")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "testuser", userDetailsServiceBeanName = "userDetailsService")
    @DisplayName("피드 삭제 성공")
    void deleteFeedTest() throws Exception {
        mockMvc.perform(delete("/api/feeds/1")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(feedFacade).deleteFeed(any(User.class), eq(1L));
    }
}