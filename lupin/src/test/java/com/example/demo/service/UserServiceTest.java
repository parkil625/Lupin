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
import org.springframework.data.redis.core.RedisTemplate; // [추가]
import org.springframework.data.redis.core.ZSetOperations; // [추가]
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set; // [추가]
import java.util.LinkedHashSet; // [추가]

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString; // [추가]
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient; // [추가] lenient import 필요
import static org.mockito.Mockito.mock; 
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

    @Mock
    private RedisTemplate<String, String> redisTemplate; // [추가]

    @Mock
    private ZSetOperations<String, String> zSetOperations; // [추가]

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        // [수정] Redis를 사용하지 않는 테스트도 있으므로 lenient()를 사용하여 불필요한 스터빙 에러 방지
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

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
    @DisplayName("랭킹 조회 시 실제 포인트가 음수라도 화면에는 0으로 표시된다")
    void getTopUsersByPoints_NegativePointsDisplayZero() {
        // given
        User positiveUser = User.builder().id(1L).name("양수유저").build();
        User negativeUser = User.builder().id(2L).name("음수유저").build();

        // 1. Redis ZSet 리턴값 Mocking (TupleSet)
        Set<ZSetOperations.TypedTuple<String>> tupleSet = new LinkedHashSet<>();
        
        // 1등: 100점 (양수)
        ZSetOperations.TypedTuple<String> tuple1 = mock(ZSetOperations.TypedTuple.class);
        given(tuple1.getValue()).willReturn("1");
        given(tuple1.getScore()).willReturn(100.0);
        tupleSet.add(tuple1);

        // 2등: -50점 (음수)
        ZSetOperations.TypedTuple<String> tuple2 = mock(ZSetOperations.TypedTuple.class);
        given(tuple2.getValue()).willReturn("2");
        given(tuple2.getScore()).willReturn(-50.0);
        tupleSet.add(tuple2);

        // Redis 조회 시 위 Set 반환
        given(zSetOperations.reverseRangeWithScores(anyString(), any(Long.class), any(Long.class)))
                .willReturn(tupleSet);

        // 2. UserRepository 조회 Mocking (ID 리스트로 조회)
        given(userRepository.findAllById(any())).willReturn(List.of(positiveUser, negativeUser));

        // when
        List<UserRankingResponse> results = userService.getTopUsersByPoints(10);

        // then
        assertThat(results).hasSize(2);

        // 1등: 100점 -> 100점 그대로
        assertThat(results.get(0).points()).isEqualTo(100L);
        assertThat(results.get(0).rank()).isEqualTo(1);
        assertThat(results.get(0).name()).isEqualTo("양수유저");

        // 2등: -50점 -> 0점으로 변환 확인 (핵심 검증)
        assertThat(results.get(1).points()).isEqualTo(0L);
        assertThat(results.get(1).rank()).isEqualTo(2);
        assertThat(results.get(1).name()).isEqualTo("음수유저");
    }
}