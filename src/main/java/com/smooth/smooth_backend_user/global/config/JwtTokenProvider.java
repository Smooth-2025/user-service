package com.smooth.smooth_backend_user.global.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.smooth.smooth_backend_user.user.entity.UserRole;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityInMilliseconds;  // 15분
    private final long refreshTokenValidityInMilliseconds; // 2주

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.access-expiration:900000}") long accessTokenValidity,  // 15분 = 900000ms
                            @Value("${jwt.refresh-expiration:1209600000}") long refreshTokenValidity) { // 2주
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityInMilliseconds = accessTokenValidity;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidity;
    }

    // Access Token 생성
    public String createAccessToken(Long userId, String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti)
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Access Token 생성 (역할별)
    public String createAccessToken(Long userId, String email, UserRole role) {
        Date now = new Date();
        long validity = (role == UserRole.ADMIN) ? 
            3600000L :                          // 관리자: 60분
            accessTokenValidityInMilliseconds;   // 일반사용자: 15분
        Date expirationDate = new Date(now.getTime() + validity);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti)
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(Long userId, String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti)
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh Token 생성 (역할별)
    public String createRefreshToken(Long userId, String email, UserRole role) {
        Date now = new Date();
        long validity = (role == UserRole.ADMIN) ? 
            43200000L :                          // 관리자: 12시간 = 43200000ms
            refreshTokenValidityInMilliseconds;  // 일반사용자: 2주
        Date expirationDate = new Date(now.getTime() + validity);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti)
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰 만료 시간 조회 (로그용)
    public long getAccessTokenExpirationTime() {
        return accessTokenValidityInMilliseconds;
    }

    public long getRefreshTokenExpirationTime() {
        return refreshTokenValidityInMilliseconds;
    }

    // Refresh Token 검증 및 사용자 정보 추출
    public Claims validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // refresh token 타입 검증
            if (!"refresh".equals(claims.get("type"))) {
                throw new JwtException("Invalid token type");
            }
            
            return claims;
        } catch (ExpiredJwtException e) {
            log.error("Refresh token expired: {}", e.getMessage());
            throw new JwtException("Refresh token expired");
        } catch (JwtException e) {
            log.error("Invalid refresh token: {}", e.getMessage());
            throw new JwtException("Invalid refresh token");
        }
    }

    // 토큰에서 사용자 ID 추출
    public Long getUserIdFromToken(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    // 토큰에서 이메일 추출
    public String getEmailFromToken(Claims claims) {
        return claims.get("email", String.class);
    }

    // 리프레시 토큰의 남은 시간 확인 (밀리초)
    public long getRemainingTimeFromToken(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    // 리프레시 토큰 재발급이 필요한지 확인 (10분 = 600000ms 미만일 때)
    public boolean needsRefreshTokenRenewal(Claims claims) {
        long remainingTime = getRemainingTimeFromToken(claims);
        return remainingTime <= 600000; // 10분
    }
}