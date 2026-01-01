package com.example.demo.integration;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.CommentRequest;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@Transactional
class CommentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    // [추가]
    @Autowired
    private jakarta.persistence.EntityManager em;

    private User testUser;
    private User otherUser;
    private Feed testFeed;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .userId("testuser")
                .password("password")
                .name("테스트유저")
                .role(Role.MEMBER)
                .build());

        otherUser = userRepository.save(User.builder()
                .userId("otheruser")
                .password("password")
                .name("다른유저")
                .role(Role.MEMBER)
                .build());

        testFeed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("테스트 피드")
                .build());
    }

    @Test
    @DisplayName("댓글 생성 → 조회 통합 테스트")
    @WithMockUser(username = "testuser")
    void createAndGetComments() throws Exception {
        // 1. 댓글 생성
        CommentRequest request = new CommentRequest("좋은 운동이네요!");

        mockMvc.perform(post("/api/feeds/" + testFeed.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("좋은 운동이네요!"));

        // 2. DB에 저장 확인
        assertThat(commentRepository.count()).isEqualTo(1);

        // 3. 댓글 목록 조회
        mockMvc.perform(get("/api/feeds/" + testFeed.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("좋은 운동이네요!"));
    }

    @Test
    @DisplayName("댓글 수정 → 삭제 통합 테스트")
    @WithMockUser(username = "testuser")
    void updateAndDeleteComment() throws Exception {
        // given: 댓글 생성
        Comment comment = commentRepository.save(Comment.builder()
                .writer(testUser)
                .feed(testFeed)
                .content("원본 댓글")
                .build());

        // 1. 댓글 수정
        CommentRequest updateRequest = new CommentRequest("수정된 댓글");

        mockMvc.perform(put("/api/comments/" + comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정된 댓글"));

        // 2. 수정 확인
        Comment updated = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(updated.getContent()).isEqualTo("수정된 댓글");

        // 3. 댓글 삭제
        mockMvc.perform(delete("/api/comments/" + comment.getId()))
                .andExpect(status().isOk());

        // [추가] 캐시 초기화
        em.flush();
        em.clear();

        // 4. 삭제 확인
        assertThat(commentRepository.findById(comment.getId())).isEmpty();
    }

    @Test
    @DisplayName("대댓글 생성 → 조회 통합 테스트")
    @WithMockUser(username = "testuser")
    void createAndGetReplies() throws Exception {
        // given: 부모 댓글 생성
        Comment parentComment = commentRepository.save(Comment.builder()
                .writer(testUser)
                .feed(testFeed)
                .content("부모 댓글")
                .build());

        // 1. 대댓글 생성
        CommentRequest replyRequest = new CommentRequest("대댓글입니다");

        mockMvc.perform(post("/api/comments/" + parentComment.getId() + "/replies")
                        .param("feedId", testFeed.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("대댓글입니다"));

        // 2. 대댓글 조회
        mockMvc.perform(get("/api/comments/" + parentComment.getId() + "/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("대댓글입니다"));
    }

    @Test
    @DisplayName("댓글 좋아요 → 취소 통합 테스트")
    @WithMockUser(username = "testuser")
    void likeAndUnlikeComment() throws Exception {
        // given: 댓글 생성
        Comment comment = commentRepository.save(Comment.builder()
                .writer(testUser)
                .feed(testFeed)
                .content("테스트 댓글")
                .build());

        // 1. 좋아요
        mockMvc.perform(post("/api/comments/" + comment.getId() + "/like"))
                .andExpect(status().isOk());

        // 2. 좋아요 확인
        assertThat(commentLikeRepository.existsByUserAndComment(testUser, comment)).isTrue();

        // 3. 좋아요 취소
        mockMvc.perform(delete("/api/comments/" + comment.getId() + "/like"))
                .andExpect(status().isOk());

        // 4. 취소 확인
        assertThat(commentLikeRepository.existsByUserAndComment(testUser, comment)).isFalse();
    }

    @Test
    @DisplayName("다른 사용자의 댓글 수정 시 403 반환")
    @WithMockUser(username = "otheruser")
    void updateCommentByOtherUser_Returns403() throws Exception {
        // given
        Comment comment = commentRepository.save(Comment.builder()
                .writer(testUser)
                .feed(testFeed)
                .content("원본 댓글")
                .build());

        CommentRequest updateRequest = new CommentRequest("수정 시도");

        // when & then
        mockMvc.perform(put("/api/comments/" + comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        // 수정되지 않았는지 확인
        Comment unchanged = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(unchanged.getContent()).isEqualTo("원본 댓글");
    }

    @Test
    @DisplayName("다른 사용자의 댓글 삭제 시 403 반환")
    @WithMockUser(username = "otheruser")
    void deleteCommentByOtherUser_Returns403() throws Exception {
        // given
        Comment comment = commentRepository.save(Comment.builder()
                .writer(testUser)
                .feed(testFeed)
                .content("삭제 대상")
                .build());

        // when & then
        mockMvc.perform(delete("/api/comments/" + comment.getId()))
                .andExpect(status().isForbidden());

        // 삭제되지 않았는지 확인
        assertThat(commentRepository.findById(comment.getId())).isPresent();
    }
}
