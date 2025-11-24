package com.example.demo.integration;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.response.FeedListResponse;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.FeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.demo.config.TestRedisConfig;

import static org.assertj.core.api.Assertions.*;

/**
 * FeedService 통합 테스트 - Testcontainers (실제 MySQL + Redis)
 * H2 대신 진짜 MySQL/Redis Docker 컨테이너에서 테스트
 *
 * 주의: Docker가 실행 중이어야 합니다.
 */
@SpringBootTest
@Import(TestRedisConfig.class)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Transactional
@DisplayName("FeedService 통합 테스트 (Testcontainers)")
class FeedServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("lupin_test")
            .withUsername("test")
            .withPassword("test")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL 설정
        registry.add("spring.datasource.url", () ->
                mysqlContainer.getJdbcUrl() + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");

        // Redis 설정
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    private FeedService feedService;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Given: 테스트 사용자 생성
        testUser = userRepository.save(User.builder()
                .userId("testuser")
                .email("test@test.com")
                .password("password123")
                .realName("테스터")
                .role(Role.MEMBER)
                .build());
    }

    @Test
    @DisplayName("피드 목록 조회 - 페이징 처리")
    void 피드_목록_조회시_페이징이_정상_동작한다() {
        // Given: 15개의 피드 생성
        for (int i = 0; i < 15; i++) {
            feedRepository.save(Feed.builder()
                    .activityType("RUNNING")
                    .content("테스트 피드 " + i)
                    .writer(testUser)
                    .build());
        }

        // When: 첫 페이지 조회 (10개)
        Page<FeedListResponse> result = feedService.getFeeds(
                null, null, null, null,
                PageRequest.of(0, 10)
        );

        // Then: 페이징 검증
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(15);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("피드 검색 - 키워드 필터링")
    void 키워드로_피드_검색시_해당_내용만_반환한다() {
        // Given: 다양한 내용의 피드 생성
        feedRepository.save(Feed.builder()
                .activityType("RUNNING")
                .content("오늘 5km 조깅을 했습니다")
                .writer(testUser)
                .build());

        feedRepository.save(Feed.builder()
                .activityType("WALKING")
                .content("산책하며 힐링했습니다")
                .writer(testUser)
                .build());

        // When: '조깅' 키워드로 검색
        Page<FeedListResponse> result = feedService.getFeeds(
                "조깅", null, null, null,
                PageRequest.of(0, 10)
        );

        // Then: 조깅 관련 피드만 반환
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).contains("조깅");
    }

    @Test
    @DisplayName("피드 검색 - 활동 유형 필터링")
    void 활동유형으로_피드_검색시_해당_유형만_반환한다() {
        // Given: 다양한 활동 유형의 피드 생성
        feedRepository.save(Feed.builder()
                .activityType("RUNNING")
                .content("러닝 피드")
                .writer(testUser)
                .build());

        feedRepository.save(Feed.builder()
                .activityType("WALKING")
                .content("워킹 피드")
                .writer(testUser)
                .build());

        feedRepository.save(Feed.builder()
                .activityType("RUNNING")
                .content("또 다른 러닝")
                .writer(testUser)
                .build());

        // When: RUNNING 유형으로 검색
        Page<FeedListResponse> result = feedService.getFeeds(
                null, "RUNNING", null, null,
                PageRequest.of(0, 10)
        );

        // Then: RUNNING 피드만 반환
        assertThat(result.getContent()).hasSize(2);
        result.getContent().forEach(feed ->
                assertThat(feed.getActivity()).isEqualTo("RUNNING")
        );
    }

    @Test
    @DisplayName("트랜잭션 롤백 - 예외 발생 시 롤백")
    void 예외_발생시_트랜잭션이_롤백된다() {
        // Given: 피드 생성
        Feed feed = feedRepository.save(Feed.builder()
                .activityType("RUNNING")
                .content("테스트 피드")
                .writer(testUser)
                .build());

        long initialCount = feedRepository.count();

        // When & Then: 존재하지 않는 피드 삭제 시도 시 예외
        assertThatThrownBy(() -> feedService.deleteFeed(999999L, testUser.getId()))
                .isInstanceOf(Exception.class);

        // 롤백 확인 - 카운트 변화 없음
        assertThat(feedRepository.count()).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("대용량 데이터 처리 - 1000개 피드 페이징")
    void 대용량_피드_데이터_페이징이_정상_동작한다() {
        // Given: 1000개의 피드 생성
        for (int i = 0; i < 1000; i++) {
            feedRepository.save(Feed.builder()
                    .activityType("RUNNING")
                    .content("대용량 테스트 피드 " + i)
                    .writer(testUser)
                    .build());
        }

        // When: 마지막 페이지 조회
        Page<FeedListResponse> result = feedService.getFeeds(
                null, null, null, null,
                PageRequest.of(99, 10)  // 100번째 페이지
        );

        // Then: 정확한 페이징
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(1000);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("여러 사용자 좋아요 - 순차 처리")
    void 여러_사용자가_순차적으로_좋아요를_누르면_모두_처리된다() {
        // Given: 피드 생성
        Feed feed = feedRepository.save(Feed.builder()
                .activityType("RUNNING")
                .content("인기 피드")
                .writer(testUser)
                .build());

        // 5명의 사용자 생성
        int userCount = 5;
        User[] users = new User[userCount];
        for (int i = 0; i < userCount; i++) {
            users[i] = userRepository.save(User.builder()
                    .userId("user" + i)
                    .email("user" + i + "@test.com")
                    .password("password")
                    .realName("유저" + i)
                    .role(Role.MEMBER)
                    .build());
        }

        // When: 순차적으로 좋아요 요청
        int successCount = 0;
        for (int i = 0; i < userCount; i++) {
            try {
                feedService.likeFeed(feed.getId(), users[i].getId());
                successCount++;
            } catch (Exception e) {
                // 좋아요 실패 시 무시
            }
        }

        // Then: 모든 좋아요가 처리됨
        assertThat(successCount).isEqualTo(userCount);
    }
}
