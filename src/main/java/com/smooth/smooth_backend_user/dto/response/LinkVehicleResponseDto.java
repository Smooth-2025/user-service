package com.smooth.smooth_backend_user.dto.response;

import com.smooth.smooth_backend_user.entity.User;
import com.smooth.smooth_backend_user.entity.UserVehicle;
import com.smooth.smooth_backend_user.entity.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@AllArgsConstructor
@Builder
public class LinkVehicleResponseDto {
    private boolean linked;
    private VehicleInfoDto vehicle;

    @Getter
    @AllArgsConstructor
    public static class VehicleInfoDto {
        private Long userId;
        private Long vehicleId;
        private String userName;
        private String plateNumber;
        private String imei;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime linkedAt;
    }

    public static LinkVehicleResponseDto fromUserVehicle(final UserVehicle userVehicle) {
        final Vehicle vehicle = userVehicle.getVehicle();
        final User user = userVehicle.getUser();
        final LocalDateTime linkedAt = userVehicle.getLinkedAt();

        return LinkVehicleResponseDto.builder()
                .linked(true)
                .vehicle(
                        new VehicleInfoDto(
                                user.getId(),
                                vehicle.getId(),
                                user.getName(),
                                vehicle.getPlateNumber(),
                                vehicle.getImei(),
                                linkedAt))
                .build();
    }
}
