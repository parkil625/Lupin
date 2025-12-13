package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedService 테스트")
class FeedServiceTest {

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private ImageMetadataService imageMetadataService;

    @Mock
    private FeedTransactionService feedTransactionService;

    @Mock
    private FeedDeleteFacade feedDeleteFacade;

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
        ReflectionTestUtils.setField(writer, "id", 1L);
    }

    @Test
    @DisplayName("이미지 없이 피드를 작성하면 예외가 발생한다")
    void createFeedWithoutImagesThrowsExceptionTest() {
        // given
        String activity = "달리기";
        String content = "오늘 5km 달렸습니다";

        // when & then (이미지가 필수이므로 예외 발생)
        assertThatThrownBy(() -> feedService.createFeed(writer, activity, content))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_IMAGES_REQUIRED);
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

        given(feedRepository.findByIdWithWriterAndImages(feedId)).willReturn(Optional.of(feed));

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
        given(feedRepository.findByIdWithWriterAndImages(feedId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> feedService.updateFeed(writer, feedId, "내용", "activity"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
    }

    @Test
    @DisplayName("피드 삭제는 FeedDeleteFacade에 위임한다")
    void deleteFeedDelegatesToFacadeTest() {
        // given
        Long feedId = 1L;

        // when
        feedService.deleteFeed(writer, feedId);

        // then
        verify(feedDeleteFacade).deleteFeed(writer, feedId);
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

        given(feedRepository.findByIdWithWriterAndImages(feedId)).willReturn(Optional.of(feed));

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
        given(feedRepository.findByIdWithWriterAndImages(feedId)).willReturn(Optional.empty());

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

        given(feedRepository.existsByWriter_IdAndCreatedAtBetween(writer.getId(), startOfDay, endOfDay))
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

        given(feedRepository.existsByWriter_IdAndCreatedAtBetween(writer.getId(), startOfDay, endOfDay))
                .willReturn(true);

        // when
        boolean canPost = feedService.canPostToday(writer);

        // then
        assertThat(canPost).isFalse();
    }

    @Test
    @DisplayName("이미지와 함께 피드를 생성한다 - EXIF 시간 검증 포함")
    void createFeedWithImagesTest() {
        // given
        String activity = "달리기";
        String content = "오늘 5km 달렸습니다";
        List<String> s3Keys = List.of("start.jpg", "end.jpg");
        // 오늘 날짜 기준으로 테스트 (당일 검증 통과)
        LocalDateTime startTime = LocalDate.now().atTime(10, 0);
        LocalDateTime endTime = LocalDate.now().atTime(11, 0); // 1시간 운동

        Feed createdFeed = Feed.builder()
                .writer(writer)
                .activity(activity)
                .content(content)
                .points(30L)
                .calories(600)
                .build();
        ReflectionTestUtils.setField(createdFeed, "id", 1L);

        given(imageMetadataService.extractPhotoDateTime("start.jpg"))
                .willReturn(Optional.of(startTime));
        given(imageMetadataService.extractPhotoDateTime("end.jpg"))
                .willReturn(Optional.of(endTime));
        given(feedTransactionService.createFeed(
                eq(writer), eq(activity), eq(content),
                eq("start.jpg"), eq("end.jpg"), eq(List.of()),
                any(), any()))
                .willReturn(createdFeed);

        // when
        Feed result = feedService.createFeed(writer, activity, content, s3Keys);

        // then
        assertThat(result.getWriter()).isEqualTo(writer);
        assertThat(result.getActivity()).isEqualTo(activity);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getPoints()).isEqualTo(30L);
        assertThat(result.getCalories()).isEqualTo(600);
        verify(feedTransactionService).createFeed(
                eq(writer), eq(activity), eq(content),
                eq("start.jpg"), eq("end.jpg"), eq(List.of()),
                any(), any());
    }

    @Test
    @DisplayName("이미지가 2개 미만이면 예외가 발생한다")
    void createFeedWithoutEnoughImagesThrowsExceptionTest() {
        // given
        String activity = "달리기";
        String content = "오늘 5km 달렸습니다";
        List<String> s3Keys = List.of("only-one.jpg");

        // when & then
        assertThatThrownBy(() -> feedService.createFeed(writer, activity, content, s3Keys))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_IMAGES_REQUIRED);
    }

    @Test
    @DisplayName("시작 사진 시간이 끝 사진 시간보다 늦으면 점수 0으로 피드가 생성된다")
    void createFeedWithInvalidPhotoTimeCreatesWithZeroScoreTest() {
        // given
        String activity = "달리기";
        String content = "오늘 운동했습니다";
        List<String> s3Keys = List.of("start.jpg", "end.jpg");
        // 오늘 날짜 기준으로 테스트
        LocalDateTime startTime = LocalDate.now().atTime(12, 0); // 늦은 시간
        LocalDateTime endTime = LocalDate.now().atTime(10, 0);   // 이른 시간

        Feed savedFeed = Feed.builder()
                .writer(writer)
                .activity(activity)
                .content(content)
                .points(0L)
                .calories(0)
                .build();
        ReflectionTestUtils.setField(savedFeed, "id", 1L);

        given(imageMetadataService.extractPhotoDateTime("start.jpg"))
                .willReturn(Optional.of(startTime));
        given(imageMetadataService.extractPhotoDateTime("end.jpg"))
                .willReturn(Optional.of(endTime));
        given(feedTransactionService.createFeed(
                eq(writer), eq(activity), eq(content),
                eq("start.jpg"), eq("end.jpg"), eq(List.of()),
                any(), any()))
                .willReturn(savedFeed);

        // when
        Feed result = feedService.createFeed(writer, activity, content, s3Keys);

        // then - 예외 대신 점수 0으로 피드 생성
        assertThat(result.getPoints()).isEqualTo(0L);
        assertThat(result.getCalories()).isEqualTo(0);
    }

    @Test
    @DisplayName("사진에 EXIF 시간 정보가 없으면 점수 0으로 피드가 생성된다")
    void createFeedWithNoExifTimeCreatesWithZeroScoreTest() {
        // given
        String activity = "달리기";
        String content = "오늘 운동했습니다";
        List<String> s3Keys = List.of("start.jpg", "end.jpg");

        Feed savedFeed = Feed.builder()
                .writer(writer)
                .activity(activity)
                .content(content)
                .points(0L)
                .calories(0)
                .build();
        ReflectionTestUtils.setField(savedFeed, "id", 1L);

        given(imageMetadataService.extractPhotoDateTime("start.jpg"))
                .willReturn(Optional.empty());
        given(imageMetadataService.extractPhotoDateTime("end.jpg"))
                .willReturn(Optional.empty());
        given(feedTransactionService.createFeed(
                eq(writer), eq(activity), eq(content),
                eq("start.jpg"), eq("end.jpg"), eq(List.of()),
                any(), any()))
                .willReturn(savedFeed);

        // when
        Feed result = feedService.createFeed(writer, activity, content, s3Keys);

        // then
        assertThat(result.getPoints()).isEqualTo(0L);
        assertThat(result.getCalories()).isEqualTo(0);
    }

    @Test
    @DisplayName("운동 시간이 24시간을 초과하면 점수 0으로 피드가 생성된다")
    void createFeedWithTooLongWorkoutCreatesWithZeroScoreTest() {
        // given
        String activity = "달리기";
        String content = "오늘 운동했습니다";
        List<String> s3Keys = List.of("start.jpg", "end.jpg");
        // 오늘 시작해서 모레 끝나는 경우 (48시간)
        LocalDateTime startTime = LocalDate.now().atTime(10, 0);
        LocalDateTime endTime = LocalDate.now().plusDays(2).atTime(10, 0); // 48시간 후

        Feed savedFeed = Feed.builder()
                .writer(writer)
                .activity(activity)
                .content(content)
                .points(0L)
                .calories(0)
                .build();
        ReflectionTestUtils.setField(savedFeed, "id", 1L);

        given(imageMetadataService.extractPhotoDateTime("start.jpg"))
                .willReturn(Optional.of(startTime));
        given(imageMetadataService.extractPhotoDateTime("end.jpg"))
                .willReturn(Optional.of(endTime));
        given(feedTransactionService.createFeed(
                eq(writer), eq(activity), eq(content),
                eq("start.jpg"), eq("end.jpg"), eq(List.of()),
                any(), any()))
                .willReturn(savedFeed);

        // when
        Feed result = feedService.createFeed(writer, activity, content, s3Keys);

        // then - 예외 대신 점수 0으로 피드 생성
        assertThat(result.getPoints()).isEqualTo(0L);
        assertThat(result.getCalories()).isEqualTo(0);
    }

    @Test
    @DisplayName("과거 사진으로 피드를 작성하면 점수 0으로 피드가 생성된다")
    void createFeedWithOldPhotoCreatesWithZeroScoreTest() {
        // given
        String activity = "달리기";
        String content = "오늘 운동했습니다";
        List<String> s3Keys = List.of("start.jpg", "end.jpg");
        // 일주일 전 사진 (±6시간 오차범위 초과)
        LocalDateTime startTime = LocalDate.now().minusDays(7).atTime(10, 0);
        LocalDateTime endTime = LocalDate.now().minusDays(7).atTime(11, 0);

        Feed savedFeed = Feed.builder()
                .writer(writer)
                .activity(activity)
                .content(content)
                .points(0L)
                .calories(0)
                .build();
        ReflectionTestUtils.setField(savedFeed, "id", 1L);

        given(imageMetadataService.extractPhotoDateTime("start.jpg"))
                .willReturn(Optional.of(startTime));
        given(imageMetadataService.extractPhotoDateTime("end.jpg"))
                .willReturn(Optional.of(endTime));
        given(feedTransactionService.createFeed(
                eq(writer), eq(activity), eq(content),
                eq("start.jpg"), eq("end.jpg"), eq(List.of()),
                any(), any()))
                .willReturn(savedFeed);

        // when
        Feed result = feedService.createFeed(writer, activity, content, s3Keys);

        // then - 예외 대신 점수 0으로 피드 생성
        assertThat(result.getPoints()).isEqualTo(0L);
        assertThat(result.getCalories()).isEqualTo(0);
    }

    @Test
    @DisplayName("자정을 넘어서 운동해도 오차범위 내면 허용된다")
    void createFeedWithMidnightWorkoutTest() {
        // given
        String activity = "달리기";
        String content = "자정 넘어서 운동했습니다";
        List<String> s3Keys = List.of("start.jpg", "end.jpg");
        // 어제 23시에 시작해서 오늘 1시에 끝난 운동 (±6시간 오차범위 내)
        LocalDateTime startTime = LocalDate.now().minusDays(1).atTime(23, 0);
        LocalDateTime endTime = LocalDate.now().atTime(1, 0);

        Feed savedFeed = Feed.builder()
                .writer(writer)
                .activity(activity)
                .content(content)
                .points(5L)
                .calories(200)
                .build();
        ReflectionTestUtils.setField(savedFeed, "id", 1L);

        given(imageMetadataService.extractPhotoDateTime("start.jpg"))
                .willReturn(Optional.of(startTime));
        given(imageMetadataService.extractPhotoDateTime("end.jpg"))
                .willReturn(Optional.of(endTime));
        given(feedTransactionService.createFeed(
                eq(writer), eq(activity), eq(content),
                eq("start.jpg"), eq("end.jpg"), eq(List.of()),
                any(), any()))
                .willReturn(savedFeed);

        // when
        Feed result = feedService.createFeed(writer, activity, content, s3Keys);

        // then
        assertThat(result).isNotNull();
        verify(feedTransactionService).createFeed(
                eq(writer), eq(activity), eq(content),
                eq("start.jpg"), eq("end.jpg"), eq(List.of()),
                any(), any());
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
        ReflectionTestUtils.setField(otherUser, "id", 2L);
        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("원래 내용")
                .build();

        given(feedRepository.findByIdWithWriterAndImages(feedId)).willReturn(Optional.of(feed));

        // when & then
        assertThatThrownBy(() -> feedService.updateFeed(otherUser, feedId, "수정 내용", "walking"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_OWNER);
    }

    @Test
    @DisplayName("피드 수정 시 이미지도 변경한다")
    void updateFeedWithImagesTest() {
        // given
        Long feedId = 1L;
        Feed updatedFeed = Feed.builder()
                .writer(writer)
                .activity("walking")
                .content("수정된 내용")
                .points(25L)
                .calories(400)
                .build();
        ReflectionTestUtils.setField(updatedFeed, "id", feedId);

        List<String> newS3Keys = List.of("new-start.jpg", "new-end.jpg");
        String newContent = "수정된 내용";
        String newActivity = "walking";

        // 오늘 날짜 기준으로 테스트
        LocalDateTime newStartTime = LocalDate.now().atTime(14, 0);
        LocalDateTime newEndTime = LocalDate.now().atTime(15, 30); // 1.5시간 운동

        given(imageMetadataService.extractPhotoDateTime("new-start.jpg"))
                .willReturn(Optional.of(newStartTime));
        given(imageMetadataService.extractPhotoDateTime("new-end.jpg"))
                .willReturn(Optional.of(newEndTime));
        given(feedTransactionService.updateFeed(
                eq(writer), eq(feedId), eq(newContent), eq(newActivity),
                eq("new-start.jpg"), eq("new-end.jpg"), eq(List.of()),
                any(), any()))
                .willReturn(updatedFeed);

        // when
        Feed result = feedService.updateFeed(writer, feedId, newContent, newActivity, newS3Keys);

        // then
        assertThat(result.getContent()).isEqualTo(newContent);
        assertThat(result.getActivity()).isEqualTo(newActivity);
        assertThat(result.getPoints()).isEqualTo(25L);
        assertThat(result.getCalories()).isEqualTo(400);
        verify(feedTransactionService).updateFeed(
                eq(writer), eq(feedId), eq(newContent), eq(newActivity),
                eq("new-start.jpg"), eq("new-end.jpg"), eq(List.of()),
                any(), any());
    }

    @Test
    @DisplayName("피드 수정 시 이미지 없이 내용만 변경한다")
    void updateFeedContentOnlyTest() {
        // given
        Long feedId = 1L;
        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("원래 내용")
                .points(30L)
                .build();
        ReflectionTestUtils.setField(feed, "id", feedId);

        String newContent = "수정된 내용";
        String newActivity = "walking";

        given(feedRepository.findByIdWithWriterAndImages(feedId)).willReturn(Optional.of(feed));

        // when - 이미지 없이 호출 (기존 메서드)
        Feed result = feedService.updateFeed(writer, feedId, newContent, newActivity);

        // then
        assertThat(result.getContent()).isEqualTo(newContent);
        assertThat(result.getActivity()).isEqualTo(newActivity);
        assertThat(result.getPoints()).isEqualTo(30L); // 기존 포인트 유지
    }
}
