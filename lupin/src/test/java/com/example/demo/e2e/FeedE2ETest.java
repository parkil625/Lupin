package com.example.demo.e2e;

import com.example.demo.config.TestRedisConfiguration;
import com.example.demo.domain.entity.*;
import com.example.demo.domain.enums.NotificationType;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.CommentRequest;
import com.example.demo.dto.request.FeedRequest;
import com.example.demo.repository.*;
import com.example.demo.service.ImageMetadataService;
import com.example.demo.service.WorkoutScoreService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@Transactional
class FeedE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private FeedLikeRepository feedLikeRepository;

    @Autowired
    private FeedImageRepository feedImageRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private ImageMetadataService imageMetadataService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private WorkoutScoreService workoutScoreService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Mock 설정: EXIF 시간 반환
        java.time.LocalDateTime startTime = java.time.LocalDateTime.now().minusHours(1);
        java.time.LocalDateTime endTime = java.time.LocalDateTime.now();

        org.mockito.Mockito.when(imageMetadataService.extractPhotoDateTime(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.Optional.of(startTime))
                .thenReturn(java.util.Optional.of(endTime));

        org.mockito.Mockito.when(workoutScoreService.calculateScore(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class)))
                .thenReturn(10);

        org.mockito.Mockito.when(workoutScoreService.calculateCalories(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class)))
                .thenReturn(100);

        testUser = userRepository.save(User.builder()
                .userId("testuser")
                .password("password")
                .name("테스트유저")
                .role(Role.MEMBER)
                .build());
    }

    @Test
    @DisplayName("E2E: 피드 작성 → 좋아요 → 댓글 → 대댓글 → 삭제 전체 흐름")
    @WithMockUser(username = "testuser")
    void feedLifecycle_FullFlow() throws Exception {
        // 1. 피드 작성 (startImage, endImage 타입별로 지정)
        FeedRequest feedRequest = FeedRequest.builder()
                .activity("RUNNING")
                .content("오늘 5km 달리기 완료!")
                .startImage("run1.jpg")
                .endImage("run2.jpg")
                .build();

        mockMvc.perform(post("/api/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("오늘 5km 달리기 완료!"));

        Feed savedFeed = feedRepository.findAll().get(0);
        Long feedId = savedFeed.getId();

        // 2. 피드에 이미지 저장 확인 (cascade로 저장되므로 feed.getImages() 확인)
        assertThat(savedFeed.getImages()).hasSize(2);

        // 3. 피드 좋아요
        mockMvc.perform(post("/api/feeds/" + feedId + "/like"))
                .andExpect(status().isOk());

        assertThat(feedLikeRepository.countByFeed(savedFeed)).isEqualTo(1);

        // 4. 댓글 작성
        CommentRequest commentRequest = new CommentRequest("멋진 기록이네요!");

        mockMvc.perform(post("/api/feeds/" + feedId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("멋진 기록이네요!"));

        Comment savedComment = commentRepository.findAll().get(0);
        Long commentId = savedComment.getId();

        // 5. 댓글 좋아요
        mockMvc.perform(post("/api/comments/" + commentId + "/like"))
                .andExpect(status().isOk());

        assertThat(commentLikeRepository.countByComment(savedComment)).isEqualTo(1);

        // 6. 대댓글 작성
        CommentRequest replyRequest = new CommentRequest("감사합니다!");

        mockMvc.perform(post("/api/comments/" + commentId + "/replies")
                        .param("feedId", feedId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("감사합니다!"));

        assertThat(commentRepository.count()).isEqualTo(2);

        // 7. 댓글 목록 조회 (부모 댓글만)
        mockMvc.perform(get("/api/feeds/" + feedId + "/comments"))
                .andExpect(status().isOk());

        // 8. 대댓글 목록 조회
        mockMvc.perform(get("/api/comments/" + commentId + "/replies"))
                .andExpect(status().isOk());

        // 9. 피드 수정
        FeedRequest updateRequest = FeedRequest.builder()
                .activity("RUNNING")
                .content("오늘 10km 달리기 완료! (수정)")
                .build();

        mockMvc.perform(put("/api/feeds/" + feedId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("오늘 10km 달리기 완료! (수정)"));

        // 10. 피드 삭제
        mockMvc.perform(delete("/api/feeds/" + feedId))
                .andExpect(status().isOk());

        // 11. 삭제 확인
        assertThat(feedRepository.findById(feedId)).isEmpty();
    }

    @Test
    @DisplayName("E2E: 내 피드 목록 조회 → 페이징 확인")
    @WithMockUser(username = "testuser")
    void myFeeds_Pagination() throws Exception {
        // 1. 피드 5개 생성
        for (int i = 1; i <= 5; i++) {
            feedRepository.save(Feed.builder()
                    .writer(testUser)
                    .activity("RUNNING")
                    .content("피드 " + i)
                    .build());
        }

        // 2. 첫 페이지 조회 (size=2)
        mockMvc.perform(get("/api/feeds/my")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));

        // 3. 두 번째 페이지 조회
        mockMvc.perform(get("/api/feeds/my")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));

        // 4. 마지막 페이지 조회
        mockMvc.perform(get("/api/feeds/my")
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("E2E: 좋아요 토글 (좋아요 → 취소 → 다시 좋아요)")
    @WithMockUser(username = "testuser")
    void likeToggle_Flow() throws Exception {
        // 1. 피드 생성
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("좋아요 테스트")
                .build());

        // 2. 좋아요
        mockMvc.perform(post("/api/feeds/" + feed.getId() + "/like"))
                .andExpect(status().isOk());
        assertThat(feedLikeRepository.countByFeed(feed)).isEqualTo(1);

        // 3. 좋아요 취소
        mockMvc.perform(delete("/api/feeds/" + feed.getId() + "/like"))
                .andExpect(status().isOk());
        assertThat(feedLikeRepository.countByFeed(feed)).isEqualTo(0);

        // 4. 다시 좋아요
        mockMvc.perform(post("/api/feeds/" + feed.getId() + "/like"))
                .andExpect(status().isOk());
        assertThat(feedLikeRepository.countByFeed(feed)).isEqualTo(1);
    }

    @Test
    @DisplayName("E2E: 댓글 CRUD 전체 흐름")
    @WithMockUser(username = "testuser")
    void commentCrud_FullFlow() throws Exception {
        // 1. 피드 생성
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("테스트 피드")
                .build());

        // 2. 댓글 작성
        CommentRequest createRequest = new CommentRequest("첫 번째 댓글");

        mockMvc.perform(post("/api/feeds/" + feed.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("첫 번째 댓글"));

        Comment savedComment = commentRepository.findAll().get(0);

        // 3. 댓글 수정
        CommentRequest updateRequest = new CommentRequest("수정된 댓글");

        mockMvc.perform(put("/api/comments/" + savedComment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정된 댓글"));

        // 4. 댓글 좋아요
        mockMvc.perform(post("/api/comments/" + savedComment.getId() + "/like"))
                .andExpect(status().isOk());

        assertThat(commentLikeRepository.countByComment(savedComment)).isEqualTo(1);

        // 5. 댓글 삭제
        mockMvc.perform(delete("/api/comments/" + savedComment.getId()))
                .andExpect(status().isOk());

        assertThat(commentRepository.findById(savedComment.getId())).isEmpty();
    }

    @Test
    @DisplayName("E2E: 대댓글 작성 및 조회 흐름")
    @WithMockUser(username = "testuser")
    void replyCreateAndRetrieve() throws Exception {
        // 1. 피드 생성
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("테스트 피드")
                .build());

        // 2. 부모 댓글 작성
        Comment parentComment = commentRepository.save(Comment.builder()
                .writer(testUser)
                .feed(feed)
                .content("부모 댓글")
                .build());

        // 3. 대댓글 작성
        CommentRequest replyRequest = new CommentRequest("대댓글입니다");

        mockMvc.perform(post("/api/comments/" + parentComment.getId() + "/replies")
                        .param("feedId", feed.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("대댓글입니다"));

        // 4. 대댓글 저장 확인
        assertThat(commentRepository.count()).isEqualTo(2);

        // 5. 대댓글 목록 조회
        mockMvc.perform(get("/api/comments/" + parentComment.getId() + "/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("대댓글입니다"));
    }

    // ========== 피드 조회 시나리오 ==========

    @Test
    @DisplayName("E2E: 피드 상세 조회")
    @WithMockUser(username = "testuser")
    void getFeedDetail() throws Exception {
        // given
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("상세 조회 테스트")
                .build());

        // when & then
        mockMvc.perform(get("/api/feeds/" + feed.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("상세 조회 테스트"))
                .andExpect(jsonPath("$.activity").value("RUNNING"));
    }

    @Test
    @DisplayName("E2E: 홈 피드 조회 (페이징)")
    @WithMockUser(username = "testuser")
    void getHomeFeeds() throws Exception {
        // given - 다른 사용자가 피드 3개 생성 (홈 피드는 본인 제외)
        User otherUser = userRepository.save(User.builder()
                .userId("otheruser")
                .password("password")
                .name("다른유저")
                .role(Role.MEMBER)
                .build());

        for (int i = 1; i <= 3; i++) {
            feedRepository.save(Feed.builder()
                    .writer(otherUser)
                    .activity("RUNNING")
                    .content("홈 피드 " + i)
                    .build());
        }

        // when & then
        mockMvc.perform(get("/api/feeds")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    @DisplayName("E2E: 오늘 포스팅 가능 여부 확인")
    @WithMockUser(username = "testuser")
    void canPostToday() throws Exception {
        // 1. 처음에는 포스팅 가능
        mockMvc.perform(get("/api/feeds/can-post-today"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // 2. 피드 작성 (startImage, endImage 타입별로 지정)
        FeedRequest request = FeedRequest.builder()
                .activity("RUNNING")
                .content("오늘의 피드")
                .startImage("start.jpg")
                .endImage("end.jpg")
                .build();

        mockMvc.perform(post("/api/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 3. 이제 포스팅 불가능
        mockMvc.perform(get("/api/feeds/can-post-today"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // ========== 에러 시나리오 ==========

    @Test
    @DisplayName("E2E: 존재하지 않는 피드 조회 시 404")
    @WithMockUser(username = "testuser")
    void getFeedNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/feeds/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("E2E: 존재하지 않는 피드에 좋아요 시 404")
    @WithMockUser(username = "testuser")
    void likeFeedNotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/feeds/99999/like"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("E2E: 존재하지 않는 피드에 댓글 작성 시 404")
    @WithMockUser(username = "testuser")
    void commentOnNonExistentFeed_Returns404() throws Exception {
        CommentRequest request = new CommentRequest("댓글");

        mockMvc.perform(post("/api/feeds/99999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("E2E: 존재하지 않는 댓글 수정 시 404")
    @WithMockUser(username = "testuser")
    void updateNonExistentComment_Returns404() throws Exception {
        CommentRequest request = new CommentRequest("수정");

        mockMvc.perform(put("/api/comments/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("E2E: 존재하지 않는 댓글 삭제 시 404")
    @WithMockUser(username = "testuser")
    void deleteNonExistentComment_Returns404() throws Exception {
        mockMvc.perform(delete("/api/comments/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("E2E: 존재하지 않는 피드 수정 시 404")
    @WithMockUser(username = "testuser")
    void updateNonExistentFeed_Returns404() throws Exception {
        FeedRequest request = FeedRequest.builder()
                .activity("WALKING")
                .content("수정")
                .build();

        mockMvc.perform(put("/api/feeds/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("E2E: 존재하지 않는 피드 삭제 시 404")
    @WithMockUser(username = "testuser")
    void deleteNonExistentFeed_Returns404() throws Exception {
        mockMvc.perform(delete("/api/feeds/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("E2E: 중복 좋아요 시 400")
    @WithMockUser(username = "testuser")
    void duplicateLike_Returns400() throws Exception {
        // given
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("테스트")
                .build());

        // 첫 번째 좋아요
        mockMvc.perform(post("/api/feeds/" + feed.getId() + "/like"))
                .andExpect(status().isOk());

        // 두 번째 좋아요 (중복)
        mockMvc.perform(post("/api/feeds/" + feed.getId() + "/like"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("E2E: 좋아요 안 했는데 취소 시 404")
    @WithMockUser(username = "testuser")
    void unlikeWithoutLike_Returns404() throws Exception {
        // given
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("테스트")
                .build());

        // 좋아요 하지 않은 상태에서 취소 시도
        mockMvc.perform(delete("/api/feeds/" + feed.getId() + "/like"))
                .andExpect(status().isNotFound());
    }

    // ========== 댓글 좋아요 시나리오 ==========

    @Test
    @DisplayName("E2E: 댓글 좋아요 토글")
    @WithMockUser(username = "testuser")
    void commentLikeToggle() throws Exception {
        // given
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("테스트")
                .build());

        Comment comment = commentRepository.save(Comment.builder()
                .writer(testUser)
                .feed(feed)
                .content("댓글")
                .build());

        // 1. 좋아요
        mockMvc.perform(post("/api/comments/" + comment.getId() + "/like"))
                .andExpect(status().isOk());
        assertThat(commentLikeRepository.countByComment(comment)).isEqualTo(1);

        // 2. 좋아요 취소
        mockMvc.perform(delete("/api/comments/" + comment.getId() + "/like"))
                .andExpect(status().isOk());
        assertThat(commentLikeRepository.countByComment(comment)).isEqualTo(0);

        // 3. 다시 좋아요
        mockMvc.perform(post("/api/comments/" + comment.getId() + "/like"))
                .andExpect(status().isOk());
        assertThat(commentLikeRepository.countByComment(comment)).isEqualTo(1);
    }

    @Test
    @DisplayName("E2E: 댓글 중복 좋아요 시 400")
    @WithMockUser(username = "testuser")
    void duplicateCommentLike_Returns400() throws Exception {
        // given
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("테스트")
                .build());

        Comment comment = commentRepository.save(Comment.builder()
                .writer(testUser)
                .feed(feed)
                .content("댓글")
                .build());

        // 첫 번째 좋아요
        mockMvc.perform(post("/api/comments/" + comment.getId() + "/like"))
                .andExpect(status().isOk());

        // 두 번째 좋아요 (중복)
        mockMvc.perform(post("/api/comments/" + comment.getId() + "/like"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("E2E: 댓글 좋아요 안 했는데 취소 시 404")
    @WithMockUser(username = "testuser")
    void unlikeCommentWithoutLike_Returns404() throws Exception {
        // given
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("테스트")
                .build());

        Comment comment = commentRepository.save(Comment.builder()
                .writer(testUser)
                .feed(feed)
                .content("댓글")
                .build());

        // 좋아요 하지 않은 상태에서 취소 시도
        mockMvc.perform(delete("/api/comments/" + comment.getId() + "/like"))
                .andExpect(status().isNotFound());
    }

    // ========== 신고 시나리오 ==========

    @Test
    @DisplayName("E2E: 피드 신고 토글")
    @WithMockUser(username = "testuser")
    void feedReportToggle() throws Exception {
        // given
        Feed feed = feedRepository.save(Feed.builder()
                .writer(testUser)
                .activity("RUNNING")
                .content("신고 테스트")
                .build());

        // 1. 신고
        mockMvc.perform(post("/api/feeds/" + feed.getId() + "/report"))
                .andExpect(status().isOk());

        // 2. 신고 취소 (토글)
        mockMvc.perform(post("/api/feeds/" + feed.getId() + "/report"))
                .andExpect(status().isOk());
    }

    // ========== 알림 시나리오 ==========

    @Test
    @DisplayName("E2E: 알림 조회 및 읽음 처리")
    @WithMockUser(username = "testuser")
    void notificationFlow() throws Exception {
        // given - 알림 생성
        Notification notification = notificationRepository.save(Notification.builder()
                .user(testUser)
                .type(NotificationType.COMMENT)
                .title("테스트 알림")
                .build());

        // 1. 알림 목록 조회
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 2. 읽지 않은 알림 존재 확인
        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasUnread").value(true));

        // 3. 알림 읽음 처리
        mockMvc.perform(patch("/api/notifications/" + notification.getId() + "/read"))
                .andExpect(status().isOk());

        // 4. 읽지 않은 알림 없음 확인
        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasUnread").value(false));
    }

    @Test
    @DisplayName("E2E: 전체 알림 읽음 처리")
    @WithMockUser(username = "testuser")
    void markAllNotificationsAsRead() throws Exception {
        // given - 알림 3개 생성
        for (int i = 1; i <= 3; i++) {
            notificationRepository.save(Notification.builder()
                    .user(testUser)
                    .type(NotificationType.COMMENT)
                    .title("알림 " + i)
                    .build());
        }

        // 1. 읽지 않은 알림 존재 확인
        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasUnread").value(true));

        // 2. 전체 읽음 처리
        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isOk());

        // 3. 읽지 않은 알림 없음 확인
        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasUnread").value(false));
    }

    @Test
    @DisplayName("E2E: 알림 삭제")
    @WithMockUser(username = "testuser")
    void deleteNotification() throws Exception {
        // given
        Notification notification = notificationRepository.save(Notification.builder()
                .user(testUser)
                .type(NotificationType.COMMENT)
                .title("삭제할 알림")
                .build());

        // when
        mockMvc.perform(delete("/api/notifications/" + notification.getId()))
                .andExpect(status().isOk());

        // then
        assertThat(notificationRepository.findById(notification.getId())).isEmpty();
    }

    // ========== 복합 시나리오 ==========

    @Test
    @DisplayName("E2E: 여러 사용자 상호작용 시뮬레이션")
    @WithMockUser(username = "testuser")
    void multiUserInteraction() throws Exception {
        // 1. 피드 작성 (startImage, endImage 타입별로 지정)
        FeedRequest feedRequest = FeedRequest.builder()
                .activity("RUNNING")
                .content("다중 사용자 테스트")
                .startImage("start.jpg")
                .endImage("end.jpg")
                .build();

        mockMvc.perform(post("/api/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedRequest)))
                .andExpect(status().isOk());

        Feed feed = feedRepository.findAll().get(0);

        // 2. 여러 댓글 작성
        for (int i = 1; i <= 3; i++) {
            CommentRequest commentRequest = new CommentRequest("댓글 " + i);
            mockMvc.perform(post("/api/feeds/" + feed.getId() + "/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isOk());
        }

        // 3. 댓글 목록 확인
        mockMvc.perform(get("/api/feeds/" + feed.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        // 4. 각 댓글에 좋아요
        List<Comment> comments = commentRepository.findAll();
        for (Comment comment : comments) {
            mockMvc.perform(post("/api/comments/" + comment.getId() + "/like"))
                    .andExpect(status().isOk());
        }

        // 5. 피드에 좋아요
        mockMvc.perform(post("/api/feeds/" + feed.getId() + "/like"))
                .andExpect(status().isOk());

        // 6. 피드 조회로 상태 확인
        mockMvc.perform(get("/api/feeds/" + feed.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1))
                .andExpect(jsonPath("$.comments").value(3));
    }

    @Test
    @DisplayName("E2E: 이미지 없는 피드 생성 시 400 에러")
    @WithMockUser(username = "testuser")
    void createFeedWithoutImages_Returns400() throws Exception {
        FeedRequest request = FeedRequest.builder()
                .activity("WALKING")
                .content("이미지 없는 피드")
                .build();

        // 이미지가 없으면 BAD_REQUEST 반환 (시작/끝 사진 필수)
        mockMvc.perform(post("/api/feeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
