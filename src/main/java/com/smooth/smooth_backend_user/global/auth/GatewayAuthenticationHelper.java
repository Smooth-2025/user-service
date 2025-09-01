package com.smooth.smooth_backend_user.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class GatewayAuthenticationHelper {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    public Long getUserIdFromHeader(HttpServletRequest request) {
        String userIdStr = request.getHeader(USER_ID_HEADER);
        if (StringUtils.hasText(userIdStr)) {
            try {
                return Long.valueOf(userIdStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID in header: {}", userIdStr);
            }
        }
        return null;
    }

    public String getUserEmailFromHeader(HttpServletRequest request) {
        String email = request.getHeader(USER_EMAIL_HEADER);
        return StringUtils.hasText(email) ? email : null;
    }

    public boolean isAuthenticated(HttpServletRequest request) {
        return getUserIdFromHeader(request) != null;
    }
}