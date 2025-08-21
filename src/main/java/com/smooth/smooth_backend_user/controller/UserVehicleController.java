package com.smooth.smooth_backend_user.controller;

import com.smooth.smooth_backend_user.dto.request.LinkVehicleRequestDto;
import com.smooth.smooth_backend_user.dto.response.LinkVehicleResponseDto;
import com.smooth.smooth_backend_user.global.common.ApiResponse;
import com.smooth.smooth_backend_user.service.UserVehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/vehicle", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserVehicleController {

    private final UserVehicleService userVehicleService;

    //-- 연동 차량 조회 --
    @GetMapping
    public ResponseEntity<ApiResponse<LinkVehicleResponseDto>> getRegisteredVehicle() {
        Long userId = 1L; //TODO: 게아트웨이 header -유저 아이디 연결
        LinkVehicleResponseDto vehicleInfo = userVehicleService.getRegisteredVehicle(userId);
        return ResponseEntity.ok(
                ApiResponse.success("사용자 차량 정보 조회 성공", vehicleInfo)
        );
    }

    //-- 차량 연동 --
    @PostMapping
    public ResponseEntity<ApiResponse<LinkVehicleResponseDto>> linkVehicle(@Valid @RequestBody LinkVehicleRequestDto vehicleInfo) {
        Long userId = 1L;//TODO: 게아트웨이 header -유저 아이디 연결
        LinkVehicleResponseDto linkVehicle = userVehicleService.linkVehicle(userId, vehicleInfo);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "차량등록이 성공적으로 생성되었습니다.", linkVehicle));
    }

    //-- 차량 연동 해제  --
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> unlinkVehicle() {
        Long userId = 1L;//TODO: 게아트웨이 header -유저 아이디 연결
        userVehicleService.unlinkVehicle(userId);
        return ResponseEntity.ok(
                ApiResponse.success("차량연동이 성공적으로 해제 되었습니다.")
        );
    }


}
