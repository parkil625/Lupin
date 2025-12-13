package com.example.demo.repository;

import com.example.demo.config.JpaConfig;
import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentLike;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.domain.enums.SocialProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
abstract class BaseRepositoryTest {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected FeedRepository feedRepository;

    @Autowired
    protected CommentRepository commentRepository;

    @Autowired
    protected CommentLikeRepository commentLikeRepository;

    protected User createAndSaveUser(String userId) {
        return createAndSaveUser(userId, "testName");
    }

    protected User createAndSaveUser(String userId, String name) {
        User user = User.builder()
                .userId(userId)
                .password("testPassword")
                .name(name)
                .role(Role.MEMBER)
                .build();
        return userRepository.save(user);
    }

    protected User createAndSaveUserWithProviderEmail(String userId, String providerEmail) {
        User user = User.builder()
                .userId(userId)
                .password("testPassword")
                .name("testName")
                .role(Role.MEMBER)
                .providerEmail(providerEmail)
                .build();
        return userRepository.save(user);
    }

    protected User createAndSaveUserWithProvider(String userId, SocialProvider provider, String providerId) {
        User user = User.builder()
                .userId(userId)
                .password("testPassword")
                .name("testName")
                .role(Role.MEMBER)
                .provider(provider)
                .providerId(providerId)
                .build();
        return userRepository.save(user);
    }

    protected Feed createAndSaveFeed(User writer, String activity) {
        Feed feed = Feed.builder()
                .writer(writer)
                .activity(activity)
                .content("testContent")
                .points(0L)
                .calories(0)
                .build();
        return feedRepository.save(feed);
    }

    protected Comment createAndSaveComment(User writer, Feed feed, String content) {
        Comment comment = Comment.builder()
                .writer(writer)
                .feed(feed)
                .content(content)
                .build();
        return commentRepository.save(comment);
    }

    protected CommentLike createAndSaveCommentLike(User user, Comment comment) {
        CommentLike commentLike = CommentLike.builder()
                .user(user)
                .comment(comment)
                .build();
        return commentLikeRepository.save(commentLike);
    }
}
