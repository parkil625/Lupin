package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.PointLogRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointLogRepository pointLogRepository;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("testUser")
                .password("password")
                .name("테스트유저")
                .role(Role.MEMBER)
                .height(175.0)
                .weight(70.0)
                .build();
    }

    @Test
    @DisplayName("사용자 정보를 조회한다")
    void getUserInfoTest() {
        // given
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        User result = userService.getUserInfo(userId);

        // then
        assertThat(result).isEqualTo(user);
        assertThat(result.getName()).isEqualTo("테스트유저");
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 조회하면 예외가 발생한다")
    void getUserInfoNotFoundTest() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserInfo(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 프로필을 수정한다")
    void updateProfileTest() {
        // given
        String newName = "수정된이름";
        Double newHeight = 180.0;
        Double newWeight = 75.0;

        // when
        userService.updateProfile(user, newName, newHeight, newWeight);

        // then
        assertThat(user.getName()).isEqualTo(newName);
        assertThat(user.getHeight()).isEqualTo(newHeight);
        assertThat(user.getWeight()).isEqualTo(newWeight);
    }

    @Test
    @DisplayName("사용자 통계를 조회한다")
    void getUserStatsTest() {
        // given
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(pointLogRepository.sumPointsByUser(user)).willReturn(100L);
        given(feedRepository.countByWriterId(userId)).willReturn(5L);
        given(commentRepository.countByWriterId(userId)).willReturn(10L);

        // when
        Map<String, Object> stats = userService.getUserStats(userId);

        // then
        assertThat(stats.get("userId")).isEqualTo(userId);
        assertThat(stats.get("totalPoints")).isEqualTo(100L);
        assertThat(stats.get("feedCount")).isEqualTo(5L);
        assertThat(stats.get("commentCount")).isEqualTo(10L);
    }

    @Test
    @DisplayName("사용자 통계 조회시 포인트가 없으면 0을 반환한다")
    void getUserStatsNoPointsTest() {
        // given
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(pointLogRepository.sumPointsByUser(user)).willReturn(null);
        given(feedRepository.countByWriterId(userId)).willReturn(0L);
        given(commentRepository.countByWriterId(userId)).willReturn(0L);

        // when
        Map<String, Object> stats = userService.getUserStats(userId);

        // then
        assertThat(stats.get("totalPoints")).isEqualTo(0L);
        assertThat(stats.get("feedCount")).isEqualTo(0L);
        assertThat(stats.get("commentCount")).isEqualTo(0L);
    }

    @Test
    @DisplayName("사용자 아바타를 수정한다")
    void updateAvatarTest() {
        // given
        String avatarUrl = "https://example.com/avatar.jpg";

        // when
        userService.updateAvatar(user, avatarUrl);

        // then
        assertThat(user.getAvatar()).isEqualTo(avatarUrl);
        verify(userRepository).save(user);
    }
}
