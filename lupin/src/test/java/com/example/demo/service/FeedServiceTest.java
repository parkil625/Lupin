package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ImageType;
import com.example.demo.domain.enums.Role;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedImageRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedService 테스트")
class FeedServiceTest {

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private FeedImageRepository feedImageRepository;

    @Mock
    private PointService pointService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private FeedService feedService;

    private User writer;

    @BeforeEach
    void setUp() {
        writer = User.builder()
                .userId("writer")
                .password("password")
                .name("작성자")
                .role(Role.MEMBER)
                .build();
    }

    @Test
    @DisplayName("피드를 작성한다")
    void createFeedTest() {
        // given
        String activity = "running";
        String content = "오늘 5km 달렸습니다";
        given(feedRepository.save(any(Feed.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Feed result = feedService.createFeed(writer, activity, content);

        // then
        assertThat(result.getWriter()).isEqualTo(writer);
        assertThat(result.getActivity()).isEqualTo(activity);
        assertThat(result.getContent()).isEqualTo(content);
        verify(feedRepository).save(any(Feed.class));
    }

    @Test
    @DisplayName("피드를 수정한다")
    void updateFeedTest() {
        // given
        Long feedId = 1L;
        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("원래 내용")
                .build();

        String newContent = "수정된 내용";
        String newActivity = "walking";

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));

        // when
        Feed result = feedService.updateFeed(writer, feedId, newContent, newActivity);

        // then
        assertThat(result.getContent()).isEqualTo(newContent);
        assertThat(result.getActivity()).isEqualTo(newActivity);
    }

    @Test
    @DisplayName("존재하지 않는 피드를 수정하면 예외가 발생한다")
    void updateFeedNotFoundTest() {
        // given
        Long feedId = 999L;
        given(feedRepository.findById(feedId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> feedService.updateFeed(writer, feedId, "내용", "activity"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
    }

    @Test
    @DisplayName("피드를 삭제한다")
    void deleteFeedTest() {
        // given
        Long feedId = 1L;
        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("내용")
                .build();

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));

        // when
        feedService.deleteFeed(writer, feedId);

        // then
        verify(feedRepository).delete(feed);
    }

    @Test
    @DisplayName("7일 이내 피드 삭제 시 포인트를 회수한다")
    void deleteFeedWithin7DaysRecoverPointsTest() {
        // given
        Long feedId = 1L;
        Long earnedPoints = 100L;
        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("내용")
                .points(earnedPoints)
                .build();
        // 3일 전에 생성된 피드
        ReflectionTestUtils.setField(feed, "createdAt", LocalDateTime.now().minusDays(3));

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));

        // when
        feedService.deleteFeed(writer, feedId);

        // then
        verify(pointService).deductPoints(writer, earnedPoints);
        verify(feedRepository).delete(feed);
    }

