package com.example.demo.repository;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PointType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PointLogRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private PointLogRepository pointLogRepository;

    @Test
    @DisplayName("sumPointsByUserId로 사용자의 총 포인트를 계산한다")
    void sumPointsByUserIdTest() {
        // given
        User user = createAndSaveUser("testUser");
        pointLogRepository.save(PointLog.builder().user(user).points(100L).type(PointType.EARN).build());
        pointLogRepository.save(PointLog.builder().user(user).points(50L).type(PointType.EARN).build());
        pointLogRepository.save(PointLog.builder().user(user).points(-30L).type(PointType.USE).build());

        // when
        Long totalPoints = pointLogRepository.sumPointsByUserId(user.getId());

        // then
        assertThat(totalPoints).isEqualTo(120L);
    }

    @Test
    @DisplayName("포인트 로그가 없으면 0을 반환한다")
    void sumPointsByUserIdReturnsZeroWhenNoLogsTest() {
        // given
        User user = createAndSaveUser("testUser");

        // when
        Long totalPoints = pointLogRepository.sumPointsByUserId(user.getId());

        // then
        assertThat(totalPoints).isEqualTo(0L);
    }

    @Test
    @DisplayName("월간 포인트 집계 시 USE 타입은 제외하고 EARN과 DEDUCT만 합산한다")
    void sumPointsByUserIdAndMonth_CheckPointTypeFilter() {
        // given
        User user = createAndSaveUser("rankingUser");
        LocalDateTime now = LocalDateTime.now();

        // 1. 포인트 획득 (+100) -> 랭킹 포함 (EARN)
        pointLogRepository.save(PointLog.builder()
                .user(user)
                .points(100L)
                .type(PointType.EARN)
                .createdAt(now)
                .build());

        // 2. 포인트 사용 (-50) -> 랭킹 제외 (USE - 경매 등)
        pointLogRepository.save(PointLog.builder()
                .user(user)
                .points(-50L)
                .type(PointType.USE) // [중요] 랭킹 집계에서 빠져야 함
                .createdAt(now)
                .build());

        // 3. 포인트 회수 (-10) -> 랭킹 포함 (DEDUCT - 피드 삭제 등)
        pointLogRepository.save(PointLog.builder()
                .user(user)
                .points(-10L)
                .type(PointType.DEDUCT)
                .createdAt(now)
                .build());

        // when
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        // 기존 오류 라인: now.withDayOfMonth(...).atTime(...) -> LocalDateTime에는 atTime이 없음
        // 수정: now.toLocalDate().withDayOfMonth(...).atTime(...)
        LocalDateTime endOfMonth = now.toLocalDate().withDayOfMonth(now.toLocalDate().lengthOfMonth()).atTime(23, 59, 59);

        Long monthlyPoints = pointLogRepository.sumPointsByUserIdAndMonth(user.getId(), startOfMonth, endOfMonth);

        // then
        // 예상 결과: 100(EARN) - 10(DEDUCT) = 90
        // -50(USE)은 랭킹 점수 계산에 포함되지 않아야 함
        assertThat(monthlyPoints).isEqualTo(90L);
    }
}