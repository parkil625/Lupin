package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ImageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeedImageRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private FeedImageRepository feedImageRepository;

    @Test
    @DisplayName("피드의 이미지를 순서대로 조회한다")
    void findByFeedOrderBySortOrderAscTest() {
        // given
        User user = createAndSaveUser("user1");
        Feed feed = createAndSaveFeed(user, "running");

        createAndSaveFeedImage(feed, "image3.jpg", 2);
        createAndSaveFeedImage(feed, "image1.jpg", 0);
        createAndSaveFeedImage(feed, "image2.jpg", 1);

        // when
        List<FeedImage> images = feedImageRepository.findByFeedOrderBySortOrderAsc(feed);

        // then
        assertThat(images).hasSize(3);
        assertThat(images.get(0).getS3Key()).isEqualTo("image1.jpg");
        assertThat(images.get(1).getS3Key()).isEqualTo("image2.jpg");
        assertThat(images.get(2).getS3Key()).isEqualTo("image3.jpg");
    }

    @Test
    @DisplayName("피드의 이미지를 전체 삭제한다")
    void deleteByFeedTest() {
        // given
        User user = createAndSaveUser("user1");
        Feed feed = createAndSaveFeed(user, "running");

        createAndSaveFeedImage(feed, "image1.jpg", 0);
        createAndSaveFeedImage(feed, "image2.jpg", 1);

        // when
        feedImageRepository.deleteByFeed(feed);

        // then
        List<FeedImage> images = feedImageRepository.findByFeedOrderBySortOrderAsc(feed);
        assertThat(images).isEmpty();
    }

    private FeedImage createAndSaveFeedImage(Feed feed, String s3Key, int sortOrder) {
        FeedImage image = FeedImage.builder()
                .feed(feed)
                .s3Key(s3Key)
                .imgType(ImageType.OTHER)
                .sortOrder(sortOrder)
                .build();
        return feedImageRepository.save(image);
    }
}
