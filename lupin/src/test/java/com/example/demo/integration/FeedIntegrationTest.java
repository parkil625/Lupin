package com.example.demo.integration;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.FeedRequest;
import com.example.demo.repository.FeedImageRepository;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ImageMetadataService;
import com.example.demo.service.WorkoutScoreService;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@Transactional
class FeedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private FeedLikeRepository feedLikeRepository;

    @Autowired
    private FeedImageRepository feedImageRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private ImageMetadataService imageMetadataService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private WorkoutScoreService workoutScoreService;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        // Mock 설정: EXIF 시간 반환
        java.time.LocalDateTime startTime = java.time.LocalDateTime.now().minusHours(1);
        java.time.LocalDateTime endTime = java.time.LocalDateTime.now();

        org.mockito.Mockito.when(imageMetadataService.extractPhotoDateTime(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.Optional.of(startTime))
                .thenReturn(java.util.Optional.of(endTime));

        org.mockito.Mockito.when(workoutScoreService.calculateScore(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class)))
                .thenReturn(10);

        org.mockito.Mockito.when(workoutScoreService.calculateCalories(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class)))
                .thenReturn(100);

        testUser = userRepository.save(User.builder()
                .userId("testuser")
                .password("password")
                .name("테스트유저")
                .role(Role.MEMBER)
                .build());

        otherUser = userRepository.save(User.builder()
                .userId("otheruser")
                .password("password")
                .name("다른유저")
                .role(Role.MEMBER)
                .build());
    }

    @Test
    @DisplayName("피드 생성 → 조회 통합 테스트")
    @WithMockUser(username = "testuser")
    void createAndGetFeed() throws Exception {
        // 1. 피드 생성 (startImage, endImage 타입별로 지정)
        FeedRequest request = FeedRequest.builder()
                .activity("RUNNING")
                .content("오늘 5km 달리기 완료!")
                .startImage("start.jpg")
                .endImage("end.jpg")
                .build();

        String createResponse = mockMvc.perform(post("/api/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("오늘 5km 달리기 완료!"))
                .andReturn().getResponse().getContentAsString();

        // 2. DB에 실제로 저장되었는지 확인
        assertThat(feedRepository.count()).isEqualTo(1);

        // 3. 피드 조회
        Feed savedFeed = feedRepository.findAll().get(0);
        mockMvc.perform(get("/api/feeds/" + savedFeed.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("오늘 5km 달리기 완료!"));
    }

    @Test
    @DisplayName("피드 좋아요 → 좋아요 취소 통합 테스트")
    @WithMockUser(username = "testuser")
    void likeAndUnlikeFeed() throws Exception {
        // given: 피드 생성
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("테스트 피드")
                .build());

        // 1. 좋아요
        mockMvc.perform(post("/api/feeds/" + feed.getId() + "/like"))
                .andExpect(status().isOk());

        // 2. 좋아요 저장 확인
        assertThat(feedLikeRepository.existsByUserAndFeed(testUser, feed)).isTrue();

        // 3. 좋아요 취소
        mockMvc.perform(delete("/api/feeds/" + feed.getId() + "/like"))
                .andExpect(status().isOk());

        // 4. 좋아요 삭제 확인
        assertThat(feedLikeRepository.existsByUserAndFeed(testUser, feed)).isFalse();
    }

    @Test
    @DisplayName("피드 수정 → 삭제 통합 테스트")
    @WithMockUser(username = "testuser")
    void updateAndDeleteFeed() throws Exception {
        // given: 피드 생성
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("원본 내용")
                .build());

        // 1. 피드 수정
        FeedRequest updateRequest = FeedRequest.builder()
                .activity("WALKING")
                .content("수정된 내용")
                .build();

        mockMvc.perform(put("/api/feeds/" + feed.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정된 내용"));

        // 2. 수정 확인
        Feed updatedFeed = feedRepository.findById(feed.getId()).orElseThrow();
        assertThat(updatedFeed.getContent()).isEqualTo("수정된 내용");

        // 3. 피드 삭제
        mockMvc.perform(delete("/api/feeds/" + feed.getId()))
                .andExpect(status().isOk());

        // 4. 삭제 확인
        assertThat(feedRepository.findById(feed.getId())).isEmpty();
    }

    @Test
    @DisplayName("내 피드 목록 조회 통합 테스트")
    @WithMockUser(username = "testuser")
    void getMyFeeds() throws Exception {
        // given: 피드 3개 생성
        for (int i = 1; i <= 3; i++) {
            feedRepository.save(Feed.builder()
                    .writer(testUser)
                    .activity("RUNNING")
                    .content("피드 " + i)
                    .build());
        }

        // when & then
        mockMvc.perform(get("/api/feeds/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    @DisplayName("이미지와 함께 피드 생성 통합 테스트")
    @WithMockUser(username = "testuser")
    void createFeedWithImages() throws Exception {
        // given - startImage, endImage 타입별로 지정
        FeedRequest request = FeedRequest.builder()
                .activity("RUNNING")
                .content("사진 포함 피드")
                .startImage("image1.jpg")
                .endImage("image2.jpg")
                .build();

        // when
        mockMvc.perform(post("/api/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("사진 포함 피드"));

        // then
        Feed savedFeed = feedRepository.findAll().get(0);
        List<FeedImage> images = feedImageRepository.findByFeedOrderBySortOrderAsc(savedFeed);
        assertThat(images).hasSize(2);
        assertThat(images.get(0).getS3Key()).isEqualTo("image1.jpg");
        assertThat(images.get(1).getS3Key()).isEqualTo("image2.jpg");
    }

    @Test
    @DisplayName("다른 사용자의 피드 수정 시 403 반환")
    @WithMockUser(username = "otheruser")
    void updateFeedByOtherUser_Returns403() throws Exception {
        // given
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("원본 내용")
                .build());

        FeedRequest updateRequest = FeedRequest.builder()
                .activity("WALKING")
                .content("수정 시도")
                .build();

        // when & then
        mockMvc.perform(put("/api/feeds/" + feed.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("다른 사용자의 피드 삭제 시 403 반환")
    @WithMockUser(username = "otheruser")
    void deleteFeedByOtherUser_Returns403() throws Exception {
        // given
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("삭제 대상")
                .build());

        // when & then
        mockMvc.perform(delete("/api/feeds/" + feed.getId()))
                .andExpect(status().isForbidden());

        // 삭제되지 않았는지 확인
        assertThat(feedRepository.findById(feed.getId())).isPresent();
    }

    @Test
    @DisplayName("피드 삭제 시 이미지도 함께 삭제 통합 테스트")
    @WithMockUser(username = "testuser")
    void deleteFeedWithImages() throws Exception {
        // given - startImage, endImage 타입별로 지정
        FeedRequest request = FeedRequest.builder()
                .activity("RUNNING")
                .content("삭제할 피드")
                .startImage("image1.jpg")
                .endImage("image2.jpg")
                .build();

        mockMvc.perform(post("/api/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Feed savedFeed = feedRepository.findAll().get(0);
        Long feedId = savedFeed.getId();

        // when
        mockMvc.perform(delete("/api/feeds/" + feedId))
                .andExpect(status().isOk());

        // then
        assertThat(feedRepository.findById(feedId)).isEmpty();
        assertThat(feedImageRepository.findByFeedOrderBySortOrderAsc(savedFeed)).isEmpty();
    }
}
