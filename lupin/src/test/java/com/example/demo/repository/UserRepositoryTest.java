package com.example.demo.repository;

import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends BaseRepositoryTest {

    @Test
    @DisplayName("findByUserId로 사용자를 조회한다")
    void findByUserIdTest() {
        // given
        User user = createAndSaveUser("testUser");

        // when
        Optional<User> found = userRepository.findByUserId("testUser");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("testUser");
    }

    @Test
    @DisplayName("findByProviderEmail로 사용자를 조회한다")
    void findByProviderEmailTest() {
        // given
        User user = createAndSaveUserWithProviderEmail("testUser", "test@gmail.com");

        // when
        Optional<User> found = userRepository.findByProviderEmail("test@gmail.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getProviderEmail()).isEqualTo("test@gmail.com");
    }

    @Test
    @DisplayName("findByProviderAndProviderId로 OAuth 사용자를 조회한다")
    void findByProviderAndProviderIdTest() {
        // given
        User user = createAndSaveUserWithProvider("testUser", "kakao", "kakao123");

        // when
        Optional<User> found = userRepository.findByProviderAndProviderId("kakao", "kakao123");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getProvider()).isEqualTo("kakao");
        assertThat(found.get().getProviderId()).isEqualTo("kakao123");
    }
}
