package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.response.FeedListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.config.TestRedisConfig;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("FeedRepository 통합 테스트")
class FeedRepositoryIntegrationTest {

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .userId("testuser1")
                .email("test1@test.com")
                .password("password")
                .realName("테스터1")
                .role(Role.MEMBER)
                .build());

        testUser2 = userRepository.save(User.builder()
                .userId("testuser2")
                .email("test2@test.com")
                .password("password")
                .realName("테스터2")
                .role(Role.MEMBER)
                .build());
    }

    @Test
    @DisplayName("키워드로 피드 검색")
    void searchFeeds_ByKeyword() {
        // given
        Feed feed1 = feedRepository.save(Feed.builder()
                .activityType("RUNNING")
                .content("오늘 조깅을 했습니다")
                .writer(testUser)
                .build());

        Feed feed2 = feedRepository.save(Feed.builder()
                .activityType("WALKING")
                .content("산책을 즐겼습니다")
                .writer(testUser)
                .build());

        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<FeedListResponse> result = feedRepository.searchFeeds("조깅", null, null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).contains("조깅");
    }

    @Test
    @DisplayName("활동 유형으로 피드 검색")
    void searchFeeds_ByActivityType() {
        // given
        feedRepository.save(Feed.builder()
                .activityType("RUNNING")
                .content("러닝 피드")
                .writer(testUser)
                .build());

        feedRepository.save(Feed.builder()
                .activityType("WALKING")
                .content("워킹 피드")
                .writer(testUser)
                .build());

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Page<FeedListResponse> result = feedRepository.searchFeeds(null, "RUNNING", null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getActivity()).isEqualTo("RUNNING");
    }

    @Test
    @DisplayName("특정 사용자 제외하고 검색")
    void searchFeeds_ExcludeUser() {
        // given
        feedRepository.save(Feed.builder()
                .activityType("RUNNING")
                .content("유저1 피드")
                .writer(testUser)
                .build());

        feedRepository.save(Feed.builder()
                .activityType("RUNNING")
                .content("유저2 피드")
                .writer(testUser2)
                .build());

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Page<FeedListResponse> result = feedRepository.searchFeeds(null, null, testUser.getId(), null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("작성자 ID로 피드 조회")
    void findByWriterId_Success() {
        // given
        feedRepository.save(Feed.builder()
                .activityType("RUNNING")
                .content("피드1")
                .writer(testUser)
                .build());

        feedRepository.save(Feed.builder()
                .activityType("WALKING")
                .content("피드2")
                .writer(testUser)
                .build());

        feedRepository.save(Feed.builder()
                .activityType("RUNNING")
                .content("다른 유저 피드")
                .writer(testUser2)
                .build());

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Page<FeedListResponse> result = feedRepository.findByWriterId(testUser.getId(), pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("사용자 피드 수 조회")
    void countUserFeeds_Success() {
        // given
        feedRepository.save(Feed.builder()
                .activityType("RUNNING")
                .content("피드1")
                .writer(testUser)
                .build());

        feedRepository.save(Feed.builder()
                .activityType("WALKING")
                .content("피드2")
                .writer(testUser)
                .build());

        // when
        Long count = feedRepository.countUserFeeds(testUser.getId());

        // then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("인기 피드 조회")
    void findPopularFeeds_Success() {
        // given
        Feed feed1 = Feed.builder()
                .activityType("RUNNING")
                .content("피드1")
                .writer(testUser)
                .build();
        feed1.setEarnedPoints(100L);
        feedRepository.save(feed1);

        Feed feed2 = Feed.builder()
                .activityType("WALKING")
                .content("피드2")
                .writer(testUser)
                .build();
        feed2.setEarnedPoints(50L);
        feedRepository.save(feed2);

        // when
        List<Feed> result = feedRepository.findPopularFeeds(10);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEarnedPoints()).isGreaterThanOrEqualTo(result.get(1).getEarnedPoints());
    }

    @Test
    @DisplayName("오늘 게시 여부 확인 - 게시함")
    void hasUserPostedToday_True() {
        // given
        feedRepository.save(Feed.builder()
                .activityType("RUNNING")
                .content("오늘 피드")
                .writer(testUser)
                .build());

        // when
        boolean result = feedRepository.hasUserPostedToday(testUser.getId());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("오늘 게시 여부 확인 - 게시 안함")
    void hasUserPostedToday_False() {
        // when
        boolean result = feedRepository.hasUserPostedToday(testUser.getId());

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("페이징 처리")
    void searchFeeds_WithPaging() {
        // given
        for (int i = 0; i < 15; i++) {
            feedRepository.save(Feed.builder()
                    .activityType("RUNNING")
                    .content("피드 " + i)
                    .writer(testUser)
                    .build());
        }

        PageRequest pageable = PageRequest.of(0, 10);

        // when
        Page<FeedListResponse> result = feedRepository.searchFeeds(null, null, null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(15);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }
}
