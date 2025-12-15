package com.example.demo.repository;

import com.example.demo.config.QueryDslConfig;
import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class PointLogRepositoryTest {

    @Autowired
    private PointLogRepository pointLogRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("test@test.com")
                .password("password")
                .name("testUser")
                .role(Role.MEMBER)
                .build();
        userRepository.save(user);

        PointLog log1 = PointLog.builder()
                .user(user)
                .points(100L)
                .build();
        PointLog log2 = PointLog.builder()
                .user(user)
                .points(50L)
                .build();
        pointLogRepository.save(log1);
        pointLogRepository.save(log2);

        user.addPoints(150);
        userRepository.save(user);
    }

    @Test
    @DisplayName("월별 포인트 합계 조회")
    void sumPointsByUserIdAndMonthTest() {
        // given
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        // when
        Long sum = pointLogRepository.sumPointsByUserIdAndMonth(user.getId(), start, end);

        // then
        assertThat(sum).isEqualTo(150);
    }

    @Test
    @DisplayName("이번 달 활동 유저 수 조회")
    void countActiveUsersThisMonthTest() {
        // given
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        // when
        Long count = pointLogRepository.countActiveUsersThisMonth(start, end);

        // then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("평균 포인트 조회")
    void getAveragePointsPerUserTest() {
        // when
        Double avg = pointLogRepository.getAveragePointsPerUser();

        // then
        assertThat(avg).isEqualTo(150.0);
    }

    @Test
    @DisplayName("유저 랭킹 컨텍스트 조회")
    void findUserRankingContextTest() {
        // when
        List<Object[]> result = pointLogRepository.findUserRankingContext(user.getId());

        // then
        assertThat(result).isNotEmpty();
    }
}