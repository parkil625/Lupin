package com.example.demo.repository;

import com.example.demo.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * RefreshToken Redis 구현체
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(String userId, String refreshToken, long validityMs) {
        redisTemplate.opsForValue().set(
                RedisKeyUtils.refreshToken(userId),
                refreshToken,
                validityMs,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public Optional<String> findByUserId(String userId) {
        String token = redisTemplate.opsForValue().get(RedisKeyUtils.refreshToken(userId));
        return Optional.ofNullable(token);
    }

    @Override
    public void deleteByUserId(String userId) {
        redisTemplate.delete(RedisKeyUtils.refreshToken(userId));
    }

    @Override
    public void addToBlacklist(String accessToken, long expirationMs) {
        if (expirationMs > 0) {
            redisTemplate.opsForValue().set(
                    RedisKeyUtils.blacklist(accessToken),
                    "logout",
                    expirationMs,
                    TimeUnit.MILLISECONDS
            );
        }
    }
}
