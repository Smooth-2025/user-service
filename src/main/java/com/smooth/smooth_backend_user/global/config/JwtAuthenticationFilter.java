package com.smooth.smooth_backend_user.global.config;

import com.smooth.smooth_backend_user.user.service.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("JWT Filter - Processing request: {} {}", request.getMethod(), requestURI);

        String token = getTokenFromRequest(request);
        log.debug("JWT Filter - Token present: {}", token != null);

        if (StringUtils.hasText(token)) {
            log.debug("JWT Filter - Token validation: {}", jwtTokenProvider.validateToken(token));
            
            if (jwtTokenProvider.validateToken(token)) {
                try {
                    // JTI 기반 블랙리스트 확인
                    String jti = jwtTokenProvider.getJti(token);
                    String tokenType = jwtTokenProvider.getTokenType(token);
                    log.debug("JWT Filter - JTI: {}, TokenType: {}", jti, tokenType);

                    boolean isBlacklisted = false;
                    // 토큰 타입이 null인 경우 access 토큰으로 처리 (기존 호환성)
                    if ("access".equals(tokenType) || tokenType == null) {
                        isBlacklisted = redisService.isAccessTokenBlacklisted(jti);
                    } else if ("refresh".equals(tokenType)) {
                        isBlacklisted = redisService.isRefreshTokenBlacklisted(jti);
                    }
                    
                    log.debug("JWT Filter - Token blacklisted: {}", isBlacklisted);

                    if (!isBlacklisted) {
                        Long userId = jwtTokenProvider.getUserId(token);
                        String email = jwtTokenProvider.getEmail(token);
                        log.debug("JWT Filter - Extracted userId: {}, email: {}", userId, email);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userId.toString(), null, new ArrayList<>());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("JWT Filter - Authentication set successfully for user: {}", userId);
                    } else {
                        log.warn("JWT Filter - Token is blacklisted: {}", jti);
                    }
                } catch (Exception e) {
                    log.error("JWT Filter - Error processing token: {}", e.getMessage(), e);
                }
            } else {
                log.warn("JWT Filter - Invalid token provided");
            }
        } else {
            log.debug("JWT Filter - No token provided for {}", requestURI);
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }
        return null;
    }
}