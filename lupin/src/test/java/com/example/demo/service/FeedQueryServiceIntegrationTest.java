package com.example.demo.service;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.response.FeedListResponse;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD: 대시보드 피드 노출 테스트 (통합 테스트)
 * 요구사항: 피드 생성 후 대시보드에서 즉시 조회 가능해야 함
 * 문제: FeedQueryService가 Pageable의 Sort를 무시하고 있음
 */
@SpringBootTest
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("FeedQueryService 통합 테스트 - Pageable Sort 동작 확인")
class FeedQueryServiceIntegrationTest {

    @Autowired
    private FeedQueryService feedQueryService;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId("dashboardUser")
                .email("dashboard@example.com")
                .password("password")
                .realName("대시보드유저")
                .role(Role.MEMBER)
                .build();
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("피드 생성 후 getFeedsByUserId()로 즉시 조회 가능해야 함")
    void getFeedsByUserId_ShouldReturnNewlyCreatedFeed() {
        // given: 피드 생성
        Feed feed = Feed.builder()
                .activityType("러닝")
                .content("새로 작성한 피드")
                .calories(150.0)
                .build();
        feed.setWriter(testUser);
        feedRepository.save(feed);

        em.flush();
        em.clear();

        // when: 해당 사용자의 피드 조회
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FeedListResponse> result = feedQueryService.getFeedsByUserId(testUser.getId(), pageable);

        // then: 방금 생성한 피드가 조회되어야 함
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent().get(0).getContent()).isEqualTo("새로 작성한 피드");
    }

    @Test
    @DisplayName("getFeeds()는 Pageable의 Sort를 존중해야 함 - ID 오름차순")
    void getFeeds_ShouldRespectPageableSort_IdAscending() {
        // given: 여러 피드 생성
        Feed feed1 = Feed.builder()
                .activityType("러닝")
                .content("첫 번째 피드")
                .calories(100.0)
                .build();
        feed1.setWriter(testUser);
        feedRepository.save(feed1);

        Feed feed2 = Feed.builder()
                .activityType("헬스")
                .content("두 번째 피드")
                .calories(150.0)
                .build();
        feed2.setWriter(testUser);
        feedRepository.save(feed2);

        em.flush();
        em.clear();

        // when: ID 오름차순으로 조회
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
        Page<FeedListResponse> result = feedQueryService.getFeeds(null, null, null, null, pageable);

        // then: ID가 작은 것부터 나와야 함
        List<FeedListResponse> feeds = result.getContent();
        assertThat(feeds).hasSizeGreaterThanOrEqualTo(2);

        // 첫 번째 피드의 ID가 두 번째 피드의 ID보다 작아야 함
        for (int i = 0; i < feeds.size() - 1; i++) {
            assertThat(feeds.get(i).getId()).isLessThan(feeds.get(i + 1).getId());
        }
    }

    @Test
    @DisplayName("getFeedsByUserId()는 Pageable의 Sort를 존중해야 함 - ID 내림차순")
    void getFeedsByUserId_ShouldRespectPageableSort_IdDescending() {
        // given: 여러 피드 생성
        Feed feed1 = Feed.builder()
                .activityType("러닝")
                .content("첫 번째 피드")
                .calories(100.0)
                .build();
        feed1.setWriter(testUser);
        feedRepository.save(feed1);

        Feed feed2 = Feed.builder()
                .activityType("헬스")
                .content("두 번째 피드")
                .calories(150.0)
                .build();
        feed2.setWriter(testUser);
        feedRepository.save(feed2);

        Feed feed3 = Feed.builder()
                .activityType("요가")
                .content("세 번째 피드")
                .calories(200.0)
                .build();
        feed3.setWriter(testUser);
        feedRepository.save(feed3);

        em.flush();
        em.clear();

        // when: ID 내림차순으로 조회
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
        Page<FeedListResponse> result = feedQueryService.getFeedsByUserId(testUser.getId(), pageable);

        // then: 최신 피드(ID가 큰 것)가 먼저 나와야 함
        List<FeedListResponse> feeds = result.getContent();
        assertThat(feeds).hasSize(3);
        assertThat(feeds.get(0).getContent()).isEqualTo("세 번째 피드");
        assertThat(feeds.get(1).getContent()).isEqualTo("두 번째 피드");
        assertThat(feeds.get(2).getContent()).isEqualTo("첫 번째 피드");
    }

    @Test
    @DisplayName("기본 정렬은 createdAt DESC여야 함 - Sort 미지정 시")
    void getFeeds_ShouldDefaultToCreatedAtDesc_WhenNoSortProvided() {
        // given: 피드 생성
        Feed feed = Feed.builder()
                .activityType("러닝")
                .content("테스트 피드")
                .calories(100.0)
                .build();
        feed.setWriter(testUser);
        feedRepository.save(feed);

        em.flush();
        em.clear();

        // when: Sort 미지정
        Pageable pageable = PageRequest.of(0, 10);
        Page<FeedListResponse> result = feedQueryService.getFeeds(null, null, null, null, pageable);

        // then: 조회 성공 (기본 정렬 적용)
        assertThat(result.getContent()).isNotEmpty();
    }
}
