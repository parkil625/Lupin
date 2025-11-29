package com.example.demo.repository;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class PointLogRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private PointLogRepository pointLogRepository;

    @Test
    @DisplayName("sumPointsByUser로 사용자의 총 포인트를 계산한다")
    void sumPointsByUserTest() {
        // given
        User user = createAndSaveUser("testUser");
        pointLogRepository.save(PointLog.builder().user(user).points(100L).build());
        pointLogRepository.save(PointLog.builder().user(user).points(50L).build());
        pointLogRepository.save(PointLog.builder().user(user).points(-30L).build());

        // when
        Long totalPoints = pointLogRepository.sumPointsByUser(user);

        // then
        assertThat(totalPoints).isEqualTo(120L);
    }

    @Test
    @DisplayName("포인트 로그가 없으면 0을 반환한다")
    void sumPointsByUserReturnsZeroWhenNoLogsTest() {
        // given
        User user = createAndSaveUser("testUser");

        // when
        Long totalPoints = pointLogRepository.sumPointsByUser(user);

        // then
        assertThat(totalPoints).isEqualTo(0L);
    }
}
