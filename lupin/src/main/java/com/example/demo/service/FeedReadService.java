package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedReadService {

    private final FeedRepository feedRepository;

    public Slice<Feed> getHomeFeeds(User user, int page, int size) {
        return feedRepository.findAllExceptWriter(user.getId(), PageRequest.of(page, size));
    }

    public Slice<Feed> getMyFeeds(User user, int page, int size) {
        return feedRepository.findAllByWriter(user.getId(), PageRequest.of(page, size));
    }

    public Feed getFeedDetail(Long feedId) {
        return feedRepository.findByIdWithWriterAndImages(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
    }

    public boolean canPostToday(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        return !feedRepository.existsByWriterAndDate(user.getId(), startOfDay, endOfDay);
    }
}
