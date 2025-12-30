package com.example.demo.controller;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.command.FeedCreateCommand;
import com.example.demo.dto.command.FeedUpdateCommand;
import com.example.demo.dto.response.FeedResponse;
import com.example.demo.dto.response.SliceResponse;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.FeedLikeService;
import com.example.demo.service.FeedQueryFacade;
import com.example.demo.service.FeedReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedQueryFacade feedQueryFacade;

    @MockitoBean
    private FeedLikeService feedLikeService;

    @MockitoBean
    private FeedReportService feedReportService;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;
    private Feed testFeed;
    private FeedResponse testFeedResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userId("testuser")
                .name("테스트유저")
                .role(Role.MEMBER)
                .build();

        testFeed = Feed.builder()
                .writer(testUser)
                .activity("달리기")
                .content("오늘 달리기 완료!")
                .points(100L)
                .calories(0)
                .build();
        ReflectionTestUtils.setField(testFeed, "id", 1L);

        testFeedResponse = FeedResponse.from(testFeed);

        given(userRepository.findByUserId("testuser")).willReturn(Optional.of(testUser));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/feeds - 피드 작성 성공")
    void createFeed_Success() throws Exception {
        // given - FeedQueryFacade.createFeed(FeedCreateCommand) 사용
        given(feedQueryFacade.createFeed(any(FeedCreateCommand.class)))
                .willReturn(testFeedResponse);

        // when & then - startImage, endImage를 타입별로 전송
        mockMvc.perform(post("/api/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activity\": \"달리기\", \"content\": \"오늘 달리기 완료!\", \"startImage\": \"start.jpg\", \"endImage\": \"end.jpg\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.activity").value("달리기"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PUT /api/feeds/{feedId} - 피드 수정 성공")
    void updateFeed_Success() throws Exception {
        // given
        Feed updatedFeed = Feed.builder()
                .writer(testUser)
                .activity("수영")
                .content("수영 완료!")
                .points(0L)
                .calories(0)
                .build();
        ReflectionTestUtils.setField(updatedFeed, "id", 1L);
        FeedResponse updatedResponse = FeedResponse.from(updatedFeed);

        // FeedQueryFacade.updateFeed(FeedUpdateCommand) 사용
        given(feedQueryFacade.updateFeed(any(FeedUpdateCommand.class)))
                .willReturn(updatedResponse);

        // when & then
        mockMvc.perform(put("/api/feeds/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activity\": \"수영\", \"content\": \"수영 완료!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activity").value("수영"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("DELETE /api/feeds/{feedId} - 피드 삭제 성공")
    void deleteFeed_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/feeds/1"))
                .andExpect(status().isOk());

        verify(feedQueryFacade).deleteFeed(any(User.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/feeds/{feedId} - 피드 상세 조회 성공")
    void getFeedDetail_Success() throws Exception {
        // given
        given(feedQueryFacade.getFeedDetail(any(User.class), eq(1L))).willReturn(testFeedResponse);

        // when & then
        mockMvc.perform(get("/api/feeds/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.activity").value("달리기"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/feeds - 홈 피드 목록 조회 성공")
    void getHomeFeeds_Success() throws Exception {
        // given
        SliceResponse<FeedResponse> sliceResponse = SliceResponse.of(List.of(testFeedResponse), false, 0, 10);
        // [수정] getHomeFeeds 메서드 시그니처 변경에 맞춰 search 파라미터(null)를 추가했습니다.
        given(feedQueryFacade.getHomeFeeds(any(User.class), eq(0), eq(10), eq(null)))
                .willReturn(sliceResponse);

        // when & then
        mockMvc.perform(get("/api/feeds")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/feeds/my - 내 피드 목록 조회 성공")
    void getMyFeeds_Success() throws Exception {
        // given
        SliceResponse<FeedResponse> sliceResponse = SliceResponse.of(List.of(testFeedResponse), false, 0, 10);
        given(feedQueryFacade.getMyFeeds(any(User.class), eq(0), eq(10)))
                .willReturn(sliceResponse);

        // when & then
        mockMvc.perform(get("/api/feeds/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/feeds/{feedId}/like - 피드 좋아요 성공")
    void likeFeed_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/feeds/1/like"))
                .andExpect(status().isOk());

        verify(feedLikeService).likeFeed(any(User.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("DELETE /api/feeds/{feedId}/like - 피드 좋아요 취소 성공")
    void unlikeFeed_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/feeds/1/like"))
                .andExpect(status().isOk());

        verify(feedLikeService).unlikeFeed(any(User.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/feeds/{feedId}/report - 피드 신고 토글 성공")
    void reportFeed_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/feeds/1/report"))
                .andExpect(status().isOk());

        verify(feedReportService).toggleReport(any(User.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/feeds/can-post-today - 오늘 피드 작성 가능 여부 조회 성공")
    void canPostToday_Success() throws Exception {
        // given
        given(feedQueryFacade.canPostToday(any(User.class))).willReturn(true);

        // when & then
        mockMvc.perform(get("/api/feeds/can-post-today"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/feeds/can-post-today - 오늘 이미 피드 작성함")
    void canPostToday_AlreadyPosted() throws Exception {
        // given
        given(feedQueryFacade.canPostToday(any(User.class))).willReturn(false);

        // when & then
        mockMvc.perform(get("/api/feeds/can-post-today"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
