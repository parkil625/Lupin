package com.example.demo.controller;

import com.example.demo.dto.request.FeedCreateRequest;
import com.example.demo.dto.response.FeedDetailResponse;
import com.example.demo.service.FeedCommandService;
import com.example.demo.service.FeedQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@DisplayName("FeedController 테스트")
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeedCommandService feedCommandService;

    @MockBean
    private FeedQueryService feedQueryService;

    @Test
    @DisplayName("피드 생성 성공")
    void createFeed_Success() throws Exception {
        // given
        FeedCreateRequest request = FeedCreateRequest.builder()
                .activityType("러닝")
                .content("오늘 운동")
                .images(Arrays.asList("img1.jpg", "img2.jpg"))
                .startedAt(LocalDateTime.now().minusHours(1))
                .endedAt(LocalDateTime.now())
                .build();

        given(feedCommandService.createFeed(eq(1L), any(FeedCreateRequest.class))).willReturn(10L);

        // when & then
        mockMvc.perform(post("/api/feeds")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feedId").value(10));
    }

    @Test
    @DisplayName("피드 상세 조회 성공")
    void getFeedDetail_Success() throws Exception {
        // given
        FeedDetailResponse response = FeedDetailResponse.builder()
                .id(1L)
                .content("테스트 피드")
                .activityType("러닝")
                .build();

        given(feedQueryService.getFeedDetail(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/feeds/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("테스트 피드"));
    }

    @Test
    @DisplayName("피드 좋아요 성공")
    void likeFeed_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/feeds/1/like")
                        .param("userId", "1"))
                .andExpect(status().isOk());

        then(feedCommandService).should().likeFeed(1L, 1L);
    }

    @Test
    @DisplayName("피드 삭제 성공")
    void deleteFeed_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/feeds/1")
                        .param("userId", "1"))
                .andExpect(status().isNoContent());

        then(feedCommandService).should().deleteFeed(1L, 1L);
    }

    @Test
    @DisplayName("오늘 피드 작성 가능 여부 확인")
    void canPostToday_Success() throws Exception {
        // given
        given(feedQueryService.canPostToday(1L)).willReturn(true);

        // when & then
        mockMvc.perform(get("/api/feeds/can-post")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
