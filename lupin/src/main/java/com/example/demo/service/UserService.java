package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.PointLogRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PointLogRepository pointLogRepository;

    public User getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void updateProfile(User user, String name, Double height, Double weight) {
        user.updateProfile(name, height, weight);
    }

    public List<Map<String, Object>> getTopUsersByPoints(int limit) {
        List<Object[]> results = pointLogRepository.findUsersRankedByPoints(PageRequest.of(0, limit));
        List<Map<String, Object>> rankings = new ArrayList<>();
        int rank = 1;
        for (Object[] row : results) {
            User user = (User) row[0];
            Long points = (Long) row[1];
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", user.getId());
            entry.put("name", user.getName());
            entry.put("avatar", user.getAvatar());
            entry.put("department", user.getDepartment());
            entry.put("points", points);
            entry.put("rank", rank++);
            rankings.add(entry);
        }
        return rankings;
    }

    public List<Map<String, Object>> getUserRankingContext(Long userId) {
        List<Object[]> allRankings = pointLogRepository.findAllUsersRankedByPoints();
        int userRank = -1;
        for (int i = 0; i < allRankings.size(); i++) {
            User u = (User) allRankings.get(i)[0];
            if (u.getId().equals(userId)) {
                userRank = i;
                break;
            }
        }

        List<Map<String, Object>> context = new ArrayList<>();
        if (userRank == -1) {
            User user = getUserInfo(userId);
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", user.getId());
            entry.put("name", user.getName());
            entry.put("avatar", user.getAvatar());
            entry.put("department", user.getDepartment());
            entry.put("points", 0L);
            entry.put("rank", allRankings.size() + 1);
            context.add(entry);
            return context;
        }

        int start = Math.max(0, userRank - 1);
        int end = Math.min(allRankings.size(), userRank + 2);

        for (int i = start; i < end; i++) {
            Object[] row = allRankings.get(i);
            User user = (User) row[0];
            Long points = (Long) row[1];
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", user.getId());
            entry.put("name", user.getName());
            entry.put("avatar", user.getAvatar());
            entry.put("department", user.getDepartment());
            entry.put("points", points);
            entry.put("rank", i + 1);
            context.add(entry);
        }
        return context;
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }
}
