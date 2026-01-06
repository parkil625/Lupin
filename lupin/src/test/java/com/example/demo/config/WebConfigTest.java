package com.example.demo.config;

import com.example.demo.security.CurrentUserArgumentResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebConfig 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebConfig 테스트")
class WebConfigTest {

    @Mock
    private CurrentUserArgumentResolver currentUserArgumentResolver;

    @InjectMocks
    private WebConfig webConfig;

    @Test
    @DisplayName("ArgumentResolver에 CurrentUserArgumentResolver가 등록됨")
    void addArgumentResolvers_ShouldAddCurrentUserArgumentResolver() {
        // Given
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        // When
        webConfig.addArgumentResolvers(resolvers);

        // Then
        assertThat(resolvers).hasSize(1);
        assertThat(resolvers.get(0)).isEqualTo(currentUserArgumentResolver);
    }
}
