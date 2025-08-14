package com.smooth.smooth_backend_user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VehicleLinkResponseDto {
    private boolean linked;
    private UserVehicleResponseDto vehicle;
}
