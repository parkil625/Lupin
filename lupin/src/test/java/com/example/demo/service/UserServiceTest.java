package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.response.UserProfileResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private FeedRepository feedRepository;
    @Mock
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .userId("user01")
                .email("test@test.com")
                .realName("테스트유저")
                .role(Role.MEMBER)
                .gender("남성")
                .birthDate(LocalDate.of(1990, 1, 1))
                .height(175.0)
                .weight(70.0)
                .currentPoints(20L)
                .monthlyPoints(50L)
                .monthlyLikes(10L)
                .build();
    }

    @Nested
    @DisplayName("사용자 프로필 조회")
    class GetUserProfile {

        @Test
        @DisplayName("프로필 조회 성공")
        void getUserProfile_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedRepository.countUserFeeds(1L)).willReturn(5L);

            // when
            UserProfileResponse result = userService.getUserProfile(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getRealName()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("존재하지 않는 유저 프로필 조회 실패")
        void getUserProfile_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUserProfile(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("포인트 적립")
    class AddPoints {

        @Test
        @DisplayName("포인트 적립 성공")
        void addPoints_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when
            userService.addPoints(1L, 5L, "운동", "feed-1");

            // then
            assertThat(user.getCurrentPoints()).isEqualTo(25L);
        }

        @Test
        @DisplayName("존재하지 않는 유저 포인트 적립 실패")
        void addPoints_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.addPoints(999L, 10L, "운동", "feed-1"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("포인트 차감")
    class DeductPoints {

        @Test
        @DisplayName("포인트 차감 성공")
        void deductPoints_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when
            userService.deductPoints(1L, 10L, "상품 구매");

            // then
            assertThat(user.getCurrentPoints()).isEqualTo(10L);
        }

        @Test
        @DisplayName("포인트 부족시 차감 실패")
        void deductPoints_InsufficientPoints_ThrowsException() {
            // given
            user.setCurrentPoints(5L);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> userService.deductPoints(1L, 100L, "상품 구매"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("이메일로 사용자 조회")
    class FindByEmail {

        @Test
        @DisplayName("이메일로 사용자 조회 성공")
        void findByEmail_Success() {
            // given
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

            // when
            User result = userService.findByEmail("test@test.com");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("test@test.com");
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 조회 실패")
        void findByEmail_NotFound_ThrowsException() {
            // given
            given(userRepository.findByEmail("unknown@test.com")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.findByEmail("unknown@test.com"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("이메일 중복 확인")
    class IsEmailExists {

        @Test
        @DisplayName("이메일 존재 확인 - 존재함")
        void isEmailExists_True() {
            // given
            given(userRepository.existsByEmail("test@test.com")).willReturn(true);

            // when
            boolean result = userService.isEmailExists("test@test.com");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("이메일 존재 확인 - 존재하지 않음")
        void isEmailExists_False() {
            // given
            given(userRepository.existsByEmail("new@test.com")).willReturn(false);

            // when
            boolean result = userService.isEmailExists("new@test.com");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("상위 포인트 사용자 조회")
    class GetTopUsersByPoints {

        @Test
        @DisplayName("상위 사용자 조회 성공")
        void getTopUsersByPoints_Success() {
            // given
            User user2 = User.builder()
                    .id(2L)
                    .realName("유저2")
                    .monthlyPoints(100L)
                    .monthlyLikes(5L)
                    .build();
            User user3 = User.builder()
                    .id(3L)
                    .realName("유저3")
                    .monthlyPoints(30L)
                    .monthlyLikes(20L)
                    .build();

            given(userRepository.findAll()).willReturn(Arrays.asList(user, user2, user3));
            given(feedRepository.countUserActiveDaysInCurrentMonth(anyLong())).willReturn(5);

            // when
            List<Map<String, Object>> result = userService.getTopUsersByPoints(3);

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).get("name")).isEqualTo("유저2");
        }
    }

    @Nested
    @DisplayName("사용자 주변 랭킹 조회")
    class GetUserRankingContext {

        @Test
        @DisplayName("주변 랭킹 조회 성공")
        void getUserRankingContext_Success() {
            // given
            User user2 = User.builder()
                    .id(2L)
                    .realName("유저2")
                    .monthlyPoints(100L)
                    .monthlyLikes(5L)
                    .build();
            User user3 = User.builder()
                    .id(3L)
                    .realName("유저3")
                    .monthlyPoints(30L)
                    .monthlyLikes(20L)
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.findAll()).willReturn(Arrays.asList(user, user2, user3));
            given(feedRepository.countUserActiveDaysInCurrentMonth(anyLong())).willReturn(5);

            // when
            List<Map<String, Object>> result = userService.getUserRankingContext(1L);

            // then
            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 유저 주변 랭킹 조회 실패")
        void getUserRankingContext_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUserRankingContext(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("전체 통계 조회")
    class GetStatistics {

        @Test
        @DisplayName("통계 조회 성공")
        void getStatistics_Success() {
            // given
            User user2 = User.builder()
                    .id(2L)
                    .monthlyPoints(100L)
                    .build();

            given(userRepository.findAll()).willReturn(Arrays.asList(user, user2));

            // when
            Map<String, Object> result = userService.getStatistics();

            // then
            assertThat(result.get("totalUsers")).isEqualTo(2);
            assertThat(result.get("activeUsersThisMonth")).isEqualTo(2L);
        }
    }
}
