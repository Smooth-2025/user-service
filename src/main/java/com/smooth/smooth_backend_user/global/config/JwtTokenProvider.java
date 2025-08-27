package com.smooth.smooth_backend_user.global.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityInMilliseconds;  // 15분
    private final long refreshTokenValidityInMilliseconds; // 30일

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

    // Refresh Token 생성
    public String createRefreshToken(Long userId, String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);
        String jti = UUID.randomUUID().toString(); // 토큰 고유 ID

        return Jwts.builder()
                .setId(jti)                           // JTI 추가
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("type", "refresh")             // 토큰 타입
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰에서 JTI 추출 (블랙리스트용)
    public String getJti(String token) {
        try {
            Claims claims = getClaims(token);
            String jti = claims.getId();
            // JTI가 없는 경우 토큰 자체를 JTI로 사용 (기존 토큰 호환성)
            return jti != null ? jti : token.substring(token.length() - 10);
        } catch (Exception e) {
            log.warn("Failed to extract JTI from token: {}", e.getMessage());
            return token.substring(token.length() - 10); // 토큰 마지막 10자리 사용
        }
    }

    // 토큰 타입 확인
    public String getTokenType(String token) {
        try {
            Claims claims = getClaims(token);
            String type = claims.get("type", String.class);
            // type이 없으면 access로 간주 (기존 토큰 호환성)
            return type != null ? type : "access";
        } catch (Exception e) {
            log.warn("Failed to extract token type: {}", e.getMessage());
            return "access"; // 기본값
        }
    }

    // 기존 메소드
    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    public String getEmail(String token) {
        try {
            Claims claims = getClaims(token);
            String email = claims.get("email", String.class);
            return email != null ? email : "";
        } catch (Exception e) {
            log.warn("Failed to extract email from token: {}", e.getMessage());
            return "";
        }
    }

    // Claims 추출 공통 메소드
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // JWT 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 토큰 만료 시간
    public long getExpirationTime(String token) {
        Claims claims = getClaims(token);
        return claims.getExpiration().getTime();
    }

    // 기존 메소드 (호환성 유지) - 나중에 제거 예정
    @Deprecated
    public String createToken(Long userId, String email) {
        return createAccessToken(userId, email);
    }
}