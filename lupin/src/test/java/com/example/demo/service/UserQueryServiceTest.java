package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserQueryService 테스트")
class UserQueryServiceTest {

    @InjectMocks
    private UserQueryService userQueryService;

    @Mock
    private JPAQueryFactory queryFactory;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FeedRepository feedRepository;

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
                .currentPoints(100L)
                .monthlyPoints(50L)
                .monthlyLikes(10L)
                .department("개발팀")
                .build();
    }

    @Nested
    @DisplayName("사용자 정보 조회")
    class GetUserInfo {

        @Test
        @DisplayName("사용자 정보 조회 성공")
        void getUserInfo_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when
            Map<String, Object> result = userQueryService.getUserInfo(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.get("realName")).isEqualTo("테스트유저");
            assertThat(result.get("email")).isEqualTo("test@test.com");
            assertThat(result.get("currentPoints")).isEqualTo(100L);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 실패")
        void getUserInfo_NotFound_ThrowsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userQueryService.getUserInfo(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("사용자 존재 여부 확인")
    class ExistsById {

        @Test
        @DisplayName("존재하는 사용자")
        void existsById_True() {
            // given
            given(userRepository.existsById(1L)).willReturn(true);

            // when
            boolean result = userQueryService.existsById(1L);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 사용자")
        void existsById_False() {
            // given
            given(userRepository.existsById(999L)).willReturn(false);

            // when
            boolean result = userQueryService.existsById(999L);

            // then
            assertThat(result).isFalse();
        }
    }
}
