package com.example.demo.repository;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
abstract class BaseRepositoryTest {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected FeedRepository feedRepository;

    @Autowired
    protected CommentRepository commentRepository;

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

    protected Feed createAndSaveFeed(User writer, String activity) {
        Feed feed = Feed.builder()
                .writer(writer)
                .activity(activity)
                .content("testContent")
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
}
