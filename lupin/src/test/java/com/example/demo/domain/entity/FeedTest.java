package com.example.demo.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedTest {

    @Test
    @DisplayName("피드 내용을 수정한다")
    void updateTest() {
        // given
        Feed feed = Feed.builder()
                .content("원래 내용")
                .activity("running")
                .build();

        // when
        feed.update("수정된 내용", "walking");

        // then
        assertThat(feed.getContent()).isEqualTo("수정된 내용");
        assertThat(feed.getActivity()).isEqualTo("walking");
    }

    @Test
    @DisplayName("피드 수정 시 updatedAt이 갱신된다")
    void updateSetsUpdatedAtTest() {
        // given
        Feed feed = Feed.builder()
                .content("원래 내용")
                .activity("running")
                .build();

        // when
        feed.update("수정된 내용", "walking");

        // then
        assertThat(feed.getUpdatedAt()).isNotNull();
    }
}
