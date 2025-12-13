package com.example.demo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkoutScoreService 테스트")
class WorkoutScoreServiceTest {

    @InjectMocks
    private WorkoutScoreService workoutScoreService;

    @Nested
    @DisplayName("calculateScore")
    class CalculateScore {

        @Test
        @DisplayName("헬스 30분 운동 시 점수를 계산한다")
        void calculateScoreForHealth30Min() {
            // given
            String activity = "헬스";
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 10, 30);

            // when
            int score = workoutScoreService.calculateScore(activity, startTime, endTime);

            // then
            // 30분 * 0.8 강도 = 24점
            assertThat(score).isEqualTo(24);
        }

        @Test
        @DisplayName("달리기 30분 운동 시 최대 점수 30점을 넘지 않는다")
        void calculateScoreDoesNotExceedMax() {
            // given
            String activity = "달리기";
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 10, 30);

            // when
            int score = workoutScoreService.calculateScore(activity, startTime, endTime);

            // then
            // 30분 * 1.5 강도 = 45 -> 최대 30점
            assertThat(score).isEqualTo(30);
        }

        @Test
        @DisplayName("산책 60분 운동 시 점수를 계산한다")
        void calculateScoreForWalking60Min() {
            // given
            String activity = "산책";
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 11, 0);

            // when
            int score = workoutScoreService.calculateScore(activity, startTime, endTime);

            // then
            // 60분 * 0.5 강도 = 30점
            assertThat(score).isEqualTo(30);
        }

        @Test
        @DisplayName("알 수 없는 운동 종류는 기본 강도 0.8을 사용한다")
        void calculateScoreWithUnknownActivity() {
            // given
            String activity = "알 수 없는 운동";
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 10, 30);

            // when
            int score = workoutScoreService.calculateScore(activity, startTime, endTime);

            // then
            // 30분 * 0.8 기본 강도 = 24점
            assertThat(score).isEqualTo(24);
        }

        @Test
        @DisplayName("0분 운동 시 점수는 0이다")
        void calculateScoreForZeroMinutes() {
            // given
            String activity = "헬스";
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 10, 0);

            // when
            int score = workoutScoreService.calculateScore(activity, startTime, endTime);

            // then
            assertThat(score).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("calculateCalories")
    class CalculateCalories {

        @Test
        @DisplayName("헬스 60분 운동 시 칼로리를 계산한다")
        void calculateCaloriesForHealth60Min() {
            // given
            String activity = "헬스";
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 11, 0);

            // when
            int calories = workoutScoreService.calculateCalories(activity, startTime, endTime);

            // then
            // MET = 0.8 * 8 = 6.4
            // 칼로리 = 6.4 * 65kg * 1시간 = 416
            assertThat(calories).isEqualTo(416);
        }

        @Test
        @DisplayName("달리기 30분 운동 시 칼로리를 계산한다")
        void calculateCaloriesForRunning30Min() {
            // given
            String activity = "달리기";
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 10, 30);

            // when
            int calories = workoutScoreService.calculateCalories(activity, startTime, endTime);

            // then
            // MET = 1.5 * 8 = 12
            // 칼로리 = 12 * 65kg * 0.5시간 = 390
            assertThat(calories).isEqualTo(390);
        }

        @Test
        @DisplayName("0분 운동 시 칼로리는 0이다")
        void calculateCaloriesForZeroMinutes() {
            // given
            String activity = "헬스";
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 10, 0);

            // when
            int calories = workoutScoreService.calculateCalories(activity, startTime, endTime);

            // then
            assertThat(calories).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("calculateDurationMinutes")
    class CalculateDurationMinutes {

        @Test
        @DisplayName("운동 시간(분)을 계산한다")
        void calculateDuration() {
            // given
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 10, 45);

            // when
            long duration = workoutScoreService.calculateDurationMinutes(startTime, endTime);

            // then
            assertThat(duration).isEqualTo(45);
        }

        @Test
        @DisplayName("같은 시간이면 0분이다")
        void calculateDurationForSameTime() {
            // given
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 10, 0);

            // when
            long duration = workoutScoreService.calculateDurationMinutes(startTime, endTime);

            // then
            assertThat(duration).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getIntensity")
    class GetIntensity {

        @Test
        @DisplayName("헬스의 강도는 0.8이다")
        void getIntensityForHealth() {
            // when
            double intensity = workoutScoreService.getIntensity("헬스");

            // then
            assertThat(intensity).isEqualTo(0.8);
        }

        @Test
        @DisplayName("달리기의 강도는 1.5이다")
        void getIntensityForRunning() {
            // when
            double intensity = workoutScoreService.getIntensity("달리기");

            // then
            assertThat(intensity).isEqualTo(1.5);
        }

        @Test
        @DisplayName("산책의 강도는 0.5이다")
        void getIntensityForWalking() {
            // when
            double intensity = workoutScoreService.getIntensity("산책");

            // then
            assertThat(intensity).isEqualTo(0.5);
        }

        @Test
        @DisplayName("HIIT의 강도는 1.7이다")
        void getIntensityForHIIT() {
            // when
            double intensity = workoutScoreService.getIntensity("HIIT");

            // then
            assertThat(intensity).isEqualTo(1.7);
        }

        @Test
        @DisplayName("알 수 없는 운동의 강도는 기본값 0.8이다")
        void getIntensityForUnknown() {
            // when
            double intensity = workoutScoreService.getIntensity("알 수 없는 운동");

            // then
            assertThat(intensity).isEqualTo(0.8);
        }
    }

    @Nested
    @DisplayName("validateAndCalculate")
    class ValidateAndCalculate {

        @Test
        @DisplayName("유효한 운동 시간으로 점수와 칼로리를 계산한다")
        void validateAndCalculateWithValidTime() {
            // given
            String activity = "헬스";
            LocalDate feedDate = LocalDate.of(2024, 1, 15);
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 15, 10, 30);

            // when
            WorkoutScoreService.WorkoutResult result = workoutScoreService.validateAndCalculate(
                    activity, Optional.of(startTime), Optional.of(endTime), feedDate);

            // then
            assertThat(result.valid()).isTrue();
            assertThat(result.score()).isEqualTo(24); // 30분 * 0.8 강도
            assertThat(result.calories()).isGreaterThan(0);
        }

        @Test
        @DisplayName("시작 시간이 없으면 빈 결과를 반환한다")
        void validateAndCalculateWithoutStartTime() {
            // given
            String activity = "헬스";
            LocalDate feedDate = LocalDate.of(2024, 1, 15);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 15, 10, 30);

            // when
            WorkoutScoreService.WorkoutResult result = workoutScoreService.validateAndCalculate(
                    activity, Optional.empty(), Optional.of(endTime), feedDate);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.score()).isEqualTo(0);
            assertThat(result.calories()).isEqualTo(0);
        }

        @Test
        @DisplayName("종료 시간이 없으면 빈 결과를 반환한다")
        void validateAndCalculateWithoutEndTime() {
            // given
            String activity = "헬스";
            LocalDate feedDate = LocalDate.of(2024, 1, 15);
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 10, 0);

            // when
            WorkoutScoreService.WorkoutResult result = workoutScoreService.validateAndCalculate(
                    activity, Optional.of(startTime), Optional.empty(), feedDate);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.score()).isEqualTo(0);
            assertThat(result.calories()).isEqualTo(0);
        }

        @Test
        @DisplayName("시작 시간이 종료 시간보다 늦으면 빈 결과를 반환한다")
        void validateAndCalculateWithInvalidTimeOrder() {
            // given
            String activity = "헬스";
            LocalDate feedDate = LocalDate.of(2024, 1, 15);
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 11, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 15, 10, 0);

            // when
            WorkoutScoreService.WorkoutResult result = workoutScoreService.validateAndCalculate(
                    activity, Optional.of(startTime), Optional.of(endTime), feedDate);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.score()).isEqualTo(0);
        }

        @Test
        @DisplayName("24시간을 초과하는 운동은 빈 결과를 반환한다")
        void validateAndCalculateWithTooLongWorkout() {
            // given
            String activity = "헬스";
            LocalDate feedDate = LocalDate.of(2024, 1, 15);
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 16, 11, 0); // 25시간

            // when
            WorkoutScoreService.WorkoutResult result = workoutScoreService.validateAndCalculate(
                    activity, Optional.of(startTime), Optional.of(endTime), feedDate);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.score()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("isValidWorkoutTime")
    class IsValidWorkoutTime {

        @Test
        @DisplayName("허용 범위 내 시간은 유효하다")
        void validTimeWithinAllowedRange() {
            // given
            LocalDate feedDate = LocalDate.of(2024, 1, 15);
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 15, 11, 0);

            // when
            boolean result = workoutScoreService.isValidWorkoutTime(startTime, endTime, feedDate);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("시작 시간이 종료 시간보다 늦으면 유효하지 않다")
        void invalidWhenStartAfterEnd() {
            // given
            LocalDate feedDate = LocalDate.of(2024, 1, 15);
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 12, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 15, 11, 0);

            // when
            boolean result = workoutScoreService.isValidWorkoutTime(startTime, endTime, feedDate);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("24시간을 초과하면 유효하지 않다")
        void invalidWhenTooLong() {
            // given
            LocalDate feedDate = LocalDate.of(2024, 1, 15);
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 14, 10, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 15, 11, 0); // 25시간

            // when
            boolean result = workoutScoreService.isValidWorkoutTime(startTime, endTime, feedDate);

            // then
            assertThat(result).isFalse();
        }
    }
}
