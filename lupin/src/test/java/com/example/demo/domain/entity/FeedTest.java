package com.example.demo.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Feed 엔티티 테스트")
class FeedTest {

    @Test
    @DisplayName("피드 생성 - 기본값 확인")
    void create_DefaultValues() {
        // given & when
        Feed feed = Feed.builder()
                .id(1L)
                .writerId(1L)
                .activityType("RUNNING")
                .content("테스트")
                .build();

        // then
        assertThat(feed.getEarnedPoints()).isEqualTo(0L);
        assertThat(feed.getLikesCount()).isEqualTo(0);
        assertThat(feed.getCommentsCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("피드 내용 수정")
    void update_Success() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .writerId(1L)
                .activityType("RUNNING")
                .content("오늘 조깅 완료!")
                .build();

        // when
        feed.update("오늘 10km 조깅 완료!");

        // then
        assertThat(feed.getContent()).isEqualTo("오늘 10km 조깅 완료!");
    }

    @Test
    @DisplayName("피드 내용 수정 - null 무시")
    void update_NullIgnored() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .writerId(1L)
                .activityType("RUNNING")
                .content("오늘 조깅 완료!")
                .build();

        // when
        feed.update(null);

        // then
        assertThat(feed.getContent()).isEqualTo("오늘 조깅 완료!");
    }

    @Test
    @DisplayName("획득 포인트 설정")
    void setEarnedPoints_Success() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .writerId(1L)
                .activityType("RUNNING")
                .content("오늘 조깅 완료!")
                .build();

        // when
        feed.setEarnedPoints(10L);

        // then
        assertThat(feed.getEarnedPoints()).isEqualTo(10L);
    }

    @Test
    @DisplayName("좋아요 수 증가")
    void incrementLikesCount_Success() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .writerId(1L)
                .activityType("RUNNING")
                .content("테스트")
                .build();

        // when
        feed.incrementLikesCount();

        // then
        assertThat(feed.getLikesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("좋아요 수 감소")
    void decrementLikesCount_Success() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .writerId(1L)
                .activityType("RUNNING")
                .content("테스트")
                .likesCount(5)
                .build();

        // when
        feed.decrementLikesCount();

        // then
        assertThat(feed.getLikesCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("좋아요 수 감소 - 0 미만으로 내려가지 않음")
    void decrementLikesCount_NotBelowZero() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .writerId(1L)
                .activityType("RUNNING")
                .content("테스트")
                .likesCount(0)
                .build();

        // when
        feed.decrementLikesCount();

        // then
        assertThat(feed.getLikesCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("댓글 수 증가")
    void incrementCommentsCount_Success() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .writerId(1L)
                .activityType("RUNNING")
                .content("테스트")
                .build();

        // when
        feed.incrementCommentsCount();

        // then
        assertThat(feed.getCommentsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("댓글 수 감소")
    void decrementCommentsCount_Success() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .writerId(1L)
                .activityType("RUNNING")
                .content("테스트")
                .commentsCount(3)
                .build();

        // when
        feed.decrementCommentsCount();

        // then
        assertThat(feed.getCommentsCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("댓글 수 감소 - 0 미만으로 내려가지 않음")
    void decrementCommentsCount_NotBelowZero() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .writerId(1L)
                .activityType("RUNNING")
                .content("테스트")
                .commentsCount(0)
                .build();

        // when
        feed.decrementCommentsCount();

        // then
        assertThat(feed.getCommentsCount()).isEqualTo(0);
    }
}
