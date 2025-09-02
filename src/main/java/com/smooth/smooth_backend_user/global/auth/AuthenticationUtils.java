package com.smooth.smooth_backend_user.global.auth;

import com.smooth.smooth_backend_user.global.exception.BusinessException;
import com.smooth.smooth_backend_user.user.exception.UserErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


// 인증 관련 유틸리티 클래스
// Gateway에서 전달된 사용자 정보를 쉽게 추출할 수 있도록 도와줌

@Slf4j
public class AuthenticationUtils {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String AUTHENTICATED_HEADER = "X-Authenticated";


//     현재 인증된 사용자의 ID를 반환
//     @return 사용자 ID (없으면 null)
    public static Long getCurrentUserId() {
        // 1. SecurityContext에서 먼저 시도
        Long userIdFromSecurity = getUserIdFromSecurityContext();
        if (userIdFromSecurity != null) {
            return userIdFromSecurity;
        }
        // 2. HTTP 헤더에서 시도 (Fallback)
        return getUserIdFromHeader();
    }

//     현재 인증된 사용자의 ID를 반환 (없으면 예외 발생)
    public static Long getCurrentUserIdOrThrow() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(UserErrorCode.INVALID_TOKEN, "인증되지 않은 사용자입니다.");
        }
        return userId;
    }


//     현재 인증된 사용자의 이메일을 반환
    public static String getCurrentUserEmail() {
        // SecurityContext 우선, 실패하면 헤더에서
        String emailFromSecurity = getUserEmailFromSecurityContext();
        if (StringUtils.hasText(emailFromSecurity)) {
            return emailFromSecurity;
        }
        return getUserEmailFromHeader();
    }


//     현재 요청이 인증된 요청인지 확인

    public static boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }

    // Private Helper Methods
    
    private static Long getUserIdFromSecurityContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof GatewayUserDetails) {
                GatewayUserDetails userDetails = (GatewayUserDetails) authentication.getPrincipal();
                return userDetails.getUserId();
            }
        } catch (Exception e) {
            log.debug("SecurityContext에서 사용자 ID 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private static Long getUserIdFromHeader() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();
            String userIdHeader = request.getHeader(USER_ID_HEADER);
            String authenticatedHeader = request.getHeader(AUTHENTICATED_HEADER);

            if (StringUtils.hasText(userIdHeader) && "true".equals(authenticatedHeader)) {
                return Long.valueOf(userIdHeader);
            }
        } catch (Exception e) {
            log.debug("HTTP 헤더에서 사용자 ID 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private static String getUserEmailFromSecurityContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof GatewayUserDetails) {
                GatewayUserDetails userDetails = (GatewayUserDetails) authentication.getPrincipal();
                return userDetails.getEmail();
            }
        } catch (Exception e) {
            log.debug("SecurityContext에서 사용자 이메일 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private static String getUserEmailFromHeader() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();
            String emailHeader = request.getHeader(USER_EMAIL_HEADER);
            String authenticatedHeader = request.getHeader(AUTHENTICATED_HEADER);

            if (StringUtils.hasText(emailHeader) && "true".equals(authenticatedHeader)) {
                return emailHeader;
            }
        } catch (Exception e) {
            log.debug("HTTP 헤더에서 사용자 이메일 추출 실패: {}", e.getMessage());
        }
        return null;
    }
}