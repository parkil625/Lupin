package com.example.demo.config;

import com.example.demo.repository.UserRepository;
import com.example.demo.security.CurrentUserArgumentResolver;
import com.example.demo.security.JwtTokenProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@TestConfiguration
public class TestSecurityConfig implements WebMvcConfigurer {

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @Bean
    public CurrentUserArgumentResolver currentUserArgumentResolver() {
        return new CurrentUserArgumentResolver(userRepository);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver());
    }
}