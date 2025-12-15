package com.example.demo.service;

import com.example.demo.dto.WriterActiveDays;
import com.example.demo.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReadService {

    private final FeedRepository feedRepository;

    public Map<Long, Integer> getActiveDaysMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        List<WriterActiveDays> results = feedRepository.findActiveDaysByWriterIds(userIds, startOfMonth, endOfMonth);

        Map<Long, Integer> activeDaysMap = new HashMap<>();
        for (WriterActiveDays row : results) {
            activeDaysMap.put(row.writerId(), row.activeDays().intValue());
        }
        return activeDaysMap;
    }
}
