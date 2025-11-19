package com.example.demo.config;

import com.example.demo.domain.entity.*;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.request.CommentCreateRequest;
import com.example.demo.dto.request.FeedCreateRequest;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.dto.response.FeedDetailResponse;
import com.example.demo.repository.*;
import com.example.demo.service.CommentService;
import com.example.demo.service.FeedService;
import com.example.demo.service.NotificationService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 테스트 데이터 초기화 - Service 레이어 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FeedRepository feedRepository;
    private final NotificationRepository notificationRepository;
    private final EntityManager entityManager;

    // Service 레이어 주입
    private final FeedService feedService;
    private final CommentService commentService;
    private final NotificationService notificationService;

    // PasswordEncoder 주입
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) {
        // 기존 데이터 삭제 및 재생성 (JWT 인증 시스템 적용)
        if (userRepository.count() > 0) {
            log.info("기존 데이터 삭제 중...");

            // 기존 데이터 삭제
            notificationRepository.deleteAll();
            feedRepository.deleteAll();
            userRepository.deleteAll();

            // AUTO_INCREMENT 초기화
            entityManager.createNativeQuery("ALTER TABLE users AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE feed AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE notification AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE comment AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE feed_like AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE feed_image AUTO_INCREMENT = 1").executeUpdate();

            log.info("기존 데이터 삭제 완료");
        }

        log.info("=== 테스트 데이터 초기화 시작 (Service 레이어 사용 + JWT 인증) ===");

        // 1. 20명의 유저 생성
        List<User> users = createTestUsers();
        log.info("20명의 테스트 유저 생성 완료");

        // 2. 피드 생성 (Service 사용 - 자동으로 포인트 적립)
        List<FeedDetailResponse> feeds = createTestFeeds(users);
        log.info("테스트 피드 생성 완료 (포인트 자동 적립됨)");

        // 3. 댓글 및 대댓글 생성 (Service 사용)
        List<CommentResponse> comments = createTestComments(users, feeds);
        log.info("테스트 댓글 생성 완료");

        // 4. 좋아요 생성 (Service 사용)
        createTestLikes(users, feeds);
        log.info("테스트 좋아요 생성 완료");

        log.info("=== 테스트 데이터 초기화 완료 ===");
    }

    private List<User> createTestUsers() {
        List<User> users = new ArrayList<>();
        String[][] userData = {
            {"user01", "김강민", "남성", "개발팀"},
            {"user02", "이서연", "여성", "마케팅팀"},
            {"user03", "박준호", "남성", "영업팀"},
            {"user04", "최지우", "여성", "디자인팀"},
            {"user05", "정민수", "남성", "인사팀"},
            {"user06", "강혜진", "여성", "재무팀"},
            {"user07", "윤태양", "남성", "법무팀"},
            {"user08", "한소희", "여성", "경영지원팀"},
            {"user09", "오성민", "남성", "연구개발팀"},
            {"user10", "서은주", "여성", "기획팀"},
            {"user11", "임동혁", "남성", "개발팀"},
            {"user12", "배수지", "여성", "마케팅팀"},
            {"user13", "신재호", "남성", "영업팀"},
            {"user14", "조미라", "여성", "디자인팀"},
            {"user15", "홍길동", "남성", "인사팀"},
            {"user16", "안지영", "여성", "재무팀"},
            {"user17", "유재석", "남성", "법무팀"},
            {"user18", "송혜교", "여성", "경영지원팀"},
            {"user19", "전지현", "여성", "연구개발팀"},
            {"user20", "현빈", "남성", "기획팀"}
        };

        // 비밀번호 암호화 (모든 사용자 비밀번호: "1")
        String encodedPassword = passwordEncoder.encode("1");

        for (String[] data : userData) {
            // 이미 존재하는 이메일이면 스킵
            if (userRepository.findByEmail(data[0]).isPresent()) {
                User existingUser = userRepository.findByEmail(data[0]).get();
                users.add(existingUser);
                continue;
            }

            // 점수는 0으로 시작 (피드 작성 시 자동 적립)
            User user = User.builder()
                    .email(data[0])
                    .password(encodedPassword)  // BCrypt로 암호화된 비밀번호
                    .realName(data[1])
                    .role(Role.MEMBER)
                    .height(165.0 + random.nextDouble() * 20)
                    .weight(50.0 + random.nextDouble() * 30)
                    .gender(data[2])
                    .birthDate(LocalDate.of(1990 + random.nextInt(10), 1 + random.nextInt(12), 1 + random.nextInt(28)))
                    .currentPoints(0L)
                    .totalPoints(0L)
                    .department(data[3])
                    .build();
            users.add(userRepository.save(user));
        }

        return users;
    }

    private List<FeedDetailResponse> createTestFeeds(List<User> users) {
        List<FeedDetailResponse> feeds = new ArrayList<>();

        // 각 유저당 하루 1개의 피드만 생성 (일일 제한 준수)
        String[] activities = {"러닝", "걷기", "자전거", "수영", "등산", "요가"};
        String[] activityImages = {
                "https://picsum.photos/seed/running/800/600", // 러닝
                "https://picsum.photos/seed/walking/800/600", // 걷기
                "https://picsum.photos/seed/cycling/800/600", // 자전거
                "https://picsum.photos/seed/swimming/800/600", // 수영
                "https://picsum.photos/seed/hiking/800/600", // 등산
                "https://picsum.photos/seed/yoga/800/600"  // 요가
        };

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            int duration = 20 + random.nextInt(60); // 20-80분
            int activityIndex = i % activities.length;
            int endImageIndex = (activityIndex + 1) % activityImages.length;

            // 이미지 URL (START, END)
            List<String> imageUrls = Arrays.asList(
                activityImages[activityIndex],
                activityImages[endImageIndex]
            );

            FeedCreateRequest request = FeedCreateRequest.builder()
                    .activityType(activities[activityIndex])
                    .duration(duration)
                    .calories((double)(100 + random.nextInt(300)))
                    .content(String.format("%s 운동 완료! 오늘도 건강하게 💪", activities[activityIndex]))
                    .imageUrls(imageUrls)
                    .build();

            // Service를 통해 피드 생성 (자동으로 포인트 적립)
            FeedDetailResponse savedFeed = feedService.createFeed(user.getId(), request);
            feeds.add(savedFeed);
        }

        return feeds;
    }

    private List<CommentResponse> createTestComments(List<User> users, List<FeedDetailResponse> feeds) {
        List<CommentResponse> comments = new ArrayList<>();

        // 각 피드에 댓글 추가
        for (FeedDetailResponse feed : feeds) {
            int commentCount = random.nextInt(5); // 0-4개의 댓글

            for (int i = 0; i < commentCount; i++) {
                User commenter = users.get(random.nextInt(users.size()));

                CommentCreateRequest request = CommentCreateRequest.builder()
                        .content("대단하세요! 저도 같이 운동하고 싶네요 👍")
                        .build();

                // Service를 통해 댓글 생성
                CommentResponse savedComment = commentService.createComment(feed.getId(), commenter.getId(), request);
                comments.add(savedComment);

                // 알림 생성 (피드 작성자에게)
                notificationService.createCommentNotification(
                    feed.getWriterId(),
                    commenter.getId(),
                    feed.getId(),
                    savedComment.getId()
                );

                // 대댓글 추가 (50% 확률)
                if (random.nextBoolean()) {
                    User replier = users.get(random.nextInt(users.size()));

                    CommentCreateRequest replyRequest = CommentCreateRequest.builder()
                            .content("감사합니다! 함께 화이팅해요 💪")
                            .parentId(savedComment.getId())
                            .build();

                    CommentResponse reply = commentService.createComment(feed.getId(), replier.getId(), replyRequest);
                    comments.add(reply);

                    // 답글 알림 생성 (원댓글 작성자에게)
                    if (!savedComment.getWriterId().equals(replier.getId())) {
                        notificationService.createCommentNotification(
                            savedComment.getWriterId(),
                            replier.getId(),
                            feed.getId(),
                            reply.getId()
                        );
                    }
                }
            }
        }

        return comments;
    }

    private void createTestLikes(List<User> users, List<FeedDetailResponse> feeds) {
        // 각 피드에 좋아요 추가
        for (FeedDetailResponse feed : feeds) {
            int likeCount = random.nextInt(10); // 0-9개의 좋아요
            List<Long> likedUserIds = new ArrayList<>();

            for (int i = 0; i < likeCount && i < users.size(); i++) {
                User liker = users.get(random.nextInt(users.size()));

                // 이미 좋아요를 누른 사용자인지 확인
                if (!likedUserIds.contains(liker.getId())) {
                    try {
                        // Service를 통해 좋아요 추가
                        feedService.likeFeed(feed.getId(), liker.getId());
                        likedUserIds.add(liker.getId());

                        // 알림 생성 (피드 작성자에게)
                        notificationService.createLikeNotification(
                            feed.getWriterId(),
                            liker.getId(),
                            feed.getId()
                        );
                    } catch (Exception e) {
                        // 이미 좋아요를 누른 경우 무시
                        log.debug("좋아요 중복: feedId={}, userId={}", feed.getId(), liker.getId());
                    }
                }
            }
        }
    }
}
