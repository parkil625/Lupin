package com.example.demo.integration;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.FeedRequest;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
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

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .userId("testuser")
                .password("password")
                .name("테스트유저")
                .role(Role.MEMBER)
                .build());
    }

    @Test
    @DisplayName("피드 생성 → 조회 통합 테스트")
    @WithMockUser(username = "testuser")
    void createAndGetFeed() throws Exception {
        // 1. 피드 생성
        FeedRequest request = FeedRequest.builder()
                .activity("RUNNING")
                .content("오늘 5km 달리기 완료!")
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
}
