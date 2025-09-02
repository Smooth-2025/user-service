package com.smooth.smooth_backend_user.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CookieUtils {

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_TOKEN_MAX_AGE = 14 * 24 * 60 * 60; // 14일
    private static final int ADMIN_REFRESH_TOKEN_MAX_AGE = 12 * 60 * 60; // 12시간


// 요청에서 리프레시 토큰 쿠키 추출
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

// 리프레시 토큰 쿠키 설정 (일반 사용자용 - 14일)
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        setRefreshTokenCookie(response, refreshToken, REFRESH_TOKEN_MAX_AGE);
    }

//     리프레시 토큰 쿠키 설정 (관리자용 - 12시간)
    public void setAdminRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        setRefreshTokenCookie(response, refreshToken, ADMIN_REFRESH_TOKEN_MAX_AGE);
    }

//     리프레시 토큰 쿠키 설정 (공통)
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, int maxAge) {
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(maxAge);
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);
        
        log.debug("리프레시 토큰 쿠키 설정 완료 - maxAge: {}초", maxAge);
    }

//     리프레시 토큰 쿠키 삭제
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // 즉시 만료
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);
        
        log.info("리프레시 토큰 쿠키 삭제 완료");
    }
}