package com.example.demo.repository;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.response.FeedListResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.config.TestRedisConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestRedisConfig.class)
@ActiveProfiles("test")
@Transactional
class FeedRepositoryTest {

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("피드 목록 조회 디버깅 테스트")
    void searchFeeds_Debug_Test() {
        // 1. [준비] 테스트용 유저 생성
        User user = User.builder()
                .userId("testUser")
                .email("test@example.com")
                .password("password")
                .realName("테스트유저")
                .role(Role.MEMBER)
                .build();
        userRepository.save(user);

        // 2. [준비] 테스트용 피드 생성
        Feed feed = Feed.builder()
                .activityType("러닝")
                .content("테스트 피드 내용입니다.")
                .calories(150.0)
                .build();
        feed.setWriter(user); // 작성자 연결
        feedRepository.save(feed);

        // 3. [중요] 영속성 컨텍스트 비우기 (실제 API 요청처럼 DB에서 새로 가져오게 함)
        em.flush();
        em.clear();

        // 4. [실행] 문제의 메서드 호출
        System.out.println("\n========== [TEST START] searchFeeds 호출 ==========");
        Pageable pageable = PageRequest.of(0, 10);

        try {
            // 여기서 에러가 나면 바로 잡힙니다.
            Page<FeedListResponse> result = feedRepository.searchFeeds(null, null, null, null, pageable);

            System.out.println("========== [TEST SUCCESS] 호출 성공 ==========");
            System.out.println("조회된 피드 수: " + result.getTotalElements());

            if (!result.getContent().isEmpty()) {
                FeedListResponse firstFeed = result.getContent().get(0);
                System.out.println("첫 번째 피드 작성자: " + firstFeed.getAuthor());
                System.out.println("첫 번째 피드 내용: " + firstFeed.getContent());
                System.out.println("첫 번째 피드 이미지 리스트: " + firstFeed.getImages());
            }

            assertThat(result).isNotNull();

        } catch (Exception e) {
            System.out.println("\n========== [TEST FAILED] 에러 발생 ==========");
            System.err.println("에러 메시지: " + e.getMessage());
            System.err.println("에러 원인: " + e.getCause());
            e.printStackTrace(); // 상세 스택 트레이스 출력
            throw e; // 테스트 실패 처리
        }
    }
}