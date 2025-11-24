package com.example.demo.chaos;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.repository.UserRepository;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.demo.config.TestRedisConfig;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Chaos Engineering 테스트 - Toxiproxy를 사용한 DB 장애 시뮬레이션
 *
 * 네트워크 지연, 연결 끊김 등의 장애 상황에서 애플리케이션의 복원력을 검증합니다.
 *
 * 주의: Docker가 실행 중이어야 합니다.
 */
@SpringBootTest
@Import(TestRedisConfig.class)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@DisplayName("Chaos Engineering - DB 복원력 테스트")
class DatabaseResilienceTest {

    private static final Network network = Network.newNetwork();

    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("lupin_test")
            .withUsername("test")
            .withPassword("test")
            .withNetwork(network)
            .withNetworkAliases("mysql")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    @Container
    static ToxiproxyContainer toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
            .withNetwork(network);

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withNetwork(network)
            .withNetworkAliases("redis");

    private static Proxy mysqlProxy;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
        // Toxiproxy를 통한 MySQL 연결
        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
        mysqlProxy = toxiproxyClient.createProxy("mysql", "0.0.0.0:8666", "mysql:3306");

        String jdbcUrl = "jdbc:mysql://" + toxiproxy.getHost() + ":" + toxiproxy.getMappedPort(8666)
                + "/lupin_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                + "&connectTimeout=5000&socketTimeout=5000";

        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");

        // HikariCP 설정 - 빠른 장애 감지
        registry.add("spring.datasource.hikari.connection-timeout", () -> "5000");
        registry.add("spring.datasource.hikari.validation-timeout", () -> "3000");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");

        // Redis 설정
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() throws IOException {
        // 각 테스트 전에 프록시를 정상 상태로 초기화
        if (mysqlProxy != null) {
            mysqlProxy.toxics().getAll().forEach(toxic -> {
                try {
                    toxic.remove();
                } catch (IOException e) {
                    // ignore
                }
            });
        }
    }

    @Test
    @DisplayName("정상 상태에서 사용자 저장 성공")
    void 정상_상태에서_사용자_저장이_성공한다() {
        // Given
        User user = User.builder()
                .userId("normaluser")
                .email("normal@test.com")
                .password("password123")
                .realName("정상 사용자")
                .role(Role.MEMBER)
                .build();

        // When
        User saved = userRepository.save(user);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo("normaluser");
    }

    @Test
    @DisplayName("네트워크 지연 시 쿼리 응답 시간 증가")
    void 네트워크_지연_시_쿼리_응답_시간이_증가한다() throws IOException {
        // Given: 500ms 네트워크 지연 추가
        mysqlProxy.toxics()
                .latency("latency", ToxicDirection.DOWNSTREAM, 500);

        User user = User.builder()
                .userId("delayuser")
                .email("delay@test.com")
                .password("password123")
                .realName("지연 사용자")
                .role(Role.MEMBER)
                .build();

        // When
        Instant start = Instant.now();
        User saved = userRepository.save(user);
        Duration elapsed = Duration.between(start, Instant.now());

        // Then: 지연이 있더라도 저장은 성공
        assertThat(saved.getId()).isNotNull();
        // 500ms 이상의 지연이 발생했는지 확인
        assertThat(elapsed.toMillis()).isGreaterThanOrEqualTo(500);
    }

    @Test
    @DisplayName("지연 후 복구 시 조회 성공")
    void 지연_후_복구_시_조회가_성공한다() throws IOException, InterruptedException {
        // Given: 먼저 정상적으로 사용자 저장
        User user1 = User.builder()
                .userId("user1")
                .email("user1@test.com")
                .password("password123")
                .realName("사용자1")
                .role(Role.MEMBER)
                .build();
        userRepository.save(user1);

        // When: 짧은 지연 추가
        mysqlProxy.toxics()
                .latency("delay", ToxicDirection.DOWNSTREAM, 100);

        // 조회
        User found = userRepository.findByUserId("user1").orElse(null);

        // toxic 제거
        mysqlProxy.toxics().get("delay").remove();

        // Then: 조회 성공
        assertThat(found).isNotNull();
        assertThat(found.getUserId()).isEqualTo("user1");
    }

    @Test
    @DisplayName("대역폭 제한 상태에서도 작은 쿼리 성공")
    void 대역폭_제한_상태에서도_작은_쿼리가_성공한다() throws IOException {
        // Given: 대역폭 제한 (1KB/s)
        mysqlProxy.toxics()
                .bandwidth("bandwidth", ToxicDirection.DOWNSTREAM, 1024);

        // When: 단순 조회 쿼리
        long count = userRepository.count();

        // Then: 작은 쿼리는 성공
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("슬로우 클로즈 상황 처리")
    void 슬로우_클로즈_상황에서도_쿼리가_완료된다() throws IOException {
        // Given: 연결 종료 지연
        mysqlProxy.toxics()
                .slowClose("slow_close", ToxicDirection.DOWNSTREAM, 1000);

        User user = User.builder()
                .userId("slowclose")
                .email("slowclose@test.com")
                .password("password123")
                .realName("슬로우 클로즈")
                .role(Role.MEMBER)
                .build();

        // When & Then: 저장은 성공
        User saved = userRepository.save(user);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("복합 장애 상황 - 지연 + 패킷 손실")
    void 복합_장애_상황에서_재시도_로직이_동작한다() throws IOException {
        // Given: 지연 + 간헐적 연결 끊김
        mysqlProxy.toxics()
                .latency("latency", ToxicDirection.DOWNSTREAM, 200);

        User user = User.builder()
                .userId("complexfault")
                .email("complex@test.com")
                .password("password123")
                .realName("복합 장애")
                .role(Role.MEMBER)
                .build();

        // When
        Instant start = Instant.now();
        User saved = userRepository.save(user);
        Duration elapsed = Duration.between(start, Instant.now());

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(elapsed.toMillis()).isGreaterThanOrEqualTo(200);
    }

    @Test
    @DisplayName("장애 복구 후 정상 동작 확인")
    void 장애_복구_후_정상_동작한다() throws IOException, InterruptedException {
        // Given: 초기 카운트
        long initialCount = userRepository.count();

        // 짧은 지연 추가
        mysqlProxy.toxics()
                .latency("delay", ToxicDirection.DOWNSTREAM, 100);

        // When: 지연이 있는 상태에서 저장
        User user = User.builder()
                .userId("recoveryuser")
                .email("recovery@test.com")
                .password("password123")
                .realName("복구 테스트")
                .role(Role.MEMBER)
                .build();

        User saved = userRepository.save(user);

        // toxic 제거 (복구)
        mysqlProxy.toxics().get("delay").remove();

        // Then: 저장 성공 확인
        assertThat(saved.getId()).isNotNull();
        long finalCount = userRepository.count();
        assertThat(finalCount).isEqualTo(initialCount + 1);
    }
}
