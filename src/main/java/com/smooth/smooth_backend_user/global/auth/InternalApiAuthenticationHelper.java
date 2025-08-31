package com.smooth.smooth_backend_user.global.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class InternalApiAuthenticationHelper {

    @Value("${internal.api.secret:smooth-internal-secret}")
    private String internalApiSecret;

    private static final String INTERNAL_API_HEADER = "X-Internal-Secret";

    public boolean isValidInternalRequest(HttpServletRequest request) {
        String headerValue = request.getHeader(INTERNAL_API_HEADER);
        
        if (!StringUtils.hasText(headerValue)) {
            log.warn("Internal API 호출에 인증 헤더가 없습니다. URI: {}", request.getRequestURI());
            return false;
        }

        boolean isValid = internalApiSecret.equals(headerValue);
        
        if (!isValid) {
            log.warn("Internal API 호출의 인증 헤더가 유효하지 않습니다. URI: {}, IP: {}", 
                    request.getRequestURI(), getClientIp(request));
        }
        
        return isValid;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}