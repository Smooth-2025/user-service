package com.smooth.smooth_backend_user.controller;

import com.smooth.smooth_backend_user.dto.request.ChangePasswordRequestDto;
import com.smooth.smooth_backend_user.dto.request.UpdateEmergencyInfoRequestDto;
import com.smooth.smooth_backend_user.dto.response.CommonResponseDto;
import com.smooth.smooth_backend_user.dto.response.UserProfileResponseDto;
import com.smooth.smooth_backend_user.entity.User;
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
    public ResponseEntity<UserProfileResponseDto> getUserProfile() {
        try {
            Long userId = getCurrentUserId();
            User user = userService.findById(userId);
            UserProfileResponseDto response = UserProfileResponseDto.fromUser(user);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 비밀번호 변경
    @PutMapping("/password")
    public ResponseEntity<CommonResponseDto> changePassword(
            @Validated @RequestBody ChangePasswordRequestDto dto) {
        try {
            Long userId = getCurrentUserId();
            userService.changePassword(userId, dto);

            return ResponseEntity.ok(CommonResponseDto.success("비밀번호가 성공적으로 변경되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(CommonResponseDto.error(e.getMessage()));
        }
    }

    // 응급정보 수정
    @PutMapping("/emergency-info")
    public ResponseEntity<UserProfileResponseDto> updateEmergencyInfo(
            @Validated @RequestBody UpdateEmergencyInfoRequestDto dto) {
        try {
            Long userId = getCurrentUserId();
            User updatedUser = userService.updateEmergencyInfo(userId, dto);
            UserProfileResponseDto response = UserProfileResponseDto.fromUser(updatedUser);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 내 정보 간단 조회 (이름, 이메일만)
    @GetMapping("/me")
    public ResponseEntity<Object> getMyInfo() {
        try {
            Long userId = getCurrentUserId();
            User user = userService.findById(userId);

            // 간단한 정보만 반환
            return ResponseEntity.ok(new Object() {
                public final Long id = user.getId();
                public final String name = user.getName();
                public final String email = user.getEmail();
                public final String phone = user.getPhone();
            });
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}