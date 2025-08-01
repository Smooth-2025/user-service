package com.smooth.smooth_backend_user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // JWT 블랙리스트 관리
    public void addToBlacklist(String token, long expirationTimeInSeconds) {
        redisTemplate.opsForValue().set(
                "blacklist:" + token,
                "blocked",
                Duration.ofSeconds(expirationTimeInSeconds)
        );
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }

    // 차량 연동 정보 관리
    public void setUserVehicle(Long userId, String vehicleId) {
        redisTemplate.opsForValue().set("user:vehicle:" + userId, vehicleId);
    }

    public String getUserVehicle(Long userId) {
        return (String) redisTemplate.opsForValue().get("user:vehicle:" + userId);
    }

    public void removeUserVehicle(Long userId) {
        redisTemplate.delete("user:vehicle:" + userId);
    }

    // QR 세션 관리
    public void setQRSession(String sessionToken, Object sessionData, long expirationInSeconds) {
        redisTemplate.opsForValue().set(
                "qr:session:" + sessionToken,
                sessionData,
                Duration.ofSeconds(expirationInSeconds)
        );
    }

    public Object getQRSession(String sessionToken) {
        return redisTemplate.opsForValue().get("qr:session:" + sessionToken);
    }

    public void removeQRSession(String sessionToken) {
        redisTemplate.delete("qr:session:" + sessionToken);
    }

    // 일반적인 캐시 작업
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
}