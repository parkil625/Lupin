package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.command.FeedCreateCommand;
import com.example.demo.dto.command.FeedUpdateCommand;
import com.example.demo.dto.request.FeedRequest;
import com.example.demo.repository.FeedRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @InjectMocks
    private FeedService feedService;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private ImageMetadataService imageMetadataService;

    @Mock
    private FeedWriter feedWriter;

    @Mock
    private FeedDeleteFacade feedDeleteFacade;

    @Mock
    private User user;

    @Mock
    private Feed feed;

    @Test
    @DisplayName("피드 생성 성공")
    void createFeedTest() {
        // given
        FeedRequest request = new FeedRequest("running", "content", "start.jpg", "end.jpg", List.of(), List.of());
        FeedCreateCommand command = FeedCreateCommand.of(user, request);

        given(imageMetadataService.extractPhotoDateTime(any())).willReturn(Optional.of(LocalDateTime.now()));
        given(feedWriter.createFeed(any(), any(), any(), any(), any(), any(), any(), any())).willReturn(feed);

        // when
        feedService.createFeed(command);

        // then
        verify(feedWriter).createFeed(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("피드 수정 성공")
    void updateFeedTest() {
        // given
        Long feedId = 1L;
        FeedRequest request = new FeedRequest("swimming", "new content", "start.jpg", "end.jpg", List.of(), List.of());
        FeedUpdateCommand command = FeedUpdateCommand.of(user, feedId, request);

        given(imageMetadataService.extractPhotoDateTime(any())).willReturn(Optional.of(LocalDateTime.now()));
        given(feedWriter.updateFeed(any(), any(), any(), any(), any(), any(), any(), any(), any())).willReturn(feed);

        // when
        feedService.updateFeed(command);

        // then
        verify(feedWriter).updateFeed(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("피드 삭제 성공")
    void deleteFeedTest() {
        // given
        Long feedId = 1L;

        // when
        feedService.deleteFeed(user, feedId);

        // then
        verify(feedDeleteFacade).deleteFeed(user, feedId);
    }
}
