package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.FeedImageUploadResult;
import com.example.demo.dto.WriterActiveDays;
import com.example.demo.dto.command.FeedCreateCommand;
import com.example.demo.dto.command.FeedUpdateCommand;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserPenaltyRepository; // [추가]
import com.example.demo.domain.enums.PenaltyType; // [추가]
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FeedRepository feedRepository;
    private final ImageMetadataService imageMetadataService;
    private final FeedTransactionService feedTransactionService;
    private final FeedDeleteFacade feedDeleteFacade;
    private final UserPenaltyRepository userPenaltyRepository; // [추가] Repository 주입

    public Slice<Feed> getHomeFeeds(User user, int page, int size) {
        // @BatchSize(100)로 images 지연 로딩 최적화 (N+1 방지)
        // user.getId()만 사용하여 detached entity (@Version null) 문제 방지
        return feedRepository.findByWriterIdNotOrderByIdDesc(user.getId(), PageRequest.of(page, size));
    }

    public Slice<Feed> getMyFeeds(User user, int page, int size) {
        // @BatchSize(100)로 images 지연 로딩 최적화 (N+1 방지)
        // user.getId()만 사용하여 detached entity (@Version null) 문제 방지
        return feedRepository.findByWriterIdOrderByIdDesc(user.getId(), PageRequest.of(page, size));
    }

    @Transactional
    public Feed createFeed(User writer, String activity, String content) {
        return createFeed(writer, activity, content, List.of());
    }

    @Transactional
    public Feed createFeed(User writer, String activity, String content, List<String> s3Keys) {
        return createFeed(writer, activity, content, FeedImageUploadResult.fromList(s3Keys));
    }

    /**
     * 피드 생성 (타입 안전한 이미지 파라미터)
     */
    public Feed createFeed(User writer, String activity, String content, FeedImageUploadResult images) {
        return createFeed(writer, activity, content, images.startImageKey(), images.endImageKey(), images.otherImageKeys());
    }

    /**
     * 피드 생성 (Parameter Object 패턴)
     */
    public Feed createFeed(FeedCreateCommand command) {
        return createFeed(
                command.writer(),
                command.activity(),
                command.content(),
                command.startImageKey(),
                command.endImageKey(),
                command.otherImageKeys()
        );
    }

    /**
     * 피드 생성 (S3 I/O를 트랜잭션 외부에서 수행)
     */
    public Feed createFeed(User writer, String activity, String content, String startImageKey, String endImageKey, List<String> otherImageKeys) {
        // [수정] 피드 작성 금지 패널티 확인 (최근 3일 이내 기록 존재 여부)
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        // [수정] User 객체 대신 ID로 조회 (확실한 조회 보장)
        if (userPenaltyRepository.existsByUserIdAndPenaltyTypeAndCreatedAtAfter(writer.getId(), PenaltyType.FEED, threeDaysAgo)) {
             throw new BusinessException(ErrorCode.FEED_CREATION_RESTRICTED);
        }

        if (startImageKey == null || endImageKey == null) {
            throw new BusinessException(ErrorCode.FEED_IMAGES_REQUIRED);
        }

        // [트랜잭션 외부] S3에서 EXIF 시간 추출 (네트워크 I/O)
        Optional<LocalDateTime> startTimeOpt = imageMetadataService.extractPhotoDateTime(startImageKey);
        Optional<LocalDateTime> endTimeOpt = imageMetadataService.extractPhotoDateTime(endImageKey);

        log.info("EXIF extraction completed: startImage={}, endImage={}",
                startTimeOpt.isPresent() ? "OK" : "NOT_FOUND",
                endTimeOpt.isPresent() ? "OK" : "NOT_FOUND");

        // [트랜잭션 내부] 별도 서비스로 분리하여 트랜잭션 적용
        return feedTransactionService.createFeed(writer, activity, content, startImageKey, endImageKey, otherImageKeys, startTimeOpt, endTimeOpt);
    }

    /**
     * 피드 수정 - 내용만 수정 (이미지 변경 없음)
     */
    @Transactional
    public Feed updateFeed(User user, Long feedId, String content, String activity) {
        return updateFeedContentOnly(user, feedId, content, activity);
    }

    @Transactional
    private Feed updateFeedContentOnly(User user, Long feedId, String content, String activity) {
        Feed feed = feedRepository.findByIdWithWriterAndImages(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        feed.validateOwner(user);
        feed.update(content, activity);
        return feed;
    }

    @Transactional
    public Feed updateFeed(User user, Long feedId, String content, String activity, List<String> s3Keys) {
        // [수정] 중간 오버로딩 메서드를 거치지 않고, 직접 변환하여 최종 메서드 호출 (컴파일 에러 방지)
        FeedImageUploadResult images = FeedImageUploadResult.fromList(s3Keys);
        return updateFeed(user, feedId, content, activity, 
                images.startImageKey(), images.endImageKey(), images.otherImageKeys());
    }

    /**
     * 피드 수정 (Parameter Object 패턴)
     */
    public Feed updateFeed(FeedUpdateCommand command) {
        // [수정] imagesChanged 및 시간 값을 전달하며 통합 메서드 호출
        return updateFeed(
                command.user(),
                command.feedId(),
                command.content(),
                command.activity(),
                command.startImageKey(),
                command.endImageKey(),
                command.otherImageKeys(),
                command.imagesChanged(),
                command.startAt(), // [추가]
                command.endAt()    // [추가]
        );
    }

    /**
     * 피드 수정 통합 메서드 (이미지 키가 null이면 내용만 수정)
     * [수정] boolean imagesChanged 파라미터 추가
     */
    public Feed updateFeed(User user, Long feedId, String content, String activity,
                           String startImageKey, String endImageKey, List<String> otherImageKeys,
                           boolean imagesChanged, LocalDateTime startAt, LocalDateTime endAt) {
        if (startImageKey != null && endImageKey != null) {
            return updateFeedWithImages(user, feedId, content, activity,
                    startImageKey, endImageKey, otherImageKeys, imagesChanged, startAt, endAt);
        }
        return updateFeedContentOnly(user, feedId, content, activity);
    }

    // 하위 호환용 (테스트 등)
    public Feed updateFeed(User user, Long feedId, String content, String activity,
                           String startImageKey, String endImageKey, List<String> otherImageKeys) {
        return updateFeed(user, feedId, content, activity, startImageKey, endImageKey, otherImageKeys, true, null, null);
    }

    private Feed updateFeedWithImages(User user, Long feedId, String content, String activity,
                                      String startImageKey, String endImageKey, List<String> otherImageKeys,
                                      boolean imagesChanged, LocalDateTime startAt, LocalDateTime endAt) {

        // [추가] 디버깅 로그: 실제 로직 분기 확인
        log.info(">>> [Feed Service] UpdateWithImages - ID: {}, ImagesChanged: {}, HasTimeInfo: {}", 
                feedId, imagesChanged, (startAt != null && endAt != null));

        Optional<LocalDateTime> startTimeOpt = Optional.empty();
        Optional<LocalDateTime> endTimeOpt = Optional.empty();

        if (imagesChanged) {
            // [핵심] 프론트에서 넘어온 시간이 있으면 최우선 사용 (압축본 S3 추출 안 함)
            if (startAt != null && endAt != null) {
                startTimeOpt = Optional.of(startAt);
                endTimeOpt = Optional.of(endAt);
                log.info("Using provided time from frontend: {} ~ {}", startAt, endAt);
            } else {
                // 프론트 시간이 없으면 기존 방식대로 S3 추출 시도
                startTimeOpt = imageMetadataService.extractPhotoDateTime(startImageKey);
                endTimeOpt = imageMetadataService.extractPhotoDateTime(endImageKey);
            }
        }

        return feedTransactionService.updateFeed(
                user, feedId, content, activity,
                startImageKey, endImageKey, otherImageKeys,
                startTimeOpt, endTimeOpt,
                imagesChanged
        );
    }

    /**
     * 피드 삭제 - FeedDeleteFacade에 위임
     */
    public void deleteFeed(User user, Long feedId) {
        feedDeleteFacade.deleteFeed(user, feedId);
    }

    public Feed getFeedDetail(Long feedId) {
        return feedRepository.findByIdWithWriterAndImages(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
    }

    public boolean canPostToday(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        // user.getId()만 사용하여 detached entity (@Version null) 문제 방지
        return !feedRepository.existsByWriter_IdAndCreatedAtBetween(user.getId(), startOfDay, endOfDay);
    }

    /**
     * 피드 목록에서 작성자별 이번 달 활동일수를 배치로 조회
     * QueryDSL을 통해 타입 안전한 WriterActiveDays DTO로 조회
     */
    public Map<Long, Integer> getActiveDaysMap(List<Feed> feeds) {
        if (feeds.isEmpty()) {
            return Map.of();
        }

        List<Long> writerIds = feeds.stream()
                .map(feed -> feed.getWriter().getId())
                .distinct()
                .toList();

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        List<WriterActiveDays> results = feedRepository.findActiveDaysByWriterIds(writerIds, startOfMonth, endOfMonth);

        Map<Long, Integer> activeDaysMap = new HashMap<>();
        for (WriterActiveDays row : results) {
            activeDaysMap.put(row.writerId(), row.activeDays().intValue());
        }
        return activeDaysMap;
    }
}
