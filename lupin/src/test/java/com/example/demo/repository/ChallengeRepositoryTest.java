package com.example.demo.repository;

import com.example.demo.domain.entity.Challenge;
import com.example.demo.domain.entity.ChallengeEntry;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ChallengeStatus;
import com.example.demo.domain.enums.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.config.TestRedisConfig;

import java.time.LocalDateTime;

@SpringBootTest
@Import(TestRedisConfig.class)
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

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 9, 0);

        // 1.테스트용 유저
        User user = User.builder()
                .userId("testUser")
                .email("test@example.com")
                .password("password")
                .realName("테스트유저")
                .role(Role.MEMBER)
                .build();
        userRepository.save(user);

        // 2.테스트용 챌린지 9시에 시작, 9시 30분 종료, 타입: 예정
        Challenge challenge = Challenge.builder()
                .title("맛있는 루팡빵")
                .openTime(now)
                .endTime(now.plusMinutes(30))
                .status(ChallengeStatus.SCHEDULED)
                .build();
        challengeRepository.save(challenge);

        //3. 테스트용 챌린지 성공 리스트
        ChallengeEntry challengeEntry = ChallengeEntry.of(challenge, user, now.plusMinutes(15));

        challengeEntryRepository.save(challengeEntry);


        em.flush();
        em.clear();
    }


}
