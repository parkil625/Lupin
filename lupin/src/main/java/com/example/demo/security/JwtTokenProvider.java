package com.example.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "role";

    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;
    private final SecretKey key;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityMs) {
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(String userId, String role) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + accessTokenValidityMs);

        return Jwts.builder()
                .subject(userId)
                .claim(AUTHORITIES_KEY, role) // 권한 정보 저장
                .issuedAt(new Date())
                .expiration(validity)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(String userId) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + refreshTokenValidityMs);

        return Jwts.builder()
                .subject(userId)
                .expiration(validity)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Refresh Token 만료 시간 (Redis 저장 시 사용)
     */
    public long getRefreshTokenValidityMs() {
        return refreshTokenValidityMs;
    }

    /**
     * 토큰에서 인증 정보 조회 (DB 조회 X -> 고성능)
     */
    public Authentication getAuthentication(String accessToken) {
        return getAuthentication(parseClaims(accessToken));
    }

    /**
     * Claims에서 인증 정보 조회 (중복 파싱 방지용)
     * - validateAndParseClaims()와 함께 사용하면 파싱 1회로 검증 + 인증 처리 가능
     */
    public Authentication getAuthentication(Claims claims) {
        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role)) // ROLE_USER 등
                        .collect(Collectors.toList());

        // UserDetails 객체를 만들어서 Authentication 리턴
        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    /**
     * 토큰 검증 + Claims 반환 (중복 파싱 방지)
     * - 유효한 토큰: Claims 반환
     * - 무효한 토큰: Optional.empty() 반환
     */
    public Optional<Claims> validateAndParseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 잘못되었습니다.");
        }
        return Optional.empty();
    }

    /**
     * 토큰 유효성 검증 (기존 호환용)
     */
    public boolean validateToken(String token) {
        return validateAndParseClaims(token).isPresent();
    }

    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public long getExpiration(String accessToken) {
        // expiration date - current time
        Date expiration = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(accessToken)
                .getPayload()
                .getExpiration();

        long now = new Date().getTime();
        return (expiration.getTime() - now);
    }
}