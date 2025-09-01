package com.smooth.smooth_backend_user.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;


    // 일반 캐시 메소드들 (Redis 장애 대응 포함)
    public void setValue(String key, Object value, long timeoutInSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeoutInSeconds));
            log.debug("Redis 값 저장 완료: key={}", key);
        } catch (Exception e) {
            log.warn("Redis 연결 실패 - 값 저장 실패: key={}, Error={}", key, e.getMessage());
            // Redis 장애 시 저장은 실패하지만 애플리케이션은 계속 동작
        }
    }

    public Object getValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis 연결 실패 - 값 조회 실패: key={}, Error={}", key, e.getMessage());
            return null; // Redis 장애 시 null 반환
        }
    }

    public void deleteValue(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Redis 값 삭제 완료: key={}", key);
        } catch (Exception e) {
            log.warn("Redis 연결 실패 - 값 삭제 실패: key={}, Error={}", key, e.getMessage());
            // Redis 장애 시 삭제는 실패하지만 애플리케이션은 계속 동작
        }
    }

    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("Redis 연결 실패 - 키 존재 확인 실패: key={}, Error={}", key, e.getMessage());
            return false; // Redis 장애 시 키가 없다고 가정
        }
    }

    public void setStringValue(String key, String value, long timeoutInSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeoutInSeconds));
            log.debug("Redis 문자열 값 저장 완료: key={}", key);
        } catch (Exception e) {
            log.warn("Redis 연결 실패 - 문자열 값 저장 실패: key={}, Error={}", key, e.getMessage());
            // Redis 장애 시 저장은 실패하지만 애플리케이션은 계속 동작
        }
    }

    public String getStringValue(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("Redis 연결 실패 - 문자열 값 조회 실패: key={}, Error={}", key, e.getMessage());
            return null; // Redis 장애 시 null 반환
        }
    }
}