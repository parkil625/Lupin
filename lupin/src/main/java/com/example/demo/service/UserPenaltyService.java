package com.example.demo.service;

import com.example.demo.config.properties.PenaltyProperties;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.UserPenalty;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.repository.UserPenaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UserPenaltyService {

    private final UserPenaltyRepository userPenaltyRepository;
    private final PenaltyProperties penaltyProperties;

    public UserPenalty addPenalty(User user, PenaltyType penaltyType) {
        UserPenalty penalty = UserPenalty.builder()
                .user(user)
                .penaltyType(penaltyType)
                .build();

        return userPenaltyRepository.save(penalty);
    }

    public boolean hasActivePenalty(User user, PenaltyType penaltyType) {
        LocalDateTime since = LocalDateTime.now().minusDays(penaltyProperties.getDurationDays());
        // [수정] User 객체 대신 UserId로 조회
        return userPenaltyRepository.existsByUserIdAndPenaltyTypeAndCreatedAtAfter(user.getId(), penaltyType, since);
    }

    public boolean shouldApplyPenalty(long likeCount, long reportCount) {
        long effectiveLikeCount = Math.max(likeCount, 1);
        return reportCount >= effectiveLikeCount * penaltyProperties.getThresholdMultiplier();
    }
}