    @Test
    @DisplayName("7일 초과 피드 삭제 시 포인트를 회수하지 않는다")
    void deleteFeedAfter7DaysNoRecoverTest() {
        // given
        Long feedId = 1L;
        Long earnedPoints = 100L;
        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("내용")
                .points(earnedPoints)
                .build();
        // 10일 전에 생성된 피드
        ReflectionTestUtils.setField(feed, "createdAt", LocalDateTime.now().minusDays(10));

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));

        // when
        feedService.deleteFeed(writer, feedId);

        // then
        verify(pointService, never()).deductPoints(any(User.class), anyLong());
        verify(feedRepository).delete(feed);
    }

    @Test
    @DisplayName("포인트가 0인 피드 삭제 시 회수하지 않는다")
    void deleteFeedWithZeroPointsTest() {
        // given
        Long feedId = 1L;
        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("내용")
                .points(0L)
                .build();
        ReflectionTestUtils.setField(feed, "createdAt", LocalDateTime.now().minusDays(3));

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));

        // when
        feedService.deleteFeed(writer, feedId);

        // then
        verify(pointService, never()).deductPoints(any(User.class), anyLong());
        verify(feedRepository).delete(feed);
    }

    @Test
    @DisplayName("피드 상세를 조회한다")
    void getFeedDetailTest() {
        // given
        Long feedId = 1L;
        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("내용")
                .build();

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));

        // when
        Feed result = feedService.getFeedDetail(feedId);

        // then
        assertThat(result).isEqualTo(feed);
    }

    @Test
    @DisplayName("존재하지 않는 피드를 조회하면 예외가 발생한다")
    void getFeedDetailNotFoundTest() {
        // given
        Long feedId = 999L;
        given(feedRepository.findById(feedId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> feedService.getFeedDetail(feedId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
    }

    @Test
    @DisplayName("오늘 피드를 작성하지 않았으면 작성 가능하다")
    void canPostTodayWhenNoFeedTodayTest() {
        // given
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        given(feedRepository.existsByWriterAndCreatedAtBetween(writer, startOfDay, endOfDay))
                .willReturn(false);

        // when
        boolean canPost = feedService.canPostToday(writer);

        // then
        assertThat(canPost).isTrue();
    }

    @Test
    @DisplayName("오늘 피드를 이미 작성했으면 작성 불가능하다")
    void canPostTodayWhenFeedAlreadyPostedTest() {
        // given
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        given(feedRepository.existsByWriterAndCreatedAtBetween(writer, startOfDay, endOfDay))
                .willReturn(true);

        // when
        boolean canPost = feedService.canPostToday(writer);

        // then
        assertThat(canPost).isFalse();
    }

    @Test
    @DisplayName("이미지와 함께 피드를 생성한다")
    void createFeedWithImagesTest() {
        // given
        String activity = "running";
        String content = "오늘 5km 달렸습니다";
        List<String> s3Keys = List.of("image1.jpg", "image2.jpg");

        given(feedRepository.save(any(Feed.class))).willAnswer(invocation -> {
            Feed feed = invocation.getArgument(0);
            ReflectionTestUtils.setField(feed, "id", 1L);
            return feed;
        });
        given(feedImageRepository.save(any(FeedImage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Feed result = feedService.createFeed(writer, activity, content, s3Keys);

        // then
        assertThat(result.getWriter()).isEqualTo(writer);
        assertThat(result.getActivity()).isEqualTo(activity);
        assertThat(result.getContent()).isEqualTo(content);
        verify(feedRepository).save(any(Feed.class));
        verify(feedImageRepository).save(argThat(image ->
                image.getS3Key().equals("image1.jpg") && image.getSortOrder() == 0));
        verify(feedImageRepository).save(argThat(image ->
                image.getS3Key().equals("image2.jpg") && image.getSortOrder() == 1));
    }

    @Test
    @DisplayName("이미지 없이 피드를 생성해도 정상 동작한다")
    void createFeedWithoutImagesTest() {
        // given
        String activity = "running";
        String content = "오늘 5km 달렸습니다";
        List<String> s3Keys = List.of();

        given(feedRepository.save(any(Feed.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Feed result = feedService.createFeed(writer, activity, content, s3Keys);

        // then
        assertThat(result.getWriter()).isEqualTo(writer);
        verify(feedRepository).save(any(Feed.class));
        verify(feedImageRepository, never()).save(any(FeedImage.class));
    }

    @Test
    @DisplayName("피드 삭제 시 연관된 이미지도 삭제된다")
    void deleteFeedWithImagesTest() {
        // given
        Long feedId = 1L;
        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("내용")
                .points(0L)
                .build();
        ReflectionTestUtils.setField(feed, "createdAt", LocalDateTime.now().minusDays(10));

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));

        // when
        feedService.deleteFeed(writer, feedId);

        // then
        verify(feedImageRepository).deleteByFeed(feed);
        verify(feedRepository).delete(feed);
    }

    @Test
    @DisplayName("존재하지 않는 피드를 삭제하면 예외가 발생한다")
    void deleteFeedNotFoundTest() {
        // given
        Long feedId = 999L;
        given(feedRepository.findById(feedId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> feedService.deleteFeed(writer, feedId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
    }

    @Test
    @DisplayName("본인이 아닌 사용자가 피드를 수정하면 예외가 발생한다")
    void updateFeedByNonOwnerThrowsExceptionTest() {
        // given
        Long feedId = 1L;
        User otherUser = User.builder()
                .userId("other")
                .password("password")
                .name("다른사람")
                .role(Role.MEMBER)
                .build();
        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("원래 내용")
                .build();

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));

        // when & then
        assertThatThrownBy(() -> feedService.updateFeed(otherUser, feedId, "수정 내용", "walking"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_OWNER);
    }

    @Test
    @DisplayName("본인이 아닌 사용자가 피드를 삭제하면 예외가 발생한다")
    void deleteFeedByNonOwnerThrowsExceptionTest() {
        // given
        Long feedId = 1L;
        User otherUser = User.builder()
                .userId("other")
                .password("password")
                .name("다른사람")
                .role(Role.MEMBER)
                .build();
        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("내용")
                .points(0L)
                .build();

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));

        // when & then
        assertThatThrownBy(() -> feedService.deleteFeed(otherUser, feedId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_OWNER);
    }

    @Test
    @DisplayName("피드 삭제 시 관련 알림도 삭제된다")
    void deleteFeedDeletesRelatedNotificationsTest() {
        // given
        Long feedId = 1L;
        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("내용")
                .points(0L)
                .build();
        ReflectionTestUtils.setField(feed, "id", feedId);
        ReflectionTestUtils.setField(feed, "createdAt", LocalDateTime.now().minusDays(10));

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));

        // when
        feedService.deleteFeed(writer, feedId);

        // then
        verify(notificationRepository).deleteByRefIdAndTypeIn(
                String.valueOf(feedId),
                List.of("FEED_LIKE", "COMMENT")
        );
        verify(feedRepository).delete(feed);
    }
}
