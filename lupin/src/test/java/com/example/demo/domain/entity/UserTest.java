package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("사용자 정보를 수정한다")
    void updateProfileTest() {
        // given
        User user = User.builder()
                .userId("testUser")
                .password("password")
                .name("원래 이름")
                .role(Role.MEMBER)
                .build();

        // when
        user.updateProfile("수정된 이름", 175.0, 70.0);

        // then
        assertThat(user.getName()).isEqualTo("수정된 이름");
        assertThat(user.getHeight()).isEqualTo(175.0);
        assertThat(user.getWeight()).isEqualTo(70.0);
    }
}
