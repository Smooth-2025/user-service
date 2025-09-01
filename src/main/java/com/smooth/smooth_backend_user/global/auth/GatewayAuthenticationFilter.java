package com.smooth.smooth_backend_user.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String AUTHENTICATED_HEADER = "X-Authenticated";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(USER_ID_HEADER);
        String userEmail = request.getHeader(USER_EMAIL_HEADER);
        String authenticated = request.getHeader(AUTHENTICATED_HEADER);

        log.debug("Gateway 헤더 확인: userId={}, email={}, authenticated={}", userId, userEmail, authenticated);

        // Gateway에서 인증된 사용자인 경우 SecurityContext에 설정
        if (StringUtils.hasText(userId) && "true".equals(authenticated)) {
            try {
                Long userIdLong = Long.valueOf(userId);
                GatewayUserDetails userDetails = new GatewayUserDetails(userIdLong, userEmail);
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Gateway 인증 정보 SecurityContext 설정 완료: userId={}", userIdLong);
            } catch (NumberFormatException e) {
                log.warn("잘못된 사용자 ID 형식: {}", userId);
            }
        }

        filterChain.doFilter(request, response);
    }
}