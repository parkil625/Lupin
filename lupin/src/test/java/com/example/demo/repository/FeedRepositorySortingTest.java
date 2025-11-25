package com.example.demo.repository;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.response.FeedListResponse;
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
 * TDD: 피드 메뉴 정렬 테스트
 * 요구사항: 피드 메뉴에서 타인의 피드가 피드 ID 오름차순으로 보여야 함
 */
@SpringBootTest
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("피드 정렬 기능 테스트")
class FeedRepositorySortingTest {

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        // 테스트용 유저 생성
        testUser = User.builder()
                .userId("testUser")
                .email("test@example.com")
                .password("password")
                .realName("테스트유저")
                .role(Role.MEMBER)
                .build();
        userRepository.save(testUser);

        otherUser = User.builder()
                .userId("otherUser")
                .email("other@example.com")
                .password("password")
                .realName("다른유저")
                .role(Role.MEMBER)
                .build();
        userRepository.save(otherUser);
    }

    @Test
    @DisplayName("피드 목록 조회 시 ID 오름차순으로 정렬되어야 한다 (기본 정렬)")
    void searchFeeds_ShouldReturnFeedsInAscendingIdOrder_WhenNoSortProvided() {
        // given: 다른 유저가 작성한 여러 개의 피드 생성
        Feed feed1 = Feed.builder()
                .activityType("러닝")
                .content("첫 번째 피드")
                .calories(100.0)
                .build();
        feed1.setWriter(otherUser);
        feedRepository.save(feed1);

        Feed feed2 = Feed.builder()
                .activityType("헬스")
                .content("두 번째 피드")
                .calories(150.0)
                .build();
        feed2.setWriter(otherUser);
        feedRepository.save(feed2);

        Feed feed3 = Feed.builder()
                .activityType("요가")
                .content("세 번째 피드")
                .calories(200.0)
                .build();
        feed3.setWriter(otherUser);
        feedRepository.save(feed3);

        // 영속성 컨텍스트 비우기
        em.flush();
        em.clear();

        // when: ID 오름차순 정렬로 피드 조회 (내 피드 제외)
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
        Page<FeedListResponse> result = feedRepository.searchFeeds(
                null,  // keyword
                null,  // activityType
                testUser.getId(),  // excludeUserId - 내 피드 제외
                null,  // excludeFeedId
                pageable
        );

        // then: 피드 ID 오름차순으로 정렬되어야 함 (가장 오래된 피드가 먼저)
        List<FeedListResponse> feeds = result.getContent();
        assertThat(feeds).hasSize(3);

        // ID가 오름차순인지 확인
        assertThat(feeds.get(0).getId()).isLessThan(feeds.get(1).getId());
        assertThat(feeds.get(1).getId()).isLessThan(feeds.get(2).getId());

        // 첫 번째로 나온 피드가 가장 먼저 생성된 피드인지 확인
        assertThat(feeds.get(0).getContent()).isEqualTo("첫 번째 피드");
        assertThat(feeds.get(1).getContent()).isEqualTo("두 번째 피드");
        assertThat(feeds.get(2).getContent()).isEqualTo("세 번째 피드");
    }

    @Test
    @DisplayName("내 피드는 제외하고 다른 사람의 피드만 조회해야 한다")
    void searchFeeds_ShouldExcludeMyFeeds_WhenExcludeUserIdProvided() {
        // given: 내가 작성한 피드와 다른 사람이 작성한 피드 생성
        Feed myFeed = Feed.builder()
                .activityType("러닝")
                .content("내 피드")
                .calories(100.0)
                .build();
        myFeed.setWriter(testUser);
        feedRepository.save(myFeed);

        Feed otherFeed = Feed.builder()
                .activityType("헬스")
                .content("다른 사람 피드")
                .calories(150.0)
                .build();
        otherFeed.setWriter(otherUser);
        feedRepository.save(otherFeed);

        em.flush();
        em.clear();

        // when: 내 피드를 제외하고 조회
        Pageable pageable = PageRequest.of(0, 10);
        Page<FeedListResponse> result = feedRepository.searchFeeds(
                null,
                null,
                testUser.getId(),  // 내 ID로 필터링
                null,
                pageable
        );

        // then: 다른 사람의 피드만 조회되어야 함
        List<FeedListResponse> feeds = result.getContent();
        assertThat(feeds).hasSize(1);
        assertThat(feeds.get(0).getContent()).isEqualTo("다른 사람 피드");
        assertThat(feeds.get(0).getAuthor()).isEqualTo("다른유저");
    }

    @Test
    @DisplayName("특정 활동 타입 필터링 시에도 ID 오름차순으로 정렬되어야 한다")
    void searchFeeds_ShouldReturnInAscendingOrder_WhenFilteredByActivityType() {
        // given: 같은 활동 타입의 여러 피드 생성
        Feed feed1 = Feed.builder()
                .activityType("러닝")
                .content("러닝 피드 1")
                .calories(100.0)
                .build();
        feed1.setWriter(otherUser);
        feedRepository.save(feed1);

        Feed feed2 = Feed.builder()
                .activityType("헬스")
                .content("헬스 피드")
                .calories(150.0)
                .build();
        feed2.setWriter(otherUser);
        feedRepository.save(feed2);

        Feed feed3 = Feed.builder()
                .activityType("러닝")
                .content("러닝 피드 2")
                .calories(200.0)
                .build();
        feed3.setWriter(otherUser);
        feedRepository.save(feed3);

        em.flush();
        em.clear();

        // when: 러닝 활동만 필터링하여 ID 오름차순으로 조회
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
        Page<FeedListResponse> result = feedRepository.searchFeeds(
                null,
                "러닝",  // activityType 필터
                testUser.getId(),
                null,
                pageable
        );

        // then: 러닝 피드만 조회되고 ID 오름차순으로 정렬
        List<FeedListResponse> feeds = result.getContent();
        assertThat(feeds).hasSize(2);
        assertThat(feeds.get(0).getId()).isLessThan(feeds.get(1).getId());
        assertThat(feeds.get(0).getContent()).isEqualTo("러닝 피드 1");
        assertThat(feeds.get(1).getContent()).isEqualTo("러닝 피드 2");
    }
}
