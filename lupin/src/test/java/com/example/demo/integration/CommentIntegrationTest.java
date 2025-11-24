package com.example.demo.integration;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.CommentCreateRequest;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CommentCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("댓글 통합 테스트")
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "TESTCONTAINERS_ENABLED", matches = "true")
class CommentIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        registry.add("app.seed-data.enabled", () -> "false");
    }

    @Autowired
    private CommentCommandService commentCommandService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private CommentRepository commentRepository;

    private User user;
    private User feedWriter;
    private Feed feed;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        feedRepository.deleteAll();
        userRepository.deleteAll();

        feedWriter = userRepository.save(User.builder()
                .userId("writer01")
                .email("writer@test.com")
                .password("password")
                .realName("피드작성자")
                .role(Role.MEMBER)
                .gender("남성")
                .birthDate(LocalDate.of(1990, 1, 1))
                .height(175.0)
                .weight(70.0)
                .currentPoints(0L)
                .monthlyPoints(0L)
                .monthlyLikes(0L)
                .build());

        user = userRepository.save(User.builder()
                .userId("user01")
                .email("user@test.com")
                .password("password")
                .realName("테스트유저")
                .role(Role.MEMBER)
                .gender("남성")
                .birthDate(LocalDate.of(1995, 5, 5))
                .height(180.0)
                .weight(75.0)
                .currentPoints(0L)
                .monthlyPoints(0L)
                .monthlyLikes(0L)
                .build());

        feed = feedRepository.save(Feed.builder()
                .content("테스트 피드")
                .activityType("러닝")
                .calories(200.0)
                .writer(feedWriter)
                .build());
    }

    @Test
    @DisplayName("실제 DB에 댓글 저장 테스트")
    void createComment_SavesToDB() {
        // given
        CommentCreateRequest request = CommentCreateRequest.builder()
                .content("통합 테스트 댓글")
                .build();

        // when
        Long commentId = commentCommandService.createComment(feed.getId(), user.getId(), request);

        // then
        assertThat(commentId).isNotNull();
        assertThat(commentRepository.findById(commentId)).isPresent();
        assertThat(commentRepository.findById(commentId).get().getContent())
                .isEqualTo("통합 테스트 댓글");
    }

    @Test
    @DisplayName("댓글 수정이 DB에 반영되는지 테스트")
    void updateComment_UpdatesInDB() {
        // given
        CommentCreateRequest request = CommentCreateRequest.builder()
                .content("원본 댓글")
                .build();
        Long commentId = commentCommandService.createComment(feed.getId(), user.getId(), request);

        // when
        commentCommandService.updateComment(commentId, user.getId(), "수정된 댓글");

        // then
        assertThat(commentRepository.findById(commentId).get().getContent())
                .isEqualTo("수정된 댓글");
    }

    @Test
    @DisplayName("댓글 삭제가 DB에서 제거되는지 테스트")
    void deleteComment_RemovesFromDB() {
        // given
        CommentCreateRequest request = CommentCreateRequest.builder()
                .content("삭제될 댓글")
                .build();
        Long commentId = commentCommandService.createComment(feed.getId(), user.getId(), request);

        // when
        commentCommandService.deleteComment(commentId, user.getId());

        // then
        assertThat(commentRepository.findById(commentId)).isEmpty();
    }
}
