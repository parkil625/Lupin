package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class WorkoutScoreService {

    // 운동 종류별 강도 (imageMetadata.ts와 동일)
    private static final Map<String, Double> INTENSITY_VALUES = Map.ofEntries(
            Map.entry("산책", 0.5),
            Map.entry("요가", 0.5),
            Map.entry("스트레칭", 0.6),
            Map.entry("필라테스", 0.6),
            Map.entry("골프", 0.7),
            Map.entry("빠른 걷기", 0.8),
            Map.entry("헬스", 0.8),
            Map.entry("웨이트", 0.8),
            Map.entry("배드민턴", 0.9),
            Map.entry("탁구", 0.9),
            Map.entry("자전거 타기", 1.0),
            Map.entry("사이클", 1.0),
            Map.entry("수영", 1.0),
            Map.entry("등산", 1.0),
            Map.entry("테니스", 1.1),
            Map.entry("조깅", 1.2),
            Map.entry("축구", 1.4),
            Map.entry("농구", 1.4),
            Map.entry("달리기", 1.5),
            Map.entry("런닝", 1.5),
            Map.entry("복싱", 1.5),
            Map.entry("수영(빠르게)", 1.7),
            Map.entry("자전거(빠르게)", 1.7),
            Map.entry("HIIT", 1.7),
            Map.entry("크로스핏", 1.7),
            Map.entry("줄넘기", 1.8),
            Map.entry("걷기", 0.6),
            Map.entry("기타", 0.8)
    );

    private static final int MAX_SCORE = 30;
    private static final double DEFAULT_INTENSITY = 0.8;
    private static final double BASE_WEIGHT_KG = 65.0;

    /**
     * 운동 점수 계산
     * 점수 = 시간(분) × 강도, 최대 30점
     */
    public int calculateScore(String activity, LocalDateTime startTime, LocalDateTime endTime) {
        long durationMinutes = Duration.between(startTime, endTime).toMinutes();
        double intensity = getIntensity(activity);

        double rawScore = durationMinutes * intensity;
        int score = (int) Math.min(MAX_SCORE, Math.round(rawScore));

        log.debug("Calculated score: {} (activity={}, duration={}min, intensity={})",
                score, activity, durationMinutes, intensity);

        return score;
    }

    /**
     * 칼로리 계산
     * 칼로리 = MET × 체중(kg) × 시간(hours)
     * MET = 강도 × 8
     */
    public int calculateCalories(String activity, LocalDateTime startTime, LocalDateTime endTime) {
        long durationMinutes = Duration.between(startTime, endTime).toMinutes();
        double durationHours = durationMinutes / 60.0;
        double intensity = getIntensity(activity);
        double baseMET = intensity * 8;

        int calories = (int) Math.round(baseMET * BASE_WEIGHT_KG * durationHours);

        log.debug("Calculated calories: {} (activity={}, duration={}min)",
                calories, activity, durationMinutes);

        return calories;
    }

    /**
     * 운동 시간(분) 계산
     */
    public long calculateDurationMinutes(LocalDateTime startTime, LocalDateTime endTime) {
        return Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * 운동 종류별 강도 조회
     */
    public double getIntensity(String activity) {
        return INTENSITY_VALUES.getOrDefault(activity, DEFAULT_INTENSITY);
    }
}
