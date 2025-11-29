package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedReport;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedReportRepository;
import com.example.demo.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedReportService {

    private final FeedReportRepository feedReportRepository;
    private final FeedRepository feedRepository;

    @Transactional
    public void toggleReport(User reporter, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        if (feedReportRepository.existsByReporterAndFeed(reporter, feed)) {
            feedReportRepository.deleteByReporterAndFeed(reporter, feed);
        } else {
            FeedReport feedReport = FeedReport.builder()
                    .reporter(reporter)
                    .feed(feed)
                    .build();
            feedReportRepository.save(feedReport);
        }
    }
}
