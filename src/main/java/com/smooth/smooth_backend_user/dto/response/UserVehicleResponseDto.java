package com.smooth.smooth_backend_user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class UserVehicleResponseDto {
    private Long userId;
    private Long vehicleId;
    private String plateNumber;
    private String imei;
    private LocalDateTime linkedAt;
}
