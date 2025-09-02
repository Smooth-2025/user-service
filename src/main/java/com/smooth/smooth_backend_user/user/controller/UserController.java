package com.smooth.smooth_backend_user.user.controller;

import com.smooth.smooth_backend_user.user.dto.request.ChangePasswordRequestDto;
import com.smooth.smooth_backend_user.user.dto.request.UpdateEmergencyInfoRequestDto;
import com.smooth.smooth_backend_user.user.dto.response.UserProfileResponseDto;
import com.smooth.smooth_backend_user.user.entity.User;
import com.smooth.smooth_backend_user.global.common.ApiResponse;
import com.smooth.smooth_backend_user.global.auth.AuthenticationUtils;
import com.smooth.smooth_backend_user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;


    // 리프레시 토큰 쿠키 삭제하는 헬퍼 메서드
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // 즉시 만료
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);
        log.info("리프레시 토큰 쿠키 삭제 완료");
    }

    // 회원 정보 조회
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> getUserProfile() {
        Long userId = AuthenticationUtils.getCurrentUserIdOrThrow();
        User user = userService.findById(userId);
        UserProfileResponseDto response = UserProfileResponseDto.fromUser(user);

        return ResponseEntity.ok(
                ApiResponse.success("회원 정보 조회 성공", response)
        );
    }

    // 비밀번호 변경
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Validated @RequestBody ChangePasswordRequestDto dto) {

        Long userId = AuthenticationUtils.getCurrentUserIdOrThrow();
        userService.changePassword(userId, dto);

        return ResponseEntity.ok(
                ApiResponse.success("비밀번호가 성공적으로 변경되었습니다.")
        );
    }

    // 응급정보 수정
    @PutMapping("/emergency-info")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> updateEmergencyInfo(
            @Validated @RequestBody UpdateEmergencyInfoRequestDto dto) {

        Long userId = AuthenticationUtils.getCurrentUserIdOrThrow();
        User updatedUser = userService.updateEmergencyInfo(userId, dto);
        UserProfileResponseDto response = UserProfileResponseDto.fromUser(updatedUser);

        return ResponseEntity.ok(
                ApiResponse.success("응급정보가 성공적으로 수정되었습니다.", response)
        );
    }

    // 내 정보 간단 조회 (이름, 이메일만)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSimpleInfoDto>> getMyInfo() {
        Long userId = AuthenticationUtils.getCurrentUserIdOrThrow();
        User user = userService.findById(userId);

        UserSimpleInfoDto info = UserSimpleInfoDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();

        return ResponseEntity.ok(
                ApiResponse.success("사용자 정보 조회 성공", info)
        );
    }

    // 회원탈퇴
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(HttpServletResponse response) {
        Long userId = AuthenticationUtils.getCurrentUserIdOrThrow();
        
        // 회원 정보 삭제
        userService.deleteAccount(userId);
        
        // 리프레시 토큰 쿠키 삭제
        clearRefreshTokenCookie(response);
        
        log.info("회원탈퇴 완료 - 사용자 ID: {}", userId);
        return ResponseEntity.ok(
                ApiResponse.success("회원탈퇴가 완료되었습니다.")
        );
    }

    // 간단한 사용자 정보 DTO
    @lombok.Builder
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class UserSimpleInfoDto {
        private Long id;
        private String name;
        private String email;
        private String phone;
    }
}