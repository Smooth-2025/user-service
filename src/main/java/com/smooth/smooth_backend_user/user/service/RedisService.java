package com.smooth.smooth_backend_user.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // JTI 기반 Access Token 블랙리스트
    public void addAccessTokenToBlacklist(String jti, long expirationTimeInSeconds) {
        try {
            redisTemplate.opsForValue().set(
                    "blacklist:access:" + jti,
                    "blocked",
                    Duration.ofSeconds(expirationTimeInSeconds)
            );
            log.debug("Access Token 블랙리스트 등록 완료: JTI={}", jti);
        } catch (Exception e) {
            log.error("Redis 연결 실패 - Access Token 블랙리스트 등록 실패: JTI={}, Error={}", jti, e.getMessage());
            // Redis 장애 시에도 로그아웃 프로세스는 계속 진행 (사용자 경험 우선)
        }
    }

    // JTI 기반 Refresh Token 블랙리스트
    public void addRefreshTokenToBlacklist(String jti, long expirationTimeInSeconds) {
        try {
            redisTemplate.opsForValue().set(
                    "blacklist:refresh:" + jti,
                    "blocked",
                    Duration.ofSeconds(expirationTimeInSeconds)
            );
            log.debug("Refresh Token 블랙리스트 등록 완료: JTI={}", jti);
        } catch (Exception e) {
            log.error("Redis 연결 실패 - Refresh Token 블랙리스트 등록 실패: JTI={}, Error={}", jti, e.getMessage());
            // Redis 장애 시에도 로그아웃 프로세스는 계속 진행 (사용자 경험 우선)
        }
    }

    // Access Token 블랙리스트 확인
    public boolean isAccessTokenBlacklisted(String jti) {
        try {
            return redisTemplate.hasKey("blacklist:access:" + jti);
        } catch (Exception e) {
            log.warn("Redis 연결 실패 - Access Token 블랙리스트 확인 스킵: JTI={}, Error={}", jti, e.getMessage());
            return false; // Redis 장애 시 토큰 허용 (가용성 우선)
        }
    }

    // Refresh Token 블랙리스트 확인
    public boolean isRefreshTokenBlacklisted(String jti) {
        try {
            return redisTemplate.hasKey("blacklist:refresh:" + jti);
        } catch (Exception e) {
            log.warn("Redis 연결 실패 - Refresh Token 블랙리스트 확인 스킵: JTI={}, Error={}", jti, e.getMessage());
            return false; // Redis 장애 시 토큰 허용 (가용성 우선)
        }
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