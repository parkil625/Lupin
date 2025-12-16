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
        user1.addPoints(100L); // User 엔티티의 totalPoints 필드 업데이트
        entityManager.persist(user1); // 영속성 컨텍스트 반영

        // 2. -50점 유저 (2등)
        User user2 = createAndSaveUser("Loser");
        user2.deductPoints(50L); // 0 - 50 = -50 (User 엔티티가 음수 허용으로 수정되었다고 가정)
        entityManager.persist(user2);

        entityManager.flush(); // DB에 즉시 반영하여 네이티브 쿼리가 읽을 수 있게 함

        // when
        // -50점인 유저(user2)의 랭킹 컨텍스트 조회
        List<Object[]> result = pointLogRepository.findUserRankingContext(user2.getId());

        // then
        // 결과: id, name, avatar, department, total_points, user_rank
        Object[] myRankRow = result.stream()
                .filter(row -> ((Number) row[0]).longValue() == user2.getId())
                .findFirst()
                .orElseThrow();

        // 검증 1: DB에는 -50으로 저장되어 있어도 조회 결과는 0이어야 함 (GREATEST 함수 동작 확인)
        assertThat(((Number) myRankRow[4]).longValue()).isEqualTo(0L);

        // 검증 2: 점수는 0으로 보이지만, 100점 유저 다음인 2등이어야 함
        assertThat(((Number) myRankRow[5]).intValue()).isEqualTo(2);
    }
}