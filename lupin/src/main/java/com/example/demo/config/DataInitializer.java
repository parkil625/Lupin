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
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    private final LotteryTicketRepository lotteryTicketRepository;
    private final com.example.demo.repository.ChatMessageRepository chatMessageRepository;
    private final com.example.demo.repository.PrizeClaimRepository prizeClaimRepository;
    private final com.example.demo.repository.ReportRepository reportRepository;
    private final EntityManager entityManager;

    // Service 레이어 주입
    private final FeedService feedService;
    private final CommentService commentService;

    // Repository for comment likes
    private final com.example.demo.repository.CommentLikeRepository commentLikeRepository;

    // PasswordEncoder 주입
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.seed-data.enabled:false}")
    private boolean seedDataEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedDataEnabled) {
            log.info("시드 데이터 생성이 비활성화되어 있습니다. (app.seed-data.enabled=false)");
            return;
        }

        // 기존 데이터 삭제 및 재생성 (JWT 인증 시스템 적용)
        if (userRepository.count() > 0) {
            log.info("기존 데이터 삭제 중...");

            // 기존 데이터 삭제 (순서 중요: 외래키 참조 순서대로)
            notificationRepository.deleteAll();
            lotteryTicketRepository.deleteAll();
            prizeClaimRepository.deleteAll();
            reportRepository.deleteAll();
            commentLikeRepository.deleteAll();
            feedRepository.deleteAll();
            chatMessageRepository.deleteAll();  // chat_message가 users를 참조하므로 먼저 삭제
            userRepository.deleteAll();

            // AUTO_INCREMENT 초기화
            entityManager.createNativeQuery("ALTER TABLE users AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE feed AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE notification AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE comment AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE feed_like AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE feed_image AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE lottery_ticket AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE comment_like AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE chat_message AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE prize_claim AUTO_INCREMENT = 1").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE report AUTO_INCREMENT = 1").executeUpdate();

            log.info("기존 데이터 삭제 완료");
        }

        log.info("=== 테스트 데이터 초기화 시작 (로그인용 유저만 생성) ===");

        // 1. 20명의 일반 유저 생성
        List<User> users = createTestUsers();
        log.info("{}명의 테스트 유저 생성 완료", users.size());

        // 2. 의사 계정 생성
        List<User> doctors = createTestDoctors();
        log.info("{}명의 테스트 의사 계정 생성 완료", doctors.size());

        log.info("=== 테스트 데이터 초기화 완료 (로그인만 가능) ===");
    }

    private List<User> createTestUsers() {
        List<User> users = new ArrayList<>();

        users.add(createUser("김강민", "남성", "개발팀", 175.5, 72.5, LocalDate.of(1990, 3, 15), "user01", Role.MEMBER));
        users.add(createUser("이서연", "여성", "마케팅팀", 162.3, 52.3, LocalDate.of(1992, 7, 22), "user02", Role.MEMBER));
        users.add(createUser("박준호", "남성", "영업팀", 180.2, 78.8, LocalDate.of(1988, 11, 8), "user03", Role.MEMBER));
        users.add(createUser("최지우", "여성", "디자인팀", 158.7, 48.5, LocalDate.of(1995, 1, 30), "user04", Role.MEMBER));
        users.add(createUser("정민수", "남성", "인사팀", 172.8, 68.2, LocalDate.of(1991, 5, 12), "user05", Role.MEMBER));
        users.add(createUser("강혜진", "여성", "재무팀", 165.4, 55.7, LocalDate.of(1993, 9, 25), "user06", Role.MEMBER));
        users.add(createUser("윤태양", "남성", "법무팀", 178.1, 75.3, LocalDate.of(1989, 2, 18), "user07", Role.MEMBER));
        users.add(createUser("한소희", "여성", "경영지원팀", 160.9, 50.8, LocalDate.of(1994, 6, 7), "user08", Role.MEMBER));
        users.add(createUser("오성민", "남성", "연구개발팀", 183.5, 82.1, LocalDate.of(1987, 10, 3), "user09", Role.MEMBER));
        users.add(createUser("서은주", "여성", "기획팀", 167.2, 58.4, LocalDate.of(1996, 4, 28), "user10", Role.MEMBER));
        users.add(createUser("임동혁", "남성", "개발팀", 176.3, 70.6, LocalDate.of(1990, 8, 11), "user11", Role.MEMBER));
        users.add(createUser("배수지", "여성", "마케팅팀", 163.8, 54.2, LocalDate.of(1992, 12, 19), "user12", Role.MEMBER));
        users.add(createUser("신재호", "남성", "영업팀", 179.4, 77.5, LocalDate.of(1988, 3, 6), "user13", Role.MEMBER));
        users.add(createUser("조미라", "여성", "디자인팀", 159.5, 49.8, LocalDate.of(1995, 7, 14), "user14", Role.MEMBER));
        users.add(createUser("홍길동", "남성", "인사팀", 171.6, 67.3, LocalDate.of(1991, 11, 23), "user15", Role.MEMBER));
        users.add(createUser("안지영", "여성", "재무팀", 164.7, 56.1, LocalDate.of(1993, 1, 9), "user16", Role.MEMBER));
        users.add(createUser("유재석", "남성", "법무팀", 177.9, 74.8, LocalDate.of(1989, 5, 27), "user17", Role.MEMBER));
        users.add(createUser("송혜교", "여성", "경영지원팀", 161.2, 51.5, LocalDate.of(1994, 9, 16), "user18", Role.MEMBER));
        users.add(createUser("전지현", "여성", "연구개발팀", 182.3, 80.2, LocalDate.of(1987, 2, 4), "user19", Role.MEMBER));
        users.add(createUser("현빈", "남성", "기획팀", 168.5, 59.7, LocalDate.of(1996, 6, 21), "user20", Role.MEMBER));

        // 구글 로그인 테스트용 유저
        users.add(createUserWithEmail("박선일", "남성", "개발팀", 175.0, 70.0, LocalDate.of(1994, 6, 25), "parkil625", "parkil625@gmail.com"));
        users.add(createUserWithEmail("홍세민", "남성", "개발팀", 175.0, 70.0, LocalDate.of(2000, 11, 28), "pfielskdh46", "pfielskdh46@gmail.com"));
        users.add(createUserWithEmail("최재홍", "남성", "개발팀", 175.0, 70.0, LocalDate.of(2003, 5, 15), "chdjehong2", "chdjehong2@gmail.com"));


        return users;
    }

    private List<User> createTestDoctors() {
        List<User> doctors = new ArrayList<>();

        doctors.add(createUser("김민준", "남성", "내과", 175.0, 70.0, LocalDate.of(1985, 3, 15), "doctor01", Role.DOCTOR));
        doctors.add(createUser("이수연", "여성", "소아과", 162.0, 52.0, LocalDate.of(1987, 7, 22), "doctor02", Role.DOCTOR));
        doctors.add(createUser("박지훈", "남성", "외과", 178.0, 75.0, LocalDate.of(1983, 11, 8), "doctor03", Role.DOCTOR));
        doctors.add(createUser("최서윤", "여성", "산부인과", 165.0, 55.0, LocalDate.of(1986, 1, 30), "doctor04", Role.DOCTOR));
        doctors.add(createUser("정우진", "남성", "정형외과", 180.0, 78.0, LocalDate.of(1984, 5, 12), "doctor05", Role.DOCTOR));

        return doctors;
    }

    private void createTestChatMessages(List<User> users, List<User> doctors) {
        if (users.isEmpty() || doctors.isEmpty()) {
            return;
        }

        // user01 (환자) 와 doctor01 (의사) 간의 테스트 채팅 메시지 생성
        User patient = users.get(0);  // user01: 김강민
        User doctor = doctors.get(0);  // doctor01: 김민준

        // roomId 형식: "patientId:doctorId"
        String roomId = ChatMessage.generateRoomId(patient.getId(), doctor.getId());

        // 채팅 메시지 3개 생성 (시간 순서대로)
        java.time.LocalDateTime baseTime = java.time.LocalDateTime.now().minusHours(2);

        // 메시지 1: 환자가 먼저 인사
        ChatMessage msg1 = ChatMessage.builder()
                .roomId(roomId)
                .sender(patient)
                .content("안녕하세요 선생님, 최근 두통이 심해서 상담 요청 드립니다.")
                .sentAt(baseTime)
                .isRead("Y")
                .build();
        chatMessageRepository.save(msg1);

        // 메시지 2: 의사 응답
        ChatMessage msg2 = ChatMessage.builder()
                .roomId(roomId)
                .sender(doctor)
                .content("안녕하세요 김강민님. 두통이 언제부터 시작되셨나요? 증상을 자세히 말씀해주세요.")
                .sentAt(baseTime.plusMinutes(5))
                .isRead("Y")
                .build();
        chatMessageRepository.save(msg2);

        // 메시지 3: 환자 답변 (읽지 않은 메시지)
        ChatMessage msg3 = ChatMessage.builder()
                .roomId(roomId)
                .sender(patient)
                .content("3일 전부터 시작됐고, 특히 오후에 심해집니다. 눈 주변도 약간 아픈 것 같아요.")
                .sentAt(baseTime.plusMinutes(10))
                .isRead("N")
                .build();
        chatMessageRepository.save(msg3);

        log.info("테스트 채팅 메시지 생성 완료: {} <-> {} (roomId: {})", patient.getRealName(), doctor.getRealName(), roomId);
    }

    private User createUser(String realName, String gender, String department,
                            Double height, Double weight, LocalDate birthDate, String userId, Role role) {
        // 이미 존재하는 userId이면 기존 사용자 반환
        if (userRepository.findByUserId(userId).isPresent()) {
            return userRepository.findByUserId(userId).get();
        }

        User user = User.builder()
                .userId(userId)
                .email(userId + "@company.com")
                .password(passwordEncoder.encode("1"))
                .realName(realName)
                .role(role)
                .height(height)
                .weight(weight)
                .gender(gender)
                .birthDate(birthDate)
                .currentPoints(0L)
                .monthlyPoints(0L)
                .department(department)
                .build();

        return userRepository.save(user);
    }

    private User createUserWithEmail(String realName, String gender, String department,
                            Double height, Double weight, LocalDate birthDate, String userId, String email) {
        // 이미 존재하는 userId이면 기존 사용자 반환
        if (userRepository.findByUserId(userId).isPresent()) {
            return userRepository.findByUserId(userId).get();
        }

        User user = User.builder()
                .userId(userId)
                .email(email)
                .password(passwordEncoder.encode("1"))
                .realName(realName)
                .role(Role.MEMBER)
                .height(height)
                .weight(weight)
                .gender(gender)
                .birthDate(birthDate)
                .currentPoints(0L)
                .monthlyPoints(0L)
                .department(department)
                .build();

        return userRepository.save(user);
    }

    private List<FeedDetailResponse> createTestFeeds(List<User> users) {
        List<FeedDetailResponse> feeds = new ArrayList<>();

        // createFeed(user, activityType, duration(분), content) - 칼로리/포인트 자동 계산
        // 포인트 = (MET × duration) / 10, 최대 30점
        feeds.add(createFeed(users.get(0), "러닝", 31, "아침 러닝으로 상쾌하게 하루 시작! 한강 따라 5km 완주했습니다 🏃‍♂️"));  // 30점
        feeds.add(createFeed(users.get(1), "걷기", 13, "짧은 점심 산책! 기분 전환에 딱이에요 ☀️"));  // 5점
        feeds.add(createFeed(users.get(2), "자전거", 40, "주말 자전거 라이딩! 북한산 둘레길 완주하고 왔습니다 🚴"));  // 30점
        feeds.add(createFeed(users.get(3), "수영", 19, "수영장에서 가볍게 몇 바퀴! 폼 연습 중이에요 🏊‍♀️"));  // 15점
        feeds.add(createFeed(users.get(4), "등산", 23, "동네 뒷산 가볍게 다녀왔어요 ⛰️"));  // 15점
        feeds.add(createFeed(users.get(5), "요가", 20, "아침 요가로 하루를 열었어요. 마음이 편안해지는 시간이에요 🧘‍♀️"));  // 5점
        feeds.add(createFeed(users.get(6), "러닝", 10, "짧게 동네 한 바퀴! 오늘은 컨디션이 별로라 가볍게 🌙"));  // 10점
        feeds.add(createFeed(users.get(7), "걷기", 53, "퇴근 후 동네 한바퀴! 요즘 걷기 운동에 빠졌어요 👟"));  // 20점
        feeds.add(createFeed(users.get(8), "자전거", 27, "출퇴근 자전거 시작했습니다! 교통비도 절약하고 운동도 되고 👍"));  // 20점
        feeds.add(createFeed(users.get(9), "수영", 38, "오늘 접영 배웠어요! 생각보다 어렵지만 재밌네요 🏊"));  // 30점
        feeds.add(createFeed(users.get(10), "등산", 8, "점심시간에 회사 뒷산 살짝 올라갔다 왔어요 🌲"));  // 5점
        feeds.add(createFeed(users.get(11), "요가", 48, "핫요가 처음 해봤는데 땀이 엄청 나네요! 개운해요 💦"));  // 12점
        feeds.add(createFeed(users.get(12), "러닝", 26, "마라톤 대회 준비 중! 오늘 5km 페이스 측정했어요 🏅"));  // 25점
        feeds.add(createFeed(users.get(13), "걷기", 26, "카페 갈 때도 걸어가기! 작은 습관이 모여 건강을 만들어요 ☕"));  // 10점
        feeds.add(createFeed(users.get(14), "자전거", 20, "한강 자전거 도로 여의도까지 🚲"));  // 15점
        feeds.add(createFeed(users.get(15), "수영", 25, "배영 폼 교정 받았어요. 코치님 말씀대로 하니까 확실히 다르네요 🤽"));  // 20점
        feeds.add(createFeed(users.get(16), "등산", 46, "관악산 정상! 힘들었지만 뷰가 최고였어요. 다음엔 도봉산 도전 🏔️"));  // 30점
        feeds.add(createFeed(users.get(17), "요가", 8, "스트레칭 위주로 가볍게! 🌸"));  // 2점
        feeds.add(createFeed(users.get(18), "러닝", 15, "가벼운 조깅으로 하루 마무리 💨"));  // 15점
        feeds.add(createFeed(users.get(19), "걷기", 79, "저녁 산책하면서 팟캐스트 듣기! 나만의 힐링 타임이에요 🎧"));  // 30점

        return feeds;
    }

    private FeedDetailResponse createFeed(User user, String activityType, int duration, String content) {
        Map<String, String> activityImages = Map.of(
            "러닝", "https://picsum.photos/seed/running/800/600",
            "걷기", "https://picsum.photos/seed/walking/800/600",
            "자전거", "https://picsum.photos/seed/cycling/800/600",
            "수영", "https://picsum.photos/seed/swimming/800/600",
            "등산", "https://picsum.photos/seed/hiking/800/600",
            "요가", "https://picsum.photos/seed/yoga/800/600"
        );

        // 칼로리는 FeedService에서 자동 계산
        FeedCreateRequest request = FeedCreateRequest.builder()
                .activityType(activityType)
                .duration(duration)
                .content(content)
                .imageUrls(Arrays.asList(activityImages.get(activityType)))
                .build();

        return feedService.createFeed(user.getId(), request);
    }

    private List<CommentResponse> createTestComments(List<User> users, List<FeedDetailResponse> feeds) {
        List<CommentResponse> comments = new ArrayList<>();

        // 명확한 시나리오로 댓글/답글 생성
        // user01(김강민)의 피드에 user02(이서연)가 댓글 -> user03(박준호)가 답글
        if (feeds.size() > 0) {
            FeedDetailResponse feed1 = feeds.get(0); // user01의 피드

            CommentResponse comment1 = commentService.createComment(feed1.getId(), users.get(1).getId(),
                CommentCreateRequest.builder().content("대단하세요! 러닝 화이팅! 👍").build());
            comments.add(comment1);

            CommentResponse reply1 = commentService.createComment(feed1.getId(), users.get(2).getId(),
                CommentCreateRequest.builder().content("저도 같이 뛰고 싶네요 💪").parentId(comment1.getId()).build());
            comments.add(reply1);
        }

        // user02(이서연)의 피드에 user04(최지우)가 댓글
        if (feeds.size() > 1) {
            FeedDetailResponse feed2 = feeds.get(1);

            CommentResponse comment2 = commentService.createComment(feed2.getId(), users.get(3).getId(),
                CommentCreateRequest.builder().content("오늘도 운동 완료! 멋져요 ✨").build());
            comments.add(comment2);
        }

        // user03(박준호)의 피드에 user01(김강민)이 댓글 -> user05(정민수)가 답글
        if (feeds.size() > 2) {
            FeedDetailResponse feed3 = feeds.get(2);

            CommentResponse comment3 = commentService.createComment(feed3.getId(), users.get(0).getId(),
                CommentCreateRequest.builder().content("좋은 운동이네요! 👏").build());
            comments.add(comment3);

            CommentResponse reply3 = commentService.createComment(feed3.getId(), users.get(4).getId(),
                CommentCreateRequest.builder().content("동의합니다! 함께해요").parentId(comment3.getId()).build());
            comments.add(reply3);
        }

        // user04(최지우)의 피드에 user06(강혜진)이 댓글
        if (feeds.size() > 3) {
            FeedDetailResponse feed4 = feeds.get(3);

            CommentResponse comment4 = commentService.createComment(feed4.getId(), users.get(5).getId(),
                CommentCreateRequest.builder().content("꾸준히 하시는 모습이 멋져요! 🌟").build());
            comments.add(comment4);
        }

        // user05(정민수)의 피드에 user07(윤태양)이 댓글 -> user08(한소희)가 답글
        if (feeds.size() > 4) {
            FeedDetailResponse feed5 = feeds.get(4);

            CommentResponse comment5 = commentService.createComment(feed5.getId(), users.get(6).getId(),
                CommentCreateRequest.builder().content("오늘도 파이팅! 💯").build());
            comments.add(comment5);

            CommentResponse reply5 = commentService.createComment(feed5.getId(), users.get(7).getId(),
                CommentCreateRequest.builder().content("응원합니다! 화이팅! 🔥").parentId(comment5.getId()).build());
            comments.add(reply5);
        }

        return comments;
    }

    private void createTestLikes(List<User> users, List<FeedDetailResponse> feeds) {
        // 명확한 시나리오로 좋아요 생성

        // user01(김강민)의 피드에 user02, user03, user04가 좋아요
        if (feeds.size() > 0) {
            FeedDetailResponse feed1 = feeds.get(0);
            feedService.likeFeed(feed1.getId(), users.get(1).getId());
            feedService.likeFeed(feed1.getId(), users.get(2).getId());
            feedService.likeFeed(feed1.getId(), users.get(3).getId());
        }

        // user02(이서연)의 피드에 user01, user05가 좋아요
        if (feeds.size() > 1) {
            FeedDetailResponse feed2 = feeds.get(1);
            feedService.likeFeed(feed2.getId(), users.get(0).getId());
            feedService.likeFeed(feed2.getId(), users.get(4).getId());
        }

        // user03(박준호)의 피드에 user01, user02, user06, user07이 좋아요
        if (feeds.size() > 2) {
            FeedDetailResponse feed3 = feeds.get(2);
            feedService.likeFeed(feed3.getId(), users.get(0).getId());
            feedService.likeFeed(feed3.getId(), users.get(1).getId());
            feedService.likeFeed(feed3.getId(), users.get(5).getId());
            feedService.likeFeed(feed3.getId(), users.get(6).getId());
        }

        // user04(최지우)의 피드에 user08, user09가 좋아요
        if (feeds.size() > 3) {
            FeedDetailResponse feed4 = feeds.get(3);
            feedService.likeFeed(feed4.getId(), users.get(7).getId());
            feedService.likeFeed(feed4.getId(), users.get(8).getId());
        }

        // user05(정민수)의 피드에 user10, user11, user12가 좋아요
        if (feeds.size() > 4) {
            FeedDetailResponse feed5 = feeds.get(4);
            feedService.likeFeed(feed5.getId(), users.get(9).getId());
            feedService.likeFeed(feed5.getId(), users.get(10).getId());
            feedService.likeFeed(feed5.getId(), users.get(11).getId());
        }
    }

    private void createTestCommentLikes(List<User> users, List<CommentResponse> comments) {
        // user01(김강민)의 피드를 기준으로 테스트 데이터 생성
        // 현재 comments 구조:
        // 0: user02가 user01의 피드에 단 댓글
        // 1: user03이 comment0에 단 답글
        // 2: user04가 user02의 피드에 단 댓글
        // 3: user01이 user03의 피드에 단 댓글
        // 4: user05가 comment3에 단 답글
        // 5: user06이 user04의 피드에 단 댓글
        // 6: user07이 user05의 피드에 단 댓글
        // 7: user08이 comment6에 단 답글

        if (comments.size() > 0) {
            // user01의 피드에 user02가 단 댓글(comment0)에 user03, user04, user05가 좋아요
            // -> user02에게 댓글 좋아요 알림 3개
            commentService.likeComment(comments.get(0).getId(), users.get(2).getId()); // user03
            commentService.likeComment(comments.get(0).getId(), users.get(3).getId()); // user04
            commentService.likeComment(comments.get(0).getId(), users.get(4).getId()); // user05
        }

        if (comments.size() > 1) {
            // user03이 단 답글(comment1)에 user01, user02가 좋아요
            // -> user03에게 댓글 좋아요 알림 2개
            commentService.likeComment(comments.get(1).getId(), users.get(0).getId()); // user01
            commentService.likeComment(comments.get(1).getId(), users.get(1).getId()); // user02
        }

        if (comments.size() > 3) {
            // user01이 user03의 피드에 단 댓글(comment3)에 user06, user07이 좋아요
            // -> user01에게 댓글 좋아요 알림 2개
            commentService.likeComment(comments.get(3).getId(), users.get(5).getId()); // user06
            commentService.likeComment(comments.get(3).getId(), users.get(6).getId()); // user07
        }

        if (comments.size() > 6) {
            // user07이 단 댓글(comment6)에 user08, user09가 좋아요
            // -> user07에게 댓글 좋아요 알림 2개
            commentService.likeComment(comments.get(6).getId(), users.get(7).getId()); // user08
            commentService.likeComment(comments.get(6).getId(), users.get(8).getId()); // user09
        }
    }
}
