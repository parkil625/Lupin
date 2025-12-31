package com.example.demo.repository;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PointType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PointLogRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private PointLogRepository pointLogRepository;

    @Autowired
    private EntityManager entityManager; // [필수] User의 totalPoints 업데이트 반영을 위해 필요

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
        LocalDateTime endOfMonth = now.toLocalDate().withDayOfMonth(now.toLocalDate().lengthOfMonth()).atTime(23, 59, 59);

        Long monthlyPoints = pointLogRepository.sumPointsByUserIdAndMonth(user.getId(), startOfMonth, endOfMonth);

        // then
        // 예상 결과: 100(EARN) - 10(DEDUCT) = 90
        assertThat(monthlyPoints).isEqualTo(90L);
    }

    @Test
    @DisplayName("내 랭킹 조회(NativeQuery) 시 음수 포인트는 0으로 반환되지만 순위는 유지된다")
    void findUserRankingContext_NegativePointsAsZero() {
        // given
        // 1. 100점 유저 (1등)
        User user1 = createAndSaveUser("Winner");
        // [수정] 쿼리가 point_logs 테이블을 집계하므로 PointLog를 저장해야 함
        pointLogRepository.save(PointLog.builder()
                .user(user1)
                .points(100L)
                .type(PointType.EARN)
                .build());

        // 2. -50점 유저 (2등)
        User user2 = createAndSaveUser("Loser");
        // [수정] 쿼리 조건에 맞는 DEDUCT 타입으로 음수 로그 저장
        pointLogRepository.save(PointLog.builder()
                .user(user2)
                .points(-50L)
                .type(PointType.DEDUCT)
                .build());

        // when
        // [수정] 기간 파라미터 추가 (전체 기간 조회를 위해 넉넉하게 설정)
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        
        List<Object[]> result = pointLogRepository.findUserRankingContext(user2.getId(), start, end);

        // then
        // 결과: id, name, avatar, department, total_points, user_rank
        Object[] myRankRow = result.stream()
                .filter(row -> ((Number) row[0]).longValue() == user2.getId())
                .findFirst()
                .orElseThrow();

        // 검증 1: DB에는 -50으로 계산되어도 조회 결과(total_points)는 0이어야 함 (GREATEST 함수 동작)
        assertThat(((Number) myRankRow[4]).longValue()).isEqualTo(0L);

        // 검증 2: 점수는 0으로 보이지만, 100점 유저 다음인 2등이어야 함
        assertThat(((Number) myRankRow[5]).intValue()).isEqualTo(2);
    }
}