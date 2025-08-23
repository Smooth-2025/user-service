package com.smooth.smooth_backend_user.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // JTI 기반 Access Token 블랙리스트
    public void addAccessTokenToBlacklist(String jti, long expirationTimeInSeconds) {
        redisTemplate.opsForValue().set(
                "blacklist:access:" + jti,
                "blocked",
                Duration.ofSeconds(expirationTimeInSeconds)
        );
    }

    // JTI 기반 Refresh Token 블랙리스트
    public void addRefreshTokenToBlacklist(String jti, long expirationTimeInSeconds) {
        redisTemplate.opsForValue().set(
                "blacklist:refresh:" + jti,
                "blocked",
                Duration.ofSeconds(expirationTimeInSeconds)
        );
    }

    // Access Token 블랙리스트 확인
    public boolean isAccessTokenBlacklisted(String jti) {
        return redisTemplate.hasKey("blacklist:access:" + jti);
    }

    // Refresh Token 블랙리스트 확인
    public boolean isRefreshTokenBlacklisted(String jti) {
        return redisTemplate.hasKey("blacklist:refresh:" + jti);
    }

    // 기존 메소드 (호환성 유지 - 나중에 제거 예정)
    @Deprecated
    public void addToBlacklist(String token, long expirationTimeInSeconds) {
        redisTemplate.opsForValue().set(
                "blacklist:" + token,
                "blocked",
                Duration.ofSeconds(expirationTimeInSeconds)
        );
    }

    @Deprecated
    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }

    // 기존 일반 캐시 메소드들...
    public void setValue(String key, Object value, long timeoutInSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeoutInSeconds));
    }

    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteValue(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public void setStringValue(String key, String value, long timeoutInSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeoutInSeconds));
    }

    public String getStringValue(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }
}