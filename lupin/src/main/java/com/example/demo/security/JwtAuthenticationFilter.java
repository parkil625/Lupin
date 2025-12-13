package com.example.demo.security;

import com.example.demo.util.RedisKeyUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        // 1. Request Header 에서 토큰 추출
        String token = resolveToken((HttpServletRequest) request);

        // 2. 토큰 유효성 검사 + Claims 파싱 (1회 파싱으로 검증과 인증 동시 처리)
        if (token != null) {
            Optional<Claims> claimsOpt = jwtTokenProvider.validateAndParseClaims(token);

            if (claimsOpt.isPresent()) {
                // 3. Redis에 해당 Access Token이 로그아웃(Blacklist) 된 상태인지 확인
                String isLogout = redisTemplate.opsForValue().get(RedisKeyUtils.blacklist(token));

                if (ObjectUtils.isEmpty(isLogout)) {
                    // 4. 토큰이 유효하고 블랙리스트가 아니라면 인증 정보 설정 (Claims 재사용)
                    Authentication authentication = jwtTokenProvider.getAuthentication(claimsOpt.get());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Security Context에 '{}' 인증 정보를 저장했습니다", authentication.getName());
                } else {
                    log.warn("로그아웃된 토큰으로 접근을 시도했습니다.");
                }
            }
        }

        chain.doFilter(request, response);
    }

    // Request Header에서 토큰 정보 추출 ("Bearer " 떼고)
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}