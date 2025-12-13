package com.example.demo.config;

import com.example.demo.config.properties.CorsProperties;
import com.example.demo.security.JwtAccessDeniedHandler;
import com.example.demo.security.JwtAuthenticationEntryPoint;
import com.example.demo.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정 (Modern Spring Security 6.x 방식)
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    // URL 상수 정의
    private static final String[] PUBLIC_URLS = {
            "/",
            "/api/health"
    };

    private static final String[] AUTH_URLS = {
            "/api/auth/**"
    };

    private static final String[] OAUTH_URLS = {
            "/api/oauth/*/login"
    };

    private static final String[] WEBSOCKET_URLS = {
            "/ws/**"
    };

    private static final String[] SWAGGER_URLS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    };

    private static final String[] PUBLIC_API_URLS = {
            "/api/users/ranking",
            "/api/users/statistics",
            "/api/users/*/ranking-context",
            "/api/notifications/subscribe",
            "/api/auction/stream/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CorsProperties corsProperties;

    /**
     * BCrypt 패스워드 인코더 빈 등록
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 설정 (CorsProperties를 통한 타입 안전한 설정 관리)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> effectiveOrigins = corsProperties.getEffectiveOrigins();
        configuration.setAllowedOrigins(effectiveOrigins);
        log.info("CORS 허용 origin 설정: {}", effectiveOrigins);

        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Security Filter Chain 설정 (Modern 방식)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용 시)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 사용 안 함 (JWT 기반 인증)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 401 에러 처리
                        .accessDeniedHandler(jwtAccessDeniedHandler)           // 403 에러 처리
                )

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight 요청 허용
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // 헬스체크 엔드포인트
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        // 로그인 엔드포인트는 인증 없이 접근 가능
                        .requestMatchers(AUTH_URLS).permitAll()
                        // OAuth 로그인 엔드포인트
                        .requestMatchers(OAUTH_URLS).permitAll()
                        // WebSocket 엔드포인트 (SockJS 관련 경로 포함)
                        .requestMatchers(WEBSOCKET_URLS).permitAll()
                        // Swagger UI
                        .requestMatchers(SWAGGER_URLS).permitAll()
                        // 공개 API (랭킹, 통계, SSE 등)
                        .requestMatchers(PUBLIC_API_URLS).permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
