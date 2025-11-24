package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Feed 엔티티 테스트")
class FeedTest {

    @Test
    @DisplayName("피드 작성자 설정")
    void setWriter_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("테스트")
                .role(Role.MEMBER)
                .build();

        Feed feed = Feed.builder()
                .id(1L)
                .activityType("RUNNING")
                .content("오늘 조깅 완료!")
                .build();

        // when
        feed.setWriter(user);

        // then
        assertThat(feed.getWriter()).isEqualTo(user);
        assertThat(user.getFeeds()).contains(feed);
    }

    @Test
    @DisplayName("피드에 이미지 추가")
    void addImage_Success() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .activityType("RUNNING")
                .content("오늘 조깅 완료!")
                .build();

        FeedImage image = FeedImage.builder()
                .id(1L)
                .s3Key("images/test.jpg")
                .imgType(com.example.demo.domain.enums.ImageType.START)
                .build();

        // when
        feed.addImage(image);

        // then
        assertThat(feed.getImages()).hasSize(1);
        assertThat(feed.getImages().get(0)).isEqualTo(image);
    }

    @Test
    @DisplayName("좋아요 수 조회")
    void getLikesCount_Success() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .activityType("RUNNING")
                .content("오늘 조깅 완료!")
                .build();

        // when & then
        assertThat(feed.getLikesCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("댓글 수 조회")
    void getCommentsCount_Success() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .activityType("RUNNING")
                .content("오늘 조깅 완료!")
                .build();

        // when & then
        assertThat(feed.getCommentsCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("피드 내용 수정")
    void update_Success() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
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
                .activityType("RUNNING")
                .content("오늘 조깅 완료!")
                .build();

        // when
        feed.setEarnedPoints(10L);

        // then
        assertThat(feed.getEarnedPoints()).isEqualTo(10L);
    }

    @Test
    @DisplayName("피드 생성 - 기본값 확인")
    void create_DefaultValues() {
        // given & when
        Feed feed = Feed.builder()
                .id(1L)
                .activityType("RUNNING")
                .content("테스트")
                .build();

        // then
        assertThat(feed.getEarnedPoints()).isEqualTo(0L);
        assertThat(feed.getImages()).isEmpty();
        assertThat(feed.getComments()).isEmpty();
        assertThat(feed.getLikes()).isEmpty();
    }

    @Test
    @DisplayName("피드에 댓글 추가")
    void addComment_Success() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .activityType("RUNNING")
                .content("오늘 조깅 완료!")
                .build();

        Comment comment = Comment.builder()
                .id(1L)
                .content("좋은 운동이네요!")
                .build();

        // when
        feed.addComment(comment);

        // then
        assertThat(feed.getComments()).hasSize(1);
        assertThat(feed.getCommentsCount()).isEqualTo(1);
        assertThat(comment.getFeed()).isEqualTo(feed);
    }

    @Test
    @DisplayName("피드에 여러 이미지 추가")
    void addMultipleImages_Success() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .activityType("RUNNING")
                .content("오늘 조깅 완료!")
                .build();

        FeedImage image1 = FeedImage.builder()
                .id(1L)
                .s3Key("images/test1.jpg")
                .imgType(com.example.demo.domain.enums.ImageType.START)
                .build();

        FeedImage image2 = FeedImage.builder()
                .id(2L)
                .s3Key("images/test2.jpg")
                .imgType(com.example.demo.domain.enums.ImageType.END)
                .build();

        // when
        feed.addImage(image1);
        feed.addImage(image2);

        // then
        assertThat(feed.getImages()).hasSize(2);
    }
}
