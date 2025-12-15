package com.example.demo.integration;

import com.example.demo.config.TestRedisConfig;
import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.CommentRequest;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestRedisConfig.class)
class CommentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private CommentService commentService;

    private User testUser;
    private Feed testFeed;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId("testuser")
                .password("password")
                .name("Test User")
                .role(Role.MEMBER)
                .build();
        userRepository.save(testUser);

        testFeed = Feed.builder()
                .writer(testUser)
                .content("Test Feed")
                .activity("running")
                .points(10)
                .calories(100)
                .build();
        feedRepository.save(testFeed);
    }

    @Test
    @WithUserDetails(value = "testuser", userDetailsServiceBeanName = "userDetailsService")
    @DisplayName("댓글 좋아요 통합 테스트")
    void likeCommentIntegrationTest() throws Exception {
        // given
        CommentRequest commentRequest = new CommentRequest("Test Comment", null);
        Comment comment = commentService.create(testUser, testFeed.getId(), Optional.ofNullable(commentRequest.getParentId()), commentRequest.getContent());

        // when: 좋아요
        mockMvc.perform(post("/api/comments/" + comment.getId() + "/like")
                        .with(csrf()))
                .andExpect(status().isOk());

        // then: 좋아요 확인
        assertThat(commentLikeRepository.existsByUserIdAndCommentId(testUser.getId(), comment.getId())).isTrue();
    }
}