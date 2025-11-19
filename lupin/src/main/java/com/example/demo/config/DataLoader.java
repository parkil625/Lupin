package com.example.demo.config;

import com.example.demo.domain.entity.*;
import com.example.demo.domain.enums.ImageType;
import com.example.demo.domain.enums.Role;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 개발 환경에서 샘플 데이터를 자동으로 생성하는 DataLoader
 * application.yml에서 app.seed-data.enabled: true로 활성화
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-data.enabled", havingValue = "true", matchIfMissing = false)
public class DataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final FeedRepository feedRepository;
    private final CommentRepository commentRepository;
    private final ChallengeRepository challengeRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=== 샘플 데이터 생성 시작 ===");

        // 사용자 생성
        User lupin = createUser("김루핀", "lupin@example.com", "개발팀", Role.MEMBER);
        User chulsoo = createUser("이철수", "chulsoo@example.com", "개발팀", Role.MEMBER);
        User younghee = createUser("박영희", "younghee@example.com", "기획팀", Role.MEMBER);
        User minsoo = createUser("최민수", "minsoo@example.com", "영업팀", Role.MEMBER);
        User sujin = createUser("정수진", "sujin@example.com", "디자인팀", Role.MEMBER);

        log.info("사용자 {} 명 생성 완료", 5);

        // 피드 생성 - 김루핀
        Feed feed1 = createFeed(
                lupin,
                "헬스 운동",
                60,
                450.0,
                "오늘 스쿼트 100kg 달성! 💪 꾸준히 해온 결과가 드디어 나타나네요. 작년에는 80kg도 힘들었는데 정말 뿌듯합니다!",
                "{\"strength\":\"+15\",\"endurance\":\"+10\",\"calories\":\"450kcal\"}",
                new String[]{
                        "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800",
                        "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800"
                },
                45,
                8
        );

        Feed feed2 = createFeed(
                lupin,
                "러닝",
                30,
                320.0,
                "아침 러닝 5km 완주! ☀️ 날씨가 좋아서 기분도 최고입니다.",
                "{\"cardio\":\"+20\",\"calories\":\"320kcal\"}",
                new String[]{
                        "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800",
                        "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=800"
                },
                32,
                5
        );

        Feed feed3 = createFeed(
                lupin,
                "요가",
                45,
                180.0,
                "요가로 하루 시작 🧘‍♀️ 몸과 마음이 한결 가벼워진 느낌!",
                "{\"flexibility\":\"+25\",\"mindfulness\":\"+30\",\"calories\":\"180kcal\"}",
                new String[]{"https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800"},
                28,
                4
        );

        // 피드 생성 - 이철수
        Feed feed4 = createFeed(
                chulsoo,
                "헬스 운동",
                60,
                480.0,
                "오늘도 데드리프트 120kg 성공! 💪 작년 이맘때는 80kg도 힘들었는데... 꾸준함이 정말 중요하다는 걸 느낍니다. 모두 파이팅!",
                "{\"strength\":\"+15\",\"endurance\":\"+10\",\"calories\":\"480kcal\"}",
                new String[]{
                        "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800",
                        "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800",
                        "https://images.unsplash.com/photo-1571902943202-507ec2618e8f?w=800"
                },
                124,
                23
        );

        // 피드 생성 - 박영희
        Feed feed5 = createFeed(
                younghee,
                "아침 러닝",
                45,
                520.0,
                "한강 러닝 10km 완주 ☀️ 아침 공기가 정말 상쾌했어요. 오늘 하루도 화이팅!",
                "{\"cardio\":\"+20\",\"calories\":\"520kcal\"}",
                new String[]{
                        "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=800",
                        "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=800"
                },
                89,
                15
        );

        // 피드 생성 - 최민수
        Feed feed6 = createFeed(
                minsoo,
                "요가 클래스",
                50,
                200.0,
                "빈야사 플로우 클래스 완료! 🧘‍♂️ 몸과 마음이 한결 가벼워진 느낌. 스트레스 해소에 최고예요.",
                "{\"flexibility\":\"+25\",\"mindfulness\":\"+30\",\"calories\":\"200kcal\"}",
                new String[]{"https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800"},
                67,
                12
        );

        log.info("피드 {} 개 생성 완료", 6);

        // 댓글 생성
        createComment(feed4, younghee, "대단해요! 👍", null);
        createComment(feed4, chulsoo, "저도 열심히 해야겠어요", null);
        createComment(feed4, minsoo, "응원합니다! 💪", null);
        createComment(feed5, lupin, "멋져요!", null);
        createComment(feed6, sujin, "저도 요가 시작해볼까요?", null);

        log.info("댓글 {} 개 생성 완료", 5);

        // 챌린지 생성
        Challenge challenge1 = createChallenge(
                "30일 플랭크 챌린지",
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().plusDays(25),
                100
        );

        Challenge challenge2 = createChallenge(
                "주 3회 러닝",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().plusDays(28),
                50
        );

        log.info("챌린지 {} 개 생성 완료", 2);

        // 알림 생성
        createNotification(lupin, "like", "새로운 좋아요", younghee.getName() + "님이 회원님의 게시물을 좋아합니다.", feed1.getId());
        createNotification(lupin, "comment", "새로운 댓글", chulsoo.getName() + "님이 회원님의 게시물에 댓글을 남겼습니다.", null);
        createNotification(chulsoo, "challenge", "챌린지 시작", "30일 플랭크 챌린지가 시작되었습니다!", challenge1.getId());

        log.info("알림 {} 개 생성 완료", 3);

        // 포인트 추가
        lupin.addPoints(138L);
        younghee.addPoints(480L);
        chulsoo.addPoints(520L);
        minsoo.addPoints(450L);

        log.info("=== 샘플 데이터 생성 완료 ===");
    }

    private User createUser(String name, String email, String department, Role role) {
        User user = User.builder()
                .realName(name)
                .email(email)
                .password("password123") // 실제로는 암호화 필요
                .role(role)
                .department(department)
                .currentPoints(0L)
                .totalPoints(0L)
                .build();
        return userRepository.save(user);
    }

    private Feed createFeed(User writer, String activityType, int duration, double calories,
                            String content, String statsJson, String[] imageUrls,
                            int likes, int comments) {
        Feed feed = Feed.builder()
                .activityType(activityType)
                .duration(duration)
                .calories(calories)
                .content(content)
                .statsJson(statsJson)
                .startedAt(LocalDateTime.now().minusHours(3))
                .build();

        feed.setWriter(writer);

        // 이미지 추가
        for (int i = 0; i < imageUrls.length; i++) {
            ImageType imageType = i == 0 ? ImageType.START :
                    i == 1 ? ImageType.END : ImageType.OTHER;

            FeedImage feedImage = FeedImage.builder()
                    .imageUrl(imageUrls[i])
                    .imgType(imageType)
                    .sortOrder(i)
                    .takenAt(LocalDateTime.now())
                    .build();

            feed.addImage(feedImage);
        }

        return feedRepository.save(feed);
    }

    private Comment createComment(Feed feed, User writer, String content, Comment parent) {
        Comment comment = Comment.builder()
                .content(content)
                .writer(writer)
                .parent(parent)
                .build();

        comment.setFeed(feed);

        return commentRepository.save(comment);
    }

    private Challenge createChallenge(String title, LocalDateTime opensAt,
                                      LocalDateTime closesAt, Integer maxWinners) {
        Challenge challenge = Challenge.builder()
                .title(title)
                .opensAt(opensAt)
                .closesAt(closesAt)
                .maxWinners(maxWinners)
                .build();

        return challengeRepository.save(challenge);
    }

    private Notification createNotification(User user, String type, String title,
                                            String content, Long relatedId) {
        Notification notification = Notification.builder()
                .type(type)
                .title(title)
                .content(content)
                .relatedId(relatedId)
                .build();

        notification.setUser(user);

        return notificationRepository.save(notification);
    }
}
