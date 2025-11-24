package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ImageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FeedImage 엔티티 테스트")
class FeedImageTest {

    @Test
    @DisplayName("피드 이미지 생성")
    void create_Success() {
        // given & when
        FeedImage feedImage = FeedImage.builder()
                .id(1L)
                .s3Key("images/test.jpg")
                .imgType(ImageType.START)
                .build();

        // then
        assertThat(feedImage.getId()).isEqualTo(1L);
        assertThat(feedImage.getS3Key()).isEqualTo("images/test.jpg");
        assertThat(feedImage.getImgType()).isEqualTo(ImageType.START);
    }

    @Test
    @DisplayName("이미지 URL 조회")
    void getImageUrl_Success() {
        // given
        FeedImage feedImage = FeedImage.builder()
                .id(1L)
                .s3Key("images/test.jpg")
                .imgType(ImageType.END)
                .build();

        // when & then
        assertThat(feedImage.getImageUrl()).isEqualTo("images/test.jpg");
    }

    @Test
    @DisplayName("피드 설정")
    void setFeed_Success() {
        // given
        Feed feed = Feed.builder()
                .id(1L)
                .activityType("RUNNING")
                .content("테스트")
                .build();

        FeedImage feedImage = FeedImage.builder()
                .id(1L)
                .s3Key("images/test.jpg")
                .imgType(ImageType.START)
                .build();

        // when
        feedImage.setFeed(feed);

        // then
        assertThat(feedImage.getFeed()).isEqualTo(feed);
    }

    @Test
    @DisplayName("START 타입 이미지 생성")
    void createStartImage_Success() {
        // given & when
        FeedImage feedImage = FeedImage.builder()
                .id(1L)
                .s3Key("images/start.jpg")
                .imgType(ImageType.START)
                .build();

        // then
        assertThat(feedImage.getImgType()).isEqualTo(ImageType.START);
    }

    @Test
    @DisplayName("END 타입 이미지 생성")
    void createEndImage_Success() {
        // given & when
        FeedImage feedImage = FeedImage.builder()
                .id(2L)
                .s3Key("images/end.jpg")
                .imgType(ImageType.END)
                .build();

        // then
        assertThat(feedImage.getImgType()).isEqualTo(ImageType.END);
    }
}
