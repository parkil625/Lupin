package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class WorkoutScoreService {

    private static final int MAX_WORKOUT_HOURS = 24;
    private static final int PHOTO_TIME_TOLERANCE_HOURS = 6;

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
    // [Modified] 고정 체중(65kg) 상수 삭제 -> 사용자 DB 몸무게 사용

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
    public int calculateCalories(String activity, LocalDateTime startTime, LocalDateTime endTime, double weight) {
        long durationMinutes = Duration.between(startTime, endTime).toMinutes();
        double durationHours = durationMinutes / 60.0;
        double intensity = getIntensity(activity);
        double baseMET = intensity * 8;

        int calories = (int) Math.round(baseMET * weight * durationHours);

        log.debug(">>> [Calorie Calc] Result: {} kcal (Activity: {}, Duration: {}min, Weight: {}kg, MET: {})",
                calories, activity, durationMinutes, weight, baseMET);

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

    /**
     * 운동 검증 및 점수/칼로리 계산 (사용자 체중 적용)
     * @param activity 운동 종류
     * @param startTimeOpt 시작 시간 (없으면 검증 없이 0점 반환)
     * @param endTimeOpt 종료 시간 (없으면 검증 없이 0점 반환)
     * @param feedDate 피드 기준 날짜
     * @param user 사용자 엔티티 (체중 조회용)
     * @return 점수와 칼로리를 담은 결과 (검증 실패 시 둘 다 0)
     */
    public WorkoutResult validateAndCalculate(String activity,
                                               Optional<LocalDateTime> startTimeOpt,
                                               Optional<LocalDateTime> endTimeOpt,
                                               LocalDate feedDate,
                                               com.example.demo.domain.entity.User user) {
        if (startTimeOpt.isEmpty() || endTimeOpt.isEmpty()) {
            log.warn(">>> [Workout Calc] Missing time info for calculation.");
            return WorkoutResult.empty();
        }

        LocalDateTime startTime = startTimeOpt.get();
        LocalDateTime endTime = endTimeOpt.get();

        if (!isValidWorkoutTime(startTime, endTime, feedDate)) {
            log.warn(">>> [Workout Calc] Invalid workout time range.");
            return WorkoutResult.empty();
        }

        // [Logic] 사용자 체중 적용 (없으면 기본값 65kg)
        double userWeight = (user.getWeight() != null && user.getWeight() > 0) ? user.getWeight() : 65.0;
        log.info(">>> [Workout Calc] Applying weight: {}kg (User ID: {})", userWeight, user.getId());

        int score = calculateScore(activity, startTime, endTime);
        int calories = calculateCalories(activity, startTime, endTime, userWeight);
        long durationMinutes = calculateDurationMinutes(startTime, endTime);

        log.info(">>> [Workout Calc] Completed. Activity={}, Duration={}min, Score={}, Calories={}",
                activity, durationMinutes, score, calories);

        return new WorkoutResult(score, calories, true);
    }

    /**
     * 운동 시간 유효성 검증
     */
    public boolean isValidWorkoutTime(LocalDateTime startTime, LocalDateTime endTime, LocalDate feedDate) {
        if (!startTime.isBefore(endTime)) {
            log.warn("Invalid workout time: start={} is not before end={}", startTime, endTime);
            return false;
        }

        if (Duration.between(startTime, endTime).toHours() > MAX_WORKOUT_HOURS) {
            log.warn("Workout too long: {} hours", Duration.between(startTime, endTime).toHours());
            return false;
        }

        LocalDateTime allowedStart = feedDate.atStartOfDay().minusHours(PHOTO_TIME_TOLERANCE_HOURS);
        LocalDateTime allowedEnd = feedDate.atTime(23, 59, 59).plusHours(PHOTO_TIME_TOLERANCE_HOURS);

        boolean startTimeValid = !startTime.isBefore(allowedStart) && !startTime.isAfter(allowedEnd);
        boolean endTimeValid = !endTime.isBefore(allowedStart) && !endTime.isAfter(allowedEnd);

        if (!startTimeValid || !endTimeValid) {
            log.warn("Photo time outside allowed range: start={}, end={}, allowed=[{} ~ {}]",
                    startTime, endTime, allowedStart, allowedEnd);
            return false;
        }

        return true;
    }

    /**
     * 운동 계산 결과
     */
    public record WorkoutResult(int score, int calories, boolean valid) {
        public static WorkoutResult empty() {
            return new WorkoutResult(0, 0, false);
        }
    }
}
