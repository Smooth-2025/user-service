package com.smooth.smooth_backend_user.controller;

import com.smooth.smooth_backend_user.dto.request.ChangePasswordRequestDto;
import com.smooth.smooth_backend_user.dto.request.UpdateEmergencyInfoRequestDto;
import com.smooth.smooth_backend_user.dto.response.UserProfileResponseDto;
import com.smooth.smooth_backend_user.entity.User;
import com.smooth.smooth_backend_user.global.common.ApiResponse;
import com.smooth.smooth_backend_user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 현재 로그인한 사용자 ID 가져오는 헬퍼 메서드
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userIdStr = (String) authentication.getPrincipal();
        return Long.valueOf(userIdStr);
    }

    // 회원 정보 조회
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> getUserProfile() {
        Long userId = getCurrentUserId();
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

        Long userId = getCurrentUserId();
        userService.changePassword(userId, dto);

        return ResponseEntity.ok(
                ApiResponse.success("비밀번호가 성공적으로 변경되었습니다.")
        );
    }

    // 응급정보 수정
    @PutMapping("/emergency-info")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> updateEmergencyInfo(
            @Validated @RequestBody UpdateEmergencyInfoRequestDto dto) {

        Long userId = getCurrentUserId();
        User updatedUser = userService.updateEmergencyInfo(userId, dto);
        UserProfileResponseDto response = UserProfileResponseDto.fromUser(updatedUser);

        return ResponseEntity.ok(
                ApiResponse.success("응급정보가 성공적으로 수정되었습니다.", response)
        );
    }

    // 내 정보 간단 조회 (이름, 이메일만)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSimpleInfoDto>> getMyInfo() {
        Long userId = getCurrentUserId();
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