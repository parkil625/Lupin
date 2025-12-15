package com.example.demo.event;

import com.example.demo.config.properties.FeedProperties;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.PointManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 포인트 관련 이벤트 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointEventListener {

    private final UserRepository userRepository;
    private final PointManager pointManager;
    private final FeedProperties feedProperties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * FeedDeletedEvent 처리 - 피드 삭제 시 포인트 회수
     */
    @Async
    @EventListener
    @Transactional
    public void handleFeedDeletedEvent(FeedDeletedEvent event) {
        if (event.feedPoints() <= 0) {
            return;
        }

        int recoveryDays = feedProperties.getPointRecoveryDays();
        LocalDateTime deadline = LocalDateTime.now().minusDays(recoveryDays);
        if (event.feedCreatedAt().isBefore(deadline)) {
            log.info("Point recovery period expired for feed: {}", event.feedId());
            return;
        }

        try {
            User writer = userRepository.findById(event.writerId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            pointManager.cancelPoints(writer, event.feedPoints());
            eventPublisher.publishEvent(PointChangedEvent.deduct(writer.getId(), event.feedPoints()));

            log.info("Points recovered for deleted feed: feedId={}, writerId={}, points={}",
                    event.feedId(), event.writerId(), event.feedPoints());

        } catch (Exception e) {
            log.error("Failed to recover points for deleted feed: {}", event, e);
        }
    }
}
