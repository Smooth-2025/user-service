package com.smooth.smooth_backend_user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VehicleResponseDto extends CommonResponseDto {
    private String vehicleId;
    private Long userId;
    private Boolean connected;

    public static VehicleResponseDto connectSuccess(String vehicleId, Long userId) {
        VehicleResponseDto response = new VehicleResponseDto();
        response.setSuccess(true);
        response.setMessage("차량 연동이 완료되었습니다.");
        response.setVehicleId(vehicleId);
        response.setUserId(userId);
        response.setConnected(true);
        return response;
    }

    public static VehicleResponseDto statusResponse(String vehicleId, Long userId, Boolean connected) {
        VehicleResponseDto response = new VehicleResponseDto();
        response.setSuccess(true);
        response.setMessage("연동 상태 조회 완료");
        response.setVehicleId(vehicleId);
        response.setUserId(userId);
        response.setConnected(connected);
        return response;
    }

    public static VehicleResponseDto disconnectSuccess() {
        VehicleResponseDto response = new VehicleResponseDto();
        response.setSuccess(true);
        response.setMessage("차량 연동이 해제되었습니다.");
        response.setConnected(false);
        return response;
    }

    public static VehicleResponseDto error(String message) {
        VehicleResponseDto response = new VehicleResponseDto();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}