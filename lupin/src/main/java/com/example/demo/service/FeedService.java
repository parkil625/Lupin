package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.command.FeedCreateCommand;
import com.example.demo.dto.command.FeedUpdateCommand;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeedService {

    private final ImageMetadataService imageMetadataService;
    private final FeedWriter feedWriter;
    private final FeedDeleteFacade feedDeleteFacade;

    /**
     * 피드 생성 (Public API - Command 패턴)
     */
    public Feed createFeed(FeedCreateCommand command) {
        return createFeedInternal(
                command.writer(),
                command.activity(),
                command.content(),
                command.startImageKey(),
                command.endImageKey(),
                command.otherImageKeys()
        );
    }

    /**
     * 피드 수정 (Public API - Command 패턴)
     */
    public Feed updateFeed(FeedUpdateCommand command) {
        return updateFeedInternal(
                command.user(),
                command.feedId(),
                command.content(),
                command.activity(),
                command.startImageKey(),
                command.endImageKey(),
                command.otherImageKeys()
        );
    }

    /**
     * 피드 삭제 - FeedDeleteFacade에 위임
     */
    public void deleteFeed(User user, Long feedId) {
        feedDeleteFacade.deleteFeed(user, feedId);
    }

    // ===================== PRIVATE METHODS =====================

    private Feed createFeedInternal(User writer, String activity, String content, String startImageKey, String endImageKey, List<String> otherImageKeys) {
        if (startImageKey == null || endImageKey == null) {
            throw new BusinessException(ErrorCode.FEED_IMAGES_REQUIRED);
        }

        Optional<LocalDateTime> startTimeOpt = imageMetadataService.extractPhotoDateTime(startImageKey);
        Optional<LocalDateTime> endTimeOpt = imageMetadataService.extractPhotoDateTime(endImageKey);

        log.info("EXIF extraction completed: startImage={}, endImage={}",
                startTimeOpt.isPresent() ? "OK" : "NOT_FOUND",
                endTimeOpt.isPresent() ? "OK" : "NOT_FOUND");

        return feedWriter.createFeed(writer, activity, content, startImageKey, endImageKey, otherImageKeys, startTimeOpt, endTimeOpt);
    }

    private Feed updateFeedInternal(User user, Long feedId, String content, String activity,
                                    String startImageKey, String endImageKey, List<String> otherImageKeys) {
        if (startImageKey != null && endImageKey != null) {
            return updateFeedWithImages(user, feedId, content, activity, startImageKey, endImageKey, otherImageKeys);
        }
        return feedWriter.updateFeedContentOnly(user, feedId, content, activity);
    }

    private Feed updateFeedWithImages(User user, Long feedId, String content, String activity,
                                      String startImageKey, String endImageKey, List<String> otherImageKeys) {
        Optional<LocalDateTime> startTimeOpt = imageMetadataService.extractPhotoDateTime(startImageKey);
        Optional<LocalDateTime> endTimeOpt = imageMetadataService.extractPhotoDateTime(endImageKey);

        log.info("Feed update - EXIF extraction completed: startImage={}, endImage={}",
                startTimeOpt.isPresent() ? "OK" : "NOT_FOUND",
                endTimeOpt.isPresent() ? "OK" : "NOT_FOUND");

        return feedWriter.updateFeed(user, feedId, content, activity, startImageKey, endImageKey, otherImageKeys, startTimeOpt, endTimeOpt);
    }
}
