package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.event.NotificationEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedLikeService 테스트")
class FeedLikeServiceTest {

    @Mock
    private FeedLikeRepository feedLikeRepository;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FeedLikeService feedLikeService;

    private User user;
    private User writer;
    private Feed feed;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("user")
                .password("password")
                .name("사용자")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        writer = User.builder()
                .userId("writer")
                .password("password")
                .name("작성자")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(writer, "id", 2L);

        feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("피드 내용")
                .build();
        ReflectionTestUtils.setField(feed, "id", 1L);
    }

    @Test
    @DisplayName("피드에 좋아요를 누른다")
    void likeFeedTest() {
        // given
        Long feedId = 1L;
        given(feedLikeRepository.existsByUserIdAndFeedId(user.getId(), feedId)).willReturn(false);
        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(feedLikeRepository.save(any(FeedLike.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        FeedLike result = feedLikeService.likeFeed(user, feedId);

        // then
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getFeed()).isEqualTo(feed);
        verify(feedLikeRepository).save(any(FeedLike.class));
    }

    @Test
    @DisplayName("피드에 좋아요를 누르면 피드 작성자에게 알림 이벤트가 발행된다")
    void likeFeedCreatesNotificationTest() {
        // given
        Long feedId = 1L;
        given(feedLikeRepository.existsByUserIdAndFeedId(user.getId(), feedId)).willReturn(false);
        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(feedLikeRepository.save(any(FeedLike.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        feedLikeService.likeFeed(user, feedId);

        // then - 이벤트 발행 검증
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("이미 좋아요한 피드에 다시 좋아요를 누르면 예외가 발생한다")
    void likeFeedAlreadyLikedTest() {
        // given
        Long feedId = 1L;
        given(feedLikeRepository.existsByUserIdAndFeedId(user.getId(), feedId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> feedLikeService.likeFeed(user, feedId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_LIKED);
    }

    @Test
    @DisplayName("피드 좋아요를 취소하면 관련 알림도 삭제된다")
    void unlikeFeedTest() {
        // given
        Long feedId = 1L;
        FeedLike feedLike = FeedLike.builder()
                .user(user)
                .feed(feed)
                .build();

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(feedLikeRepository.findByUserAndFeed(user, feed)).willReturn(Optional.of(feedLike));

        // when
        feedLikeService.unlikeFeed(user, feedId);

        // then - refId는 feedId 사용
        verify(notificationRepository).deleteByRefIdAndType(String.valueOf(feedId), "FEED_LIKE");
        verify(feedLikeRepository).delete(feedLike);
    }

    @Test
    @DisplayName("좋아요하지 않은 피드의 좋아요를 취소하면 예외가 발생한다")
    void unlikeFeedNotLikedTest() {
        // given
        Long feedId = 1L;
        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(feedLikeRepository.findByUserAndFeed(user, feed)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> feedLikeService.unlikeFeed(user, feedId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LIKE_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 피드에 좋아요를 누르면 예외가 발생한다")
    void likeFeedNotFoundTest() {
        // given
        Long feedId = 999L;
        given(feedLikeRepository.existsByUserIdAndFeedId(user.getId(), feedId)).willReturn(false);
        given(feedRepository.findById(feedId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> feedLikeService.likeFeed(user, feedId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
    }
}
