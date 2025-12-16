package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.response.UserRankingResponse;
import com.example.demo.dto.response.UserStatsResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("사용자 프로필을 수정하고 DB에 저장한다")
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

        // [TDD] 저장 메서드가 호출되었는지 검증 (기존 코드에서는 실패함)
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("사용자 통계를 조회한다 (반정규화된 totalPoints 사용)")
    void getUserStatsTest() {
        // given
        Long userId = 1L;
        ReflectionTestUtils.setField(user, "totalPoints", 100L);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(feedRepository.countByWriterId(userId)).willReturn(5L);
        given(commentRepository.countByWriterId(userId)).willReturn(10L);

        // when
        UserStatsResponse stats = userService.getUserStats(userId);

        // then
        assertThat(stats.userId()).isEqualTo(userId);
        assertThat(stats.totalPoints()).isEqualTo(100L);
        assertThat(stats.feedCount()).isEqualTo(5L);
        assertThat(stats.commentCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("사용자 통계 조회시 포인트가 없으면 기본값 0을 반환한다")
    void getUserStatsNoPointsTest() {
        // given
        Long userId = 1L;
        // totalPoints 기본값은 0L
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(feedRepository.countByWriterId(userId)).willReturn(0L);
        given(commentRepository.countByWriterId(userId)).willReturn(0L);

        // when
        UserStatsResponse stats = userService.getUserStats(userId);

        // then
        assertThat(stats.totalPoints()).isZero();
        assertThat(stats.feedCount()).isZero();
        assertThat(stats.commentCount()).isZero();
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

    @Test
    @DisplayName("랭킹 조회 시 실제 포인트가 음수라도 화면에는 0으로 표시된다")
    void getTopUsersByPoints_NegativePointsDisplayZero() {
        // given
        User positiveUser = User.builder().id(1L).name("양수유저").build();
        User negativeUser = User.builder().id(2L).name("음수유저").build();

        // DB에서는 실제 값(-50)을 가져온다고 가정
        List<Object[]> mockResults = List.of(
                new Object[]{positiveUser, 100L},
                new Object[]{negativeUser, -50L}
        );

        given(pointLogRepository.findAllUsersWithPointsRanked(any(Pageable.class)))
                .willReturn(mockResults);

        // when
        List<UserRankingResponse> results = userService.getTopUsersByPoints(10);

        // then
        assertThat(results).hasSize(2);

        // 1등: 100점 -> 100점 그대로
        assertThat(results.get(0).points()).isEqualTo(100L);
        assertThat(results.get(0).rank()).isEqualTo(1);

        // 2등: -50점 -> 0점으로 변환 확인 (핵심 검증)
        assertThat(results.get(1).points()).isEqualTo(0L);
        assertThat(results.get(1).rank()).isEqualTo(2); // 순위는 2등 유지
    }
}