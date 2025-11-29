package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

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
}
