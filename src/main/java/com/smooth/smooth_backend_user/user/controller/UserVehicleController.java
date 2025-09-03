package com.smooth.smooth_backend_user.user.controller;

import com.smooth.smooth_backend_user.user.dto.request.LinkVehicleRequestDto;
import com.smooth.smooth_backend_user.user.dto.response.LinkVehicleResponseDto;
import com.smooth.smooth_backend_user.global.common.ApiResponse;
import com.smooth.smooth_backend_user.user.service.UserVehicleService;
import com.smooth.smooth_backend_user.global.auth.AuthenticationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/api/users/vehicle", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserVehicleController {

    private final UserVehicleService userVehicleService;

    //-- 연동 차량 조회 --
    @GetMapping
    public ResponseEntity<ApiResponse<LinkVehicleResponseDto>> getRegisteredVehicle() {
        Long userId = AuthenticationUtils.getCurrentUserIdOrThrow();
        log.debug("Getting vehicle info for user: {}", userId);
        LinkVehicleResponseDto vehicleInfo = userVehicleService.getRegisteredVehicle(userId);
        log.info("Vehicle info retrieved: {}", vehicleInfo);
        return ResponseEntity.ok(
                ApiResponse.success("사용자 차량 정보 조회 성공", vehicleInfo)
        );
    }

    //-- 차량 연동 --
    @PostMapping
    public ResponseEntity<ApiResponse<LinkVehicleResponseDto>> linkVehicle(@Valid @RequestBody LinkVehicleRequestDto vehicleInfo) {
        Long userId = AuthenticationUtils.getCurrentUserIdOrThrow();
        LinkVehicleResponseDto linkVehicle = userVehicleService.linkVehicle(userId, vehicleInfo);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "차량등록이 성공적으로 생성되었습니다.", linkVehicle));
    }

    //-- 차량 연동 해제  --
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> unlinkVehicle() {
        Long userId = AuthenticationUtils.getCurrentUserIdOrThrow();
        userVehicleService.unlinkVehicle(userId);
        return ResponseEntity.ok(
                ApiResponse.success("차량연동이 성공적으로 해제 되었습니다.")
        );
    }


}
