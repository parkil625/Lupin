const fs = require('fs');

// 1. FeedLikeService 수정
const feedLikeServicePath = 'c:/Lupin/lupin/src/main/java/com/example/demo/service/FeedLikeService.java';
const feedLikeServiceContent = `package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedLikeService {

    private final FeedLikeRepository feedLikeRepository;
    private final FeedRepository feedRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @Transactional
    public FeedLike likeFeed(User user, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        if (feedLikeRepository.existsByUserAndFeed(user, feed)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        FeedLike feedLike = FeedLike.builder()
                .user(user)
                .feed(feed)
                .build();

        FeedLike savedFeedLike = feedLikeRepository.save(feedLike);

        // 좋아요 카운트 증가 (반정규화)
        feed.incrementLikeCount();

        // refId = feedId (피드 참조)
        notificationService.createFeedLikeNotification(feed.getWriter(), user, feedId);

        return savedFeedLike;
    }

    @Transactional
    public void unlikeFeed(User user, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        FeedLike feedLike = feedLikeRepository.findByUserAndFeed(user, feed)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_NOT_FOUND));

        // 좋아요 카운트 감소 (반정규화)
        feed.decrementLikeCount();

        // refId = feedId (피드 참조)
        notificationRepository.deleteByRefIdAndType(String.valueOf(feedId), "FEED_LIKE");
        feedLikeRepository.delete(feedLike);
    }
}
`;
fs.writeFileSync(feedLikeServicePath, feedLikeServiceContent);
console.log('FeedLikeService updated!');

// 2. CommentService 수정 - 읽어서 수정
const commentServicePath = 'c:/Lupin/lupin/src/main/java/com/example/demo/service/CommentService.java';
let commentServiceContent = fs.readFileSync(commentServicePath, 'utf8');

// createComment에 commentCount 증가 추가
commentServiceContent = commentServiceContent.replace(
    'Comment savedComment = commentRepository.save(comment);',
    `Comment savedComment = commentRepository.save(comment);

        // 댓글 카운트 증가 (반정규화)
        feed.incrementCommentCount();`
);

// deleteComment에 commentCount 감소 추가
commentServiceContent = commentServiceContent.replace(
    'validateOwnership(comment, user);',
    `validateOwnership(comment, user);

        // 댓글 카운트 감소 (반정규화)
        comment.getFeed().decrementCommentCount();`
);

// createReply에도 commentCount 증가 추가
commentServiceContent = commentServiceContent.replace(
    'Comment savedReply = commentRepository.save(reply);',
    `Comment savedReply = commentRepository.save(reply);

        // 대댓글도 댓글 카운트 증가 (반정규화)
        feed.incrementCommentCount();`
);

fs.writeFileSync(commentServicePath, commentServiceContent);
console.log('CommentService updated!');

// 3. FeedService 수정 - thumbnailUrl 설정
const feedServicePath = 'c:/Lupin/lupin/src/main/java/com/example/demo/service/FeedService.java';
let feedServiceContent = fs.readFileSync(feedServicePath, 'utf8');

// createFeed에서 thumbnailUrl 설정 추가
feedServiceContent = feedServiceContent.replace(
    'Feed savedFeed = feedRepository.save(feed);',
    `Feed savedFeed = feedRepository.save(feed);

        // 썸네일 URL 설정 (반정규화 - 시작 이미지 사용)
        savedFeed.setThumbnailUrl(startImageKey);`
);

// updateFeed에서 thumbnailUrl 설정 추가
feedServiceContent = feedServiceContent.replace(
    '// 기존 이미지 삭제 (orphanRemoval이 동작하도록 Set을 clear)',
    `// 썸네일 URL 업데이트 (반정규화)
        feed.setThumbnailUrl(startImageKey);

        // 기존 이미지 삭제 (orphanRemoval이 동작하도록 Set을 clear)`
);

fs.writeFileSync(feedServicePath, feedServiceContent);
console.log('FeedService updated!');

// 4. UserPenaltyService 수정 - ban() 호출 추가
const userPenaltyServicePath = 'c:/Lupin/lupin/src/main/java/com/example/demo/service/UserPenaltyService.java';
let userPenaltyServiceContent = fs.readFileSync(userPenaltyServicePath, 'utf8');

userPenaltyServiceContent = userPenaltyServiceContent.replace(
    'return userPenaltyRepository.save(penalty);',
    `// 유저 상태를 BANNED로 변경
        user.ban();

        return userPenaltyRepository.save(penalty);`
);

fs.writeFileSync(userPenaltyServicePath, userPenaltyServiceContent);
console.log('UserPenaltyService updated!');

// 5. AuthService 수정 - isActive() 체크 추가
const authServicePath = 'c:/Lupin/lupin/src/main/java/com/example/demo/service/AuthService.java';
let authServiceContent = fs.readFileSync(authServicePath, 'utf8');

// login 메서드에 isActive 체크 추가
authServiceContent = authServiceContent.replace(
    `if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        return generateTokens(user);`,
    `if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 유저 상태 확인 (정지/탈퇴 상태인지)
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_BANNED);
        }

        return generateTokens(user);`
);

fs.writeFileSync(authServicePath, authServiceContent);
console.log('AuthService updated!');

console.log('\\nAll services updated successfully!');
