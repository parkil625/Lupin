package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.FeedUpdateRequest;
import com.example.demo.dto.response.FeedDetailResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.LotteryTicketRepository;
import com.example.demo.repository.UserPenaltyRepository;
import com.example.demo.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedService 테스트")
class FeedServiceTest {

    @InjectMocks
    private FeedService feedService;

    @Mock
    private FeedRepository feedRepository;
    @Mock
    private FeedLikeRepository feedLikeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPenaltyRepository userPenaltyRepository;
    @Mock
    private UserService userService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ImageService imageService;
    @Mock
    private LotteryTicketRepository lotteryTicketRepository;

    private User user;
    private User feedWriter;
    private Feed feed;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .userId("user01")
                .realName("테스트유저")
                .role(Role.MEMBER)
                .monthlyLikes(0L)
                .monthlyPoints(0L)
                .currentPoints(0L)
                .build();

        feedWriter = User.builder()
                .id(2L)
                .userId("writer01")
                .realName("피드작성자")
                .role(Role.MEMBER)
                .monthlyLikes(10L)
                .build();

        feed = Feed.builder()
                .id(1L)
                .content("테스트 피드")
                .activityType("러닝")
                .calories(200.0)
                .build();
        feed.setWriter(feedWriter);
        ReflectionTestUtils.setField(feed, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(feed, "earnedPoints", 10L);
        ReflectionTestUtils.setField(feed, "images", new ArrayList<>());
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
            FeedDetailResponse result = feedService.getFeedDetail(1L);

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
            assertThatThrownBy(() -> feedService.getFeedDetail(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("피드 수정")
    class UpdateFeed {

        @Test
        @DisplayName("피드 수정 성공")
        void updateFeed_Success() {
            // given
            Feed myFeed = Feed.builder().id(1L).content("원본").build();
            myFeed.setWriter(user);
            ReflectionTestUtils.setField(myFeed, "images", new ArrayList<>());
            ReflectionTestUtils.setField(myFeed, "createdAt", LocalDateTime.now());

            FeedUpdateRequest request = new FeedUpdateRequest("수정된 내용");

            given(feedRepository.findById(1L)).willReturn(Optional.of(myFeed));

            // when
            FeedDetailResponse result = feedService.updateFeed(1L, 1L, request);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("다른 사용자 피드 수정 실패")
        void updateFeed_NotOwner_ThrowsException() {
            // given
            FeedUpdateRequest request = new FeedUpdateRequest("수정된 내용");
            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));

            // when & then
            assertThatThrownBy(() -> feedService.updateFeed(1L, 1L, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("피드 좋아요")
    class LikeFeed {

        @Test
        @DisplayName("좋아요 성공")
        void likeFeed_Success() {
            // given
            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedLikeRepository.existsByUserIdAndFeedId(1L, 1L)).willReturn(false);

            // when
            feedService.likeFeed(1L, 1L);

            // then
            then(feedLikeRepository).should().save(any(FeedLike.class));
        }

        @Test
        @DisplayName("자신의 피드에 좋아요 실패")
        void likeFeed_OwnFeed_ThrowsException() {
            // given
            Feed myFeed = Feed.builder().id(1L).build();
            myFeed.setWriter(user);

            given(feedRepository.findById(1L)).willReturn(Optional.of(myFeed));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> feedService.likeFeed(1L, 1L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("피드 좋아요 취소")
    class UnlikeFeed {

        @Test
        @DisplayName("좋아요 취소 성공")
        void unlikeFeed_Success() {
            // given
            FeedLike feedLike = FeedLike.builder().id(1L).user(user).feed(feed).build();

            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));
            given(feedLikeRepository.findByUserIdAndFeedId(1L, 1L)).willReturn(Optional.of(feedLike));

            // when
            feedService.unlikeFeed(1L, 1L);

            // then
            then(feedLikeRepository).should().delete(feedLike);
        }
    }

    @Nested
    @DisplayName("인기 피드 조회")
    class GetPopularFeeds {

        @Test
        @DisplayName("인기 피드 조회 성공")
        void getPopularFeeds_Success() {
            // given
            given(feedRepository.findPopularFeeds(5)).willReturn(Arrays.asList(feed));

            // when
            List<Feed> result = feedService.getPopularFeeds(5);

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("오늘 피드 작성 가능 여부")
    class CanPostToday {

        @Test
        @DisplayName("작성 가능")
        void canPostToday_True() {
            // given
            given(feedRepository.hasUserPostedToday(1L)).willReturn(false);

            // when
            boolean result = feedService.canPostToday(1L);

            // then
            assertThat(result).isTrue();
        }
    }
}
