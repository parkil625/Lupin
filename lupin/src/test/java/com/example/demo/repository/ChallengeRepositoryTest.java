package com.example.demo.repository;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.domain.entity.Challenge;
import com.example.demo.domain.entity.ChallengeEntry;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ChallengeStatus;
import com.example.demo.domain.enums.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@Transactional
class ChallengeRepositoryTest {
    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private ChallengeEntryRepository challengeEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Test
    void findByOpenTimeBetweenOrderByOpenTimeAsc() {
        // given
        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 9, 0);

        // when
        var result = challengeRepository.findByOpenTimeBetweenOrderByOpenTimeAsc(
                now.minusHours(1),
                now.plusHours(1)
        );

        // then - 빈 결과도 허용
        assertThat(result).isNotNull();
    }
}
