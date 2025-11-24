package com.example.demo.dto.response;

import com.example.demo.domain.entity.ChallengeEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeJoinResponse {
    private boolean success;
    private String message;
    private int currentJoinCount;
    private Long challengeId;
    private Long userId;
    private LocalDateTime joinedAt;
    private boolean joined;

    public static ChallengeJoinResponse from(ChallengeEntry entry) {
        return ChallengeJoinResponse.builder()
                .challengeId(entry.getChallenge().getId())
                .userId(entry.getUser().getId())
                .joinedAt(entry.getJoinedAt())
                .joined(true)
                .message("참여한 사용자입니다.")
                .build();
    }

    public static ChallengeJoinResponse notJoined() {
        return ChallengeJoinResponse.builder()
                .joined(false)
                .message("아직 이 이벤트에 참여하지 않았습니다.")
                .build();
    }
}
