package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.FeedCreateRequest;
import com.example.demo.dto.request.FeedUpdateRequest;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedCommandService 테스트")
class FeedCommandServiceTest {

    @InjectMocks
    private FeedCommandService feedCommandService;

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
                .gender("남성")
                .birthDate(LocalDate.of(1990, 1, 1))
                .height(175.0)
                .weight(70.0)
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
    }

    @Nested
    @DisplayName("피드 생성")
    class CreateFeed {

        @Test
        @DisplayName("피드 생성 성공")
        void createFeed_Success() {
            // given
            FeedCreateRequest request = FeedCreateRequest.builder()
                    .activityType("러닝")
                    .content("오늘 운동")
                    .images(Arrays.asList("image1.jpg", "image2.jpg"))
                    .startedAt(LocalDateTime.now().minusHours(1))
                    .endedAt(LocalDateTime.now())
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userPenaltyRepository.hasActivePenalty(eq(1L), eq("FEED"), any(LocalDateTime.class)))
                    .willReturn(false);
            given(feedRepository.hasUserPostedToday(1L)).willReturn(false);
            given(feedRepository.save(any(Feed.class))).willAnswer(invocation -> {
                Feed saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 10L);
                return saved;
            });

            // when
            Long feedId = feedCommandService.createFeed(1L, request);

            // then
            assertThat(feedId).isEqualTo(10L);
            then(feedRepository).should().save(any(Feed.class));
        }

        @Test
        @DisplayName("패널티 유저 피드 생성 실패")
        void createFeed_WithPenalty_ThrowsException() {
            // given
            FeedCreateRequest request = FeedCreateRequest.builder()
                    .activityType("러닝")
                    .content("오늘 운동")
                    .images(Arrays.asList("image1.jpg", "image2.jpg"))
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userPenaltyRepository.hasActivePenalty(eq(1L), eq("FEED"), any(LocalDateTime.class)))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> feedCommandService.createFeed(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("신고");
        }

        @Test
        @DisplayName("하루 제한 초과시 피드 생성 실패")
        void createFeed_DailyLimitExceeded_ThrowsException() {
            // given
            FeedCreateRequest request = FeedCreateRequest.builder()
                    .activityType("러닝")
                    .content("오늘 운동")
                    .images(Arrays.asList("image1.jpg", "image2.jpg"))
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userPenaltyRepository.hasActivePenalty(eq(1L), eq("FEED"), any(LocalDateTime.class)))
                    .willReturn(false);
            given(feedRepository.hasUserPostedToday(1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> feedCommandService.createFeed(1L, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("이미지 부족시 피드 생성 실패")
        void createFeed_InsufficientImages_ThrowsException() {
            // given
            FeedCreateRequest request = FeedCreateRequest.builder()
                    .activityType("러닝")
                    .content("오늘 운동")
                    .images(Arrays.asList("image1.jpg"))
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userPenaltyRepository.hasActivePenalty(eq(1L), eq("FEED"), any(LocalDateTime.class)))
                    .willReturn(false);
            given(feedRepository.hasUserPostedToday(1L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> feedCommandService.createFeed(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("사진");
        }
    }

    @Nested
    @DisplayName("피드 수정")
    class UpdateFeed {

        @Test
        @DisplayName("피드 수정 성공")
        void updateFeed_Success() {
            // given
            Feed myFeed = Feed.builder()
                    .id(1L)
                    .content("원본 내용")
                    .build();
            myFeed.setWriter(user);

            FeedUpdateRequest request = new FeedUpdateRequest("수정된 내용");

            given(feedRepository.findById(1L)).willReturn(Optional.of(myFeed));

            // when
            feedCommandService.updateFeed(1L, 1L, request);

            // then
            assertThat(myFeed.getContent()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("다른 사용자 피드 수정 실패")
        void updateFeed_NotOwner_ThrowsException() {
            // given
            FeedUpdateRequest request = new FeedUpdateRequest("수정된 내용");
            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));

            // when & then
            assertThatThrownBy(() -> feedCommandService.updateFeed(1L, 1L, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("피드 삭제")
    class DeleteFeed {

        @Test
        @DisplayName("피드 삭제 성공")
        void deleteFeed_Success() {
            // given
            Feed myFeed = Feed.builder()
                    .id(1L)
                    .content("삭제할 피드")
                    .build();
            myFeed.setWriter(user);
            ReflectionTestUtils.setField(myFeed, "createdAt", LocalDateTime.now());

            given(feedRepository.findById(1L)).willReturn(Optional.of(myFeed));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when
            feedCommandService.deleteFeed(1L, 1L);

            // then
            then(feedRepository).should().delete(myFeed);
        }

        @Test
        @DisplayName("다른 사용자 피드 삭제 실패")
        void deleteFeed_NotOwner_ThrowsException() {
            // given
            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> feedCommandService.deleteFeed(1L, 1L))
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
            feedCommandService.likeFeed(1L, 1L);

            // then
            then(feedLikeRepository).should().save(any(FeedLike.class));
            then(notificationService).should().createLikeNotification(eq(2L), eq(1L), eq(1L));
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
            assertThatThrownBy(() -> feedCommandService.likeFeed(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("자신의 피드");
        }

        @Test
        @DisplayName("이미 좋아요한 피드에 좋아요 실패")
        void likeFeed_AlreadyLiked_ThrowsException() {
            // given
            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedLikeRepository.existsByUserIdAndFeedId(1L, 1L)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> feedCommandService.likeFeed(1L, 1L))
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
            FeedLike feedLike = FeedLike.builder()
                    .id(1L)
                    .user(user)
                    .feed(feed)
                    .build();

            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));
            given(feedLikeRepository.findByUserIdAndFeedId(1L, 1L)).willReturn(Optional.of(feedLike));

            // when
            feedCommandService.unlikeFeed(1L, 1L);

            // then
            then(feedLikeRepository).should().delete(feedLike);
        }

        @Test
        @DisplayName("좋아요하지 않은 피드 취소 실패")
        void unlikeFeed_NotLiked_ThrowsException() {
            // given
            given(feedRepository.findById(1L)).willReturn(Optional.of(feed));
            given(feedLikeRepository.findByUserIdAndFeedId(1L, 1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedCommandService.unlikeFeed(1L, 1L))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
