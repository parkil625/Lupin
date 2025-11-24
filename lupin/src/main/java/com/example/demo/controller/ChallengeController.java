package com.example.demo.controller;

import com.example.demo.domain.entity.Challenge;
import com.example.demo.domain.entity.ChallengeEntry;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.response.ChallengeJoinResponse;
import com.example.demo.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 챌린지 관련 API
 */
@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    /**
     * 활성화된 챌린지 목록 조회
     */
    @GetMapping("/active")
    public ResponseEntity<List<Challenge>> getActiveChallenges() {
        List<Challenge> challenges = challengeService.getActiveChallenges();
        return ResponseEntity.ok(challenges);
    }

    /**
     * 챌린지 상세 조회
     */
    @GetMapping("/{challengeId}")
    public ResponseEntity<Challenge> getChallengeDetail(@PathVariable Long challengeId) {
        Challenge challenge = challengeService.getChallengeDetail(challengeId);
        return ResponseEntity.ok(challenge);
    }

    /**
     * 챌린지 참가
     */
    @PostMapping("/{challengeId}/join")
    public ResponseEntity<Void> joinChallenge(
            @PathVariable Long challengeId,
            @RequestParam Long userId) {
        challengeService.joinChallenge(challengeId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 챌린지 참가 여부 확인
     */
    @GetMapping("/{challengeId}/joined")
    public ResponseEntity<Boolean> isUserJoined(
            @PathVariable Long challengeId,
            @RequestParam Long userId) {
        boolean isJoined = challengeService.isUserJoined(challengeId, userId);
        return ResponseEntity.ok(isJoined);
    }

    /**
     * 챌린지 참가자 목록 조회
     */
    @GetMapping("/{challengeId}/entries")
    public ResponseEntity<List<ChallengeEntry>> getChallengeEntries(@PathVariable Long challengeId) {
        List<ChallengeEntry> entries = challengeService.getChallengeEntries(challengeId);
        return ResponseEntity.ok(entries);
    }

    /**
     * 챌린지 시작
     */
    @PostMapping("/{challengeId}/start")
    public ResponseEntity<Void> startChallenge(@PathVariable Long challengeId) {
        challengeService.startChallenge(challengeId);
        return ResponseEntity.ok().build();
    }

    /**
     * 챌린지 종료
     */
    @PostMapping("/{challengeId}/close")
    public ResponseEntity<Void> closeChallenge(@PathVariable Long challengeId) {
        challengeService.closeChallenge(challengeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{challengeId}/me")
    public ChallengeJoinResponse getMyJoinResult(
            @PathVariable Long challengeId,
            @AuthenticationPrincipal User user
    ) {
        return challengeService.checkChallengeByUserId(challengeId, user.getId());
    }


}
