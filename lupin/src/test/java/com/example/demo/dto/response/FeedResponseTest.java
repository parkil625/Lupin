package com.example.demo.dto.response;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ImageType;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeedResponse 테스트")
class FeedResponseTest {

    @Test
    @DisplayName("FeedResponse.from()은 writer의 avatar를 writerAvatar로 반환한다")
    void fromShouldIncludeWriterAvatar() {
        // given
        String expectedAvatarUrl = "https://lupin-storage.s3.ap-northeast-2.amazonaws.com/avatars/test-avatar.jpg";

        User writer = User.builder()
                .userId("testUser")
                .password("password")
                .name("테스트 유저")
                .role(Role.MEMBER)
                .avatar(expectedAvatarUrl)
                .build();
        ReflectionTestUtils.setField(writer, "id", 1L);

        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("테스트 피드 내용")
                .points(100L)
                .calories(500)
                .build();
        ReflectionTestUtils.setField(feed, "id", 1L);

        // 이미지 리스트 초기화 (빈 리스트로 설정하여 NPE 방지)
        ReflectionTestUtils.setField(feed, "images", new ArrayList<FeedImage>());

        // when
        FeedResponse response = FeedResponse.from(feed, 10, 5);

        // then
        assertThat(response.getWriterAvatar()).isNotNull();
        assertThat(response.getWriterAvatar()).isEqualTo(expectedAvatarUrl);
        assertThat(response.getWriterName()).isEqualTo("테스트 유저");
        assertThat(response.getWriterId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("writer에 avatar가 없으면 writerAvatar는 null이다")
    void fromShouldReturnNullWhenWriterHasNoAvatar() {
        // given
        User writer = User.builder()
                .userId("testUser")
                .password("password")
                .name("테스트 유저")
                .role(Role.MEMBER)
                // avatar 설정하지 않음
                .build();
        ReflectionTestUtils.setField(writer, "id", 1L);

        Feed feed = Feed.builder()
                .writer(writer)
                .activity("running")
                .content("테스트 피드 내용")
                .points(100L)
                .calories(500)
                .build();
        ReflectionTestUtils.setField(feed, "id", 1L);
        ReflectionTestUtils.setField(feed, "images", new ArrayList<FeedImage>());

        // when
        FeedResponse response = FeedResponse.from(feed, 10, 5);

        // then
        assertThat(response.getWriterAvatar()).isNull();
    }

    @Test
    @DisplayName("FeedResponse에 모든 필드가 올바르게 매핑된다")
    void fromShouldMapAllFieldsCorrectly() {
        // given
        String avatarUrl = "https://example.com/avatar.jpg";

        User writer = User.builder()
                .userId("testUser")
                .password("password")
                .name("홍길동")
                .role(Role.MEMBER)
                .avatar(avatarUrl)
                .build();
        ReflectionTestUtils.setField(writer, "id", 42L);

        List<FeedImage> images = new ArrayList<>();
        FeedImage image1 = FeedImage.builder()
                .s3Key("images/start.jpg")
                .imgType(ImageType.START)
                .sortOrder(0)
                .build();
        FeedImage image2 = FeedImage.builder()
                .s3Key("images/end.jpg")
                .imgType(ImageType.END)
                .sortOrder(1)
                .build();
        images.add(image1);
        images.add(image2);

        Feed feed = Feed.builder()
                .writer(writer)
                .activity("swimming")
                .content("오늘 수영했습니다")
                .points(50L)
                .calories(300)
                .build();
        ReflectionTestUtils.setField(feed, "id", 100L);
        ReflectionTestUtils.setField(feed, "images", images);

        // when
        FeedResponse response = FeedResponse.from(feed, 25, 10);

        // then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getWriterId()).isEqualTo(42L);
        assertThat(response.getWriterName()).isEqualTo("홍길동");
        assertThat(response.getWriterAvatar()).isEqualTo(avatarUrl);
        assertThat(response.getActivity()).isEqualTo("swimming");
        assertThat(response.getContent()).isEqualTo("오늘 수영했습니다");
        assertThat(response.getPoints()).isEqualTo(50L);
        assertThat(response.getCalories()).isEqualTo(300);
        assertThat(response.getLikes()).isEqualTo(25L);
        assertThat(response.getComments()).isEqualTo(10L);
        assertThat(response.getImages()).hasSize(2);
        assertThat(response.getImages()).containsExactly("images/start.jpg", "images/end.jpg");
    }
}
