package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.response.UserStatsResponse;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.PointLogRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointLogRepository pointLogRepository;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private User user;

    @Test
    @DisplayName("사용자 정보를 조회한다")
    void getUserInfoTest() {
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        User result = userService.getUserInfo(userId);
        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("사용자 프로필을 수정한다")
    void updateProfileTest() {
        String name = "newName";
        Double height = 180.0;
        Double weight = 75.0;
        userService.updateProfile(user, name, height, weight);
        verify(user).updateProfile(name, height, weight);
    }

    @Test
    @DisplayName("사용자 통계를 조회한다")
    void getUserStatsTest() {
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(feedRepository.countByWriterId(userId)).willReturn(10L);
        given(commentRepository.countByWriterId(userId)).willReturn(5L);
        given(user.getTotalPoints()).willReturn(100L);
        UserStatsResponse stats = userService.getUserStats(userId);
        assertThat(stats.feedCount()).isEqualTo(10L);
        assertThat(stats.commentCount()).isEqualTo(5L);
        assertThat(stats.totalPoints()).isEqualTo(100L);
    }

    @Test
    @DisplayName("사용자 아바타를 수정한다")
    void updateAvatarTest() {
        String avatarUrl = "newAvatar.jpg";
        userService.updateAvatar(user, avatarUrl);
        verify(user).updateAvatar(avatarUrl);
    }
}