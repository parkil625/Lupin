package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.response.FeedDetailResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedQueryService 테스트")
class FeedQueryServiceTest {

    @InjectMocks
    private FeedQueryService feedQueryService;

    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private FeedRepository feedRepository;
    @Mock
    private FeedLikeRepository feedLikeRepository;

    private User user;
    private Feed feed;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .userId("user01")
                .realName("테스트유저")
                .role(Role.MEMBER)
                .build();

        feed = Feed.builder()
                .id(1L)
                .activityType("러닝")
                .calories(200.0)
                .content("테스트 피드")
                .build();
        feed.setWriter(user);
        ReflectionTestUtils.setField(feed, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(feed, "images", new ArrayList<>());
        ReflectionTestUtils.setField(feed, "likes", new ArrayList<>());
        ReflectionTestUtils.setField(feed, "earnedPoints", 10L);
    }

    @Nested
    @DisplayName("피드 상세 조회")
    class GetFeedDetail {

        @Test
        @DisplayName("피드 상세 조회 성공")
        void getFeedDetail_Success() {
            // given
            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));

            // when
            FeedDetailResponse result = feedQueryService.getFeedDetail(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("테스트 피드");
        }

        @Test
        @DisplayName("존재하지 않는 피드 조회 실패")
        void getFeedDetail_NotFound_ThrowsException() {
            // given
            given(feedRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedQueryService.getFeedDetail(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("인기 피드 조회")
    class GetPopularFeeds {

        @Test
        @DisplayName("인기 피드 조회 성공")
        void getPopularFeeds_Success() {
            // given
            Feed feed2 = Feed.builder().id(2L).content("피드2").build();
            feed2.setWriter(user);

            given(feedRepository.findPopularFeeds(5)).willReturn(Arrays.asList(feed, feed2));

            // when
            List<Feed> result = feedQueryService.getPopularFeeds(5);

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("오늘 피드 작성 가능 여부")
    class CanPostToday {

        @Test
        @DisplayName("작성 가능 - 오늘 작성 안함")
        void canPostToday_True() {
            // given
            given(feedRepository.hasUserPostedToday(1L)).willReturn(false);

            // when
            boolean result = feedQueryService.canPostToday(1L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("작성 불가 - 오늘 이미 작성")
        void canPostToday_False() {
            // given
            given(feedRepository.hasUserPostedToday(1L)).willReturn(true);

            // when
            boolean result = feedQueryService.canPostToday(1L);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("좋아요 여부 확인")
    class HasUserLikedFeed {

        @Test
        @DisplayName("좋아요 여부 확인 - 좋아요함")
        void hasUserLikedFeed_True() {
            // given
            given(feedLikeRepository.existsByUserIdAndFeedId(1L, 1L)).willReturn(true);

            // when
            boolean result = feedQueryService.hasUserLikedFeed(1L, 1L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("좋아요 여부 확인 - 좋아요 안함")
        void hasUserLikedFeed_False() {
            // given
            given(feedLikeRepository.existsByUserIdAndFeedId(1L, 1L)).willReturn(false);

            // when
            boolean result = feedQueryService.hasUserLikedFeed(1L, 1L);

            // then
            assertThat(result).isFalse();
        }
    }
}
